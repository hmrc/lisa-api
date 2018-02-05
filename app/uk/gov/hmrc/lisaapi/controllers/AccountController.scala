/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.lisaapi.controllers

import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsObject, JsPath, JsValue, Json}
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, _}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.http.HeaderCarrier

class AccountController extends LisaController with LisaConstants {

  val service: AccountService = AccountService
  val auditService: AuditService = AuditService

  def createOrTransferLisaAccount(lisaManager: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    implicit val startTime = System.currentTimeMillis()
    LisaMetrics.startMetrics(startTime,LisaMetricKeys.ACCOUNT)

    withValidLMRN(lisaManager) {
      withValidJson[CreateLisaAccountRequest](
        (req) => {
          req match {
            case createRequest: CreateLisaAccountCreationRequest => {
              if (hasAccountTransferData(request.body.asJson.get.as[JsObject])) {
                LisaMetrics.startMetrics(System.currentTimeMillis(),
                  LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

                Future.successful(Forbidden(toJson(ErrorTransferAccountDataProvided)))
              }
              else {
                processAccountCreation(lisaManager, createRequest)
              }
            }
            case transferRequest: CreateLisaAccountTransferRequest => processAccountTransfer(lisaManager, transferRequest)
          }
        },
        Some(
          (errors) => {
            Logger.info("The errors are " + errors.toString())

            val transferAccountDataNotProvided = errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString().contains("/transferAccount") && errors.contains(ValidationError("error.path.missing"))
              }
            }

            if (transferAccountDataNotProvided > 0) {
              LisaMetrics.incrementMetrics(startTime,
                LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

              Future.successful(Forbidden(toJson(ErrorTransferAccountDataNotProvided)))
            }
            else {
              LisaMetrics.incrementMetrics(startTime,
                LisaMetricKeys.lisaError(BAD_REQUEST,LisaMetricKeys.ACCOUNT))

              Future.successful(BadRequest(toJson(ErrorBadRequest(errorConverter.convert(errors)))))
            }
          }
        ), lisaManager = lisaManager
      )
    }
  }

  def getAccountDetails (lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    implicit val startTime = System.currentTimeMillis()
    LisaMetrics.startMetrics(startTime, LisaMetricKeys.ACCOUNT)
    withValidLMRN(lisaManager) {
      withValidAccountId(accountId) {
        processGetAccountDetails(lisaManager, accountId)
      }
    }
  }

  def closeLisaAccount(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    withValidLMRN(lisaManager) {
      withValidJson[CloseLisaAccountRequest]( closeRequest =>
        {
            implicit val startTime = System.currentTimeMillis()
            LisaMetrics.startMetrics(startTime,LisaMetricKeys.CLOSE)
            processAccountClosure(lisaManager, accountId, closeRequest)
        }, lisaManager = lisaManager
      )
    }
  }

  private def processGetAccountDetails(lisaManager:String, accountId:String)(implicit hc: HeaderCarrier,startTime:Long) = {
    service.getAccount(lisaManager, accountId).map { result =>
      result match {
        case response : GetLisaAccountSuccessResponse  => {
          LisaMetrics.incrementMetrics(startTime,LisaMetricKeys.ACCOUNT)
          Ok(Json.toJson(response))
        }

        case GetLisaAccountDoesNotExistResponse => {
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))
          NotFound(Json.toJson(ErrorAccountNotFound))
        }

        case _ => {
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))
          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    }
  }

  private def returnCreationFailure(lisaManager: String,
                                    creationRequest: CreateLisaAccountCreationRequest,
                                    e: ErrorResponse, status: Int)
                                   (implicit hc: HeaderCarrier, startTime:Long) = {
    auditService.audit(
      auditType = "accountNotCreated",
      path = getEndpointUrl(lisaManager),
      auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager, "reasonNotCreated" -> e.errorCode)
    )

    LisaMetrics.incrementMetrics(System.currentTimeMillis(), LisaMetricKeys.lisaError(status, LisaMetricKeys.ACCOUNT))

    Status(status).apply(Json.toJson(e))
  }

  private def hasValidSubscriptionDate(lisaManager: String, creationRequest: CreateLisaAccountCreationRequest)
                                      (success: () => Future[Result])
                                      (implicit hc: HeaderCarrier, startTime:Long): Future[Result] = {

    if (creationRequest.firstSubscriptionDate.isBefore(LISA_START_DATE)) {
      auditService.audit(
        auditType = "accountNotCreated",
        path = getEndpointUrl(lisaManager),
        auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
          "reasonNotCreated" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(System.currentTimeMillis(), LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation("INVALID_DATE", "The firstSubscriptionDate cannot be before 6 April 2017", Some("/firstSubscriptionDate"))
      )))))
    }
    else {
      success()
    }
  }

  private def processAccountCreation(lisaManager: String, creationRequest: CreateLisaAccountCreationRequest)
                                    (implicit hc: HeaderCarrier, startTime:Long) = {

    hasValidSubscriptionDate(lisaManager, creationRequest) { () =>
      service.createAccount(lisaManager, creationRequest).map { result =>
        result match {
          case CreateLisaAccountSuccessResponse(accountId) => {
            auditService.audit(
              auditType = "accountCreated",
              path = getEndpointUrl(lisaManager),
              auditData = creationRequest.toStringMap + (ZREF -> lisaManager)
            )
            val data = ApiResponseData(message = "Account created", accountId = Some(accountId))
            LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.ACCOUNT)

            Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
          }
          case CreateLisaAccountInvestorNotFoundResponse =>
            returnCreationFailure(lisaManager, creationRequest, ErrorInvestorNotFound, FORBIDDEN)
          case CreateLisaAccountInvestorNotEligibleResponse =>
            returnCreationFailure(lisaManager, creationRequest, ErrorInvestorNotEligible, FORBIDDEN)
          case CreateLisaAccountInvestorComplianceCheckFailedResponse =>
            returnCreationFailure(lisaManager, creationRequest, ErrorInvestorComplianceCheckFailedCreateTransfer, FORBIDDEN)
          case CreateLisaAccountInvestorAccountAlreadyClosedResponse =>
            returnCreationFailure(lisaManager, creationRequest, ErrorAccountAlreadyClosed, FORBIDDEN)
          case CreateLisaAccountInvestorAccountAlreadyVoidResponse =>
            returnCreationFailure(lisaManager, creationRequest, ErrorAccountAlreadyVoided, FORBIDDEN)
          case CreateLisaAccountAlreadyExistsResponse =>
            returnCreationFailure(lisaManager, creationRequest, ErrorAccountAlreadyExists(creationRequest.accountId), CONFLICT)
          case _ => {
            Logger.error(s"AccountController: createAccount unknown case from DES returning internal server error")
            returnCreationFailure(lisaManager, creationRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR)
          }
        }
      } recover {
        case e: Exception => {
          Logger.error(s"AccountController: An error occurred due to ${e.getMessage} returning internal server error")
          returnCreationFailure(lisaManager, creationRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR)
        }
      }
    }
  }

  private def processAccountTransfer(lisaManager: String, transferRequest: CreateLisaAccountTransferRequest)(implicit hc: HeaderCarrier,startTime:Long) = {
    if (
      transferRequest.firstSubscriptionDate.isBefore(LISA_START_DATE) ||
      transferRequest.transferAccount.transferInDate.isBefore(LISA_START_DATE)) {

      def firstSubscriptionDateError(request: CreateLisaAccountTransferRequest) = {
        if (transferRequest.firstSubscriptionDate.isBefore(LISA_START_DATE)) {
          Some(ErrorValidation("INVALID_DATE", "The firstSubscriptionDate cannot be before 6 April 2017", Some("/firstSubscriptionDate")))
        }
        else {
          None
        }
      }

      def transferInDateError(request: CreateLisaAccountTransferRequest) = {
        if (transferRequest.transferAccount.transferInDate.isBefore(LISA_START_DATE)) {
          Some(ErrorValidation("INVALID_DATE", "The transferInDate cannot be before 6 April 2017", Some("/transferAccount/transferInDate")))
        }
        else {
          None
        }
      }

      val errors = List(firstSubscriptionDateError(transferRequest), transferInDateError(transferRequest)).
                    filter(_.isDefined).
                    map(_.get)

      auditService.audit(
        auditType = "accountNotTransferred",
        path = getEndpointUrl(lisaManager),
        auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
          "reasonNotCreated" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(System.currentTimeMillis(), LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors))))
    }
    else {
      service.transferAccount(lisaManager, transferRequest).map { result =>
        result match {
          case CreateLisaAccountSuccessResponse(accountId) => {
            auditService.audit(
              auditType = "accountTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap + (ZREF -> lisaManager)
            )
            val data = ApiResponseData(message = "Account transferred", accountId = Some(accountId))
            LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.ACCOUNT)

            Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
          }
          case CreateLisaAccountInvestorNotFoundResponse => {
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> ErrorInvestorNotFound.errorCode)
            )
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

            Forbidden(Json.toJson(ErrorInvestorNotFound))
          }
          case CreateLisaAccountInvestorComplianceCheckFailedResponse => {
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> ErrorInvestorComplianceCheckFailedCreateTransfer.errorCode)
            )
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

            Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailedCreateTransfer))
          }
          case CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse => {
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> ErrorPreviousAccountDoesNotExist.errorCode)
            )
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

            Forbidden(Json.toJson(ErrorPreviousAccountDoesNotExist))
          }
          case CreateLisaAccountInvestorAccountAlreadyClosedResponse => {
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> ErrorAccountAlreadyClosed.errorCode)
            )
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

            Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
          }
          case CreateLisaAccountInvestorAccountAlreadyVoidResponse => {
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> ErrorAccountAlreadyVoided.errorCode)
            )
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.ACCOUNT))

            Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
          }
          case CreateLisaAccountAlreadyExistsResponse => {
            val result = ErrorAccountAlreadyExists(transferRequest.accountId)
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> result.errorCode)
            )
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(CONFLICT, LisaMetricKeys.ACCOUNT))

            Conflict(Json.toJson(result))
          }
          case _ => {
            auditService.audit(
              auditType = "accountNotTransferred",
              path = getEndpointUrl(lisaManager),
              auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "reasonNotCreated" -> ErrorInternalServerError.errorCode)
            )
            Logger.error(s"AccountController: transferAccount unknown case from DES returning internal server error")
            LisaMetrics.incrementMetrics(System.currentTimeMillis(),
              LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR, LisaMetricKeys.ACCOUNT))

            InternalServerError(Json.toJson(ErrorInternalServerError))
          }
        }
      } recover {
        case e: Exception => {
          Logger.error(s"AccountController: An error occurred in due to ${e.getMessage} returning internal server error")

          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR, LisaMetricKeys.ACCOUNT))

          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    }
  }

  private def processAccountClosure(lisaManager: String, accountId: String, closeLisaAccountRequest: CloseLisaAccountRequest)(implicit hc: HeaderCarrier, startTime:Long) = {
    if (closeLisaAccountRequest.closureDate.isBefore(LISA_START_DATE)) {
      auditService.audit(
        auditType = "accountNotClosed",
        path = getCloseEndpointUrl(lisaManager, accountId),
        auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
          "accountId" -> accountId,
          "reasonNotClosed" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(System.currentTimeMillis(), LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.CLOSE))

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation("INVALID_DATE", "The closureDate cannot be before 6 April 2017", Some("/closureDate"))
      )))))
    }
    else {
      service.closeAccount(lisaManager, accountId, closeLisaAccountRequest).map { result =>
        result match {
          case CloseLisaAccountSuccessResponse(accountId) => {
            LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.CLOSE)

            auditService.audit(
              auditType = "accountClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId)
            )

            val data = ApiResponseData(message = "LISA account closed", accountId = Some(accountId))

            Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = 200)))
          }
          case CloseLisaAccountAlreadyVoidResponse => {
            auditService.audit(
              auditType = "accountNotClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId,
                "reasonNotClosed" -> ErrorAccountAlreadyVoided.errorCode)
            )
            LisaMetrics.incrementMetrics(startTime,
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.CLOSE))

            Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
          }
          case CloseLisaAccountAlreadyClosedResponse => {
            auditService.audit(
              auditType = "accountNotClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId,
                "reasonNotClosed" -> ErrorAccountAlreadyClosed.errorCode)
            )
            LisaMetrics.incrementMetrics(startTime,
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.CLOSE))

            Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
          }
          case CloseLisaAccountCancellationPeriodExceeded => {
            auditService.audit(
              auditType = "accountNotClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId,
                "reasonNotClosed" -> ErrorAccountCancellationPeriodExceeded.errorCode)
            )
            LisaMetrics.incrementMetrics(startTime,
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.CLOSE))

            Forbidden(Json.toJson(ErrorAccountCancellationPeriodExceeded))
          }
          case CloseLisaAccountWithinCancellationPeriod => {
            auditService.audit(
              auditType = "accountNotClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId,
                "reasonNotClosed" -> ErrorAccountWithinCancellationPeriod.errorCode)
            )
            LisaMetrics.incrementMetrics(startTime,
              LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.CLOSE))

            Forbidden(Json.toJson(ErrorAccountWithinCancellationPeriod))
          }
          case CloseLisaAccountNotFoundResponse => {
            auditService.audit(
              auditType = "accountNotClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId,
                "reasonNotClosed" -> ErrorAccountNotFound.errorCode)
            )
            LisaMetrics.incrementMetrics(startTime,
              LisaMetricKeys.lisaError(NOT_FOUND, LisaMetricKeys.CLOSE))

            NotFound(Json.toJson(ErrorAccountNotFound))
          }
          case _ => {
            auditService.audit(
              auditType = "accountNotClosed",
              path = getCloseEndpointUrl(lisaManager, accountId),
              auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
                "accountId" -> accountId,
                "reasonNotClosed" -> ErrorInternalServerError.errorCode)
            )
            Logger.error(s"AccountController: closeAccount unknown case from DES returning internal server error")
            LisaMetrics.incrementMetrics(startTime,
              LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR, LisaMetricKeys.CLOSE))

            InternalServerError(Json.toJson(ErrorInternalServerError))
          }
        }
      } recover {
        case e: Exception => Logger.error(s"AccountController: closeAccount: An error occurred due to ${e.getMessage} returning internal server error")
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR, LisaMetricKeys.CLOSE))
          InternalServerError(Json.toJson(ErrorInternalServerError))
      }
    }
  }

  private def hasAccountTransferData(js: JsObject): Boolean = {
    js.keys.contains("transferAccount")
  }

  private def getEndpointUrl(lisaManagerReferenceNumber: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts"
  }

  private def getAccountDetailsEndpointUrl(lisaManagerReferenceNumber: String, accountId: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountId"
  }

  private def getCloseEndpointUrl(lisaManagerReferenceNumber: String, accountID: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountID/close-account"
  }
}
