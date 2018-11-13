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
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsObject, JsPath, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, _}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountController extends LisaController with LisaConstants {

  val service: AccountService = AccountService
  val auditService: AuditService = AuditService

  //region Create Or Transfer Account

  def createOrTransferLisaAccount(lisaManager: String): Action[AnyContent] =
    (validateHeader() andThen validateLMRN(lisaManager)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidJson[CreateLisaAccountRequest]({
        case createRequest: CreateLisaAccountCreationRequest =>
          if (hasAccountTransferData(request.body.asJson.get.as[JsObject])) {
            LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

            Future.successful(Forbidden(toJson(ErrorTransferAccountDataProvided)))
          }
          else {
            processAccountCreation(lisaManager, createRequest)
          }
        case transferRequest: CreateLisaAccountTransferRequest => processAccountTransfer(lisaManager, transferRequest)
      },
        Some(
          (errors) => {
            Logger.info("The errors are " + errors.toString())

            val transferAccountDataNotProvided = errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) =>
                path.toString().contains("/transferAccount") && errors.contains(ValidationError("error.path.missing"))
            }

            if (transferAccountDataNotProvided > 0) {
              LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

              Future.successful(Forbidden(toJson(ErrorTransferAccountDataNotProvided)))
            }
            else {
              LisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.ACCOUNT)

              Future.successful(BadRequest(toJson(ErrorBadRequest(errorConverter.convert(errors)))))
            }
          }
        ), lisaManager = lisaManager
      )
    }

  def getAccountDetails(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader() andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withEnrolment(lisaManager) { (_) =>
        processGetAccountDetails(lisaManager, accountId)
      }
    }

  private def processGetAccountDetails(lisaManager: String, accountId: String)
                                      (implicit hc: HeaderCarrier, startTime: Long) = {
    service.getAccount(lisaManager, accountId).map {
      case response: GetLisaAccountSuccessResponse =>
        LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.ACCOUNT)
        Ok(Json.toJson(response))

      case GetLisaAccountDoesNotExistResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.ACCOUNT)
        NotFound(Json.toJson(ErrorAccountNotFound))

      case _ =>
        LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.ACCOUNT)
        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  def closeLisaAccount(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader() andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidJson[CloseLisaAccountRequest](
        closeRequest => processAccountClosure(lisaManager, accountId, closeRequest),
        lisaManager = lisaManager
      )
    }

  private def processAccountClosure(lisaManager: String, accountId: String, closeLisaAccountRequest: CloseLisaAccountRequest)
                                   (implicit hc: HeaderCarrier, startTime: Long) = {
    hasValidDatesForClosure(lisaManager, accountId, closeLisaAccountRequest) { () =>
      service.closeAccount(lisaManager, accountId, closeLisaAccountRequest).map {
        case CloseLisaAccountSuccessResponse(`accountId`) =>
          auditService.audit(
            auditType = "accountClosed",
            path = getCloseEndpointUrl(lisaManager, accountId),
            auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "accountId" -> accountId)
          )

          LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.CLOSE)

          val data = ApiResponseData(message = "LISA account closed", accountId = Some(accountId))

          Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = OK)))
        case CloseLisaAccountAlreadyVoidResponse =>
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorAccountAlreadyVoided, FORBIDDEN)
        case CloseLisaAccountAlreadyClosedResponse =>
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorAccountAlreadyClosed, FORBIDDEN)
        case CloseLisaAccountCancellationPeriodExceeded =>
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorAccountCancellationPeriodExceeded, FORBIDDEN)
        case CloseLisaAccountWithinCancellationPeriod =>
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorAccountWithinCancellationPeriod, FORBIDDEN)
        case CloseLisaAccountNotFoundResponse =>
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorAccountNotFound, NOT_FOUND)
        case CloseLisaAccountErrorResponse => {
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR)
        }
      } recover {
        case e: Exception =>
          Logger.error(s"AccountController: closeAccount: An error occurred due to ${e.getMessage} returning internal server error")
          handleClosureFailure(lisaManager, accountId, closeLisaAccountRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR)
      }
    }
  }

  private def hasValidDatesForClosure(lisaManager: String, accountId: String, req: CloseLisaAccountRequest)
                                     (success: () => Future[Result])
                                     (implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {

    if (req.closureDate.isBefore(LISA_START_DATE)) {
      auditService.audit(
        auditType = "accountNotClosed",
        path = getCloseEndpointUrl(lisaManager, accountId),
        auditData = req.toStringMap ++ Map(ZREF -> lisaManager,
          "accountId" -> accountId,
          "reasonNotClosed" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.CLOSE)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("closureDate"), Some("/closureDate"))
      )))))
    }
    else {
      success()
    }
  }

  private def handleClosureFailure(lisaManager: String,
                                   accountId: String,
                                   requestData: Product,
                                   e: ErrorResponse,
                                   status: Int)
                                  (implicit hc: HeaderCarrier, startTime: Long) = {
    auditService.audit(
      auditType = "accountNotClosed",
      path = getCloseEndpointUrl(lisaManager, accountId),
      auditData = requestData.toStringMap ++ Map(ZREF -> lisaManager,
        "accountId" -> accountId,
        "reasonNotClosed" -> e.errorCode)
    )

    LisaMetrics.incrementMetrics(startTime, status, LisaMetricKeys.CLOSE)

    Status(status).apply(Json.toJson(e))
  }

  private def getCloseEndpointUrl(lisaManagerReferenceNumber: String, accountID: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountID/close-account"
  }

  //endregion

  //region Get Account

  private def hasAccountTransferData(js: JsObject): Boolean = {
    js.keys.contains("transferAccount")
  }

  private def processAccountCreation(lisaManager: String, creationRequest: CreateLisaAccountCreationRequest)
                                    (implicit hc: HeaderCarrier, startTime: Long) = {
    val action = "Created"

    hasValidDatesForCreation(lisaManager, creationRequest) { () =>
      service.createAccount(lisaManager, creationRequest).map {
        case CreateLisaAccountSuccessResponse(accountId) =>
          auditService.audit(
            auditType = "accountCreated",
            path = getCreateOrTransferEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap + (ZREF -> lisaManager)
          )

          val data = ApiResponseData(message = "Account created", accountId = Some(accountId))

          LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.ACCOUNT)

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
        case CreateLisaAccountInvestorNotFoundResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInvestorNotFound, FORBIDDEN, action)
        case CreateLisaAccountInvestorNotEligibleResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInvestorNotEligible, FORBIDDEN, action)
        case CreateLisaAccountInvestorComplianceCheckFailedResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInvestorComplianceCheckFailedCreateTransfer, FORBIDDEN, action)
        case CreateLisaAccountInvestorAccountAlreadyClosedResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorAccountAlreadyClosed, FORBIDDEN, action)
        case CreateLisaAccountInvestorAccountAlreadyVoidResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorAccountAlreadyVoided, FORBIDDEN, action)
        case CreateLisaAccountAlreadyExistsResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorAccountAlreadyExists(creationRequest.accountId), CONFLICT, action)
        case CreateLisaAccountErrorResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR, action)
      } recover {
        case e: Exception =>
          Logger.error(s"AccountController: An error occurred due to ${e.getMessage} returning internal server error")
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR, action)
      }
    }
  }

  //endregion

  //region Close Account

  private def processAccountTransfer(lisaManager: String, transferRequest: CreateLisaAccountTransferRequest)
                                    (implicit hc: HeaderCarrier, startTime: Long) = {
    val action = "Transferred"

    hasValidDatesForTransfer(lisaManager, transferRequest) { () =>
      service.transferAccount(lisaManager, transferRequest).map {
        case CreateLisaAccountSuccessResponse(accountId) =>
          auditService.audit(
            auditType = "accountTransferred",
            path = getCreateOrTransferEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap + (ZREF -> lisaManager)
          )

          val data = ApiResponseData(message = "Account transferred", accountId = Some(accountId))

          LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.ACCOUNT)

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
        case CreateLisaAccountInvestorNotFoundResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorInvestorNotFound, FORBIDDEN, action)
        case CreateLisaAccountInvestorComplianceCheckFailedResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorInvestorComplianceCheckFailedCreateTransfer, FORBIDDEN, action)
        case CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorPreviousAccountDoesNotExist, FORBIDDEN, action)
        case CreateLisaAccountInvestorAccountAlreadyClosedResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorAccountAlreadyClosed, FORBIDDEN, action)
        case CreateLisaAccountInvestorAccountAlreadyVoidResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorAccountAlreadyVoided, FORBIDDEN, action)
        case CreateLisaAccountAlreadyExistsResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorAccountAlreadyExists(transferRequest.accountId), CONFLICT, action)
        case CreateLisaAccountErrorResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR, action)
      } recover {
        case e: Exception =>
          Logger.error(s"AccountController: An error occurred in due to ${e.getMessage} returning internal server error")
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR, action)
      }
    }
  }

  private def hasValidDatesForCreation(lisaManager: String, creationRequest: CreateLisaAccountCreationRequest)
                                      (success: () => Future[Result])
                                      (implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {

    if (creationRequest.firstSubscriptionDate.isBefore(LISA_START_DATE)) {
      auditService.audit(
        auditType = "accountNotCreated",
        path = getCreateOrTransferEndpointUrl(lisaManager),
        auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
          "reasonNotCreated" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("firstSubscriptionDate"), Some("/firstSubscriptionDate"))
      )))))
    }
    else {
      success()
    }
  }

  private def hasValidDatesForTransfer(lisaManager: String, transferRequest: CreateLisaAccountTransferRequest)
                                      (success: () => Future[Result])
                                      (implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {

    if (
      transferRequest.firstSubscriptionDate.isBefore(LISA_START_DATE) ||
        transferRequest.transferAccount.transferInDate.isBefore(LISA_START_DATE)) {

      def firstSubscriptionDateError(request: CreateLisaAccountTransferRequest) = {
        if (transferRequest.firstSubscriptionDate.isBefore(LISA_START_DATE)) {
          Some(ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("firstSubscriptionDate"), Some("/firstSubscriptionDate")))
        }
        else {
          None
        }
      }

      def transferInDateError(request: CreateLisaAccountTransferRequest) = {
        if (transferRequest.transferAccount.transferInDate.isBefore(LISA_START_DATE)) {
          Some(ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("transferInDate"), Some("/transferAccount/transferInDate")))
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
        path = getCreateOrTransferEndpointUrl(lisaManager),
        auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
          "reasonNotCreated" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors))))
    }
    else {
      success()
    }
  }

  private def handleCreateOrTransferFailure(lisaManager: String,
                                            requestData: Product,
                                            e: ErrorResponse,
                                            status: Int,
                                            action: String)
                                           (implicit hc: HeaderCarrier, startTime: Long) = {
    auditService.audit(
      auditType = s"accountNot$action",
      path = getCreateOrTransferEndpointUrl(lisaManager),
      auditData = requestData.toStringMap ++ Map(
        ZREF -> lisaManager,
        s"reasonNotCreated" -> e.errorCode
      )
    )

    LisaMetrics.incrementMetrics(startTime, status, LisaMetricKeys.ACCOUNT)

    Status(status).apply(Json.toJson(e))
  }

  private def getCreateOrTransferEndpointUrl(lisaManagerReferenceNumber: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts"
  }

  //endregion

}