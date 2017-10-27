/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, _}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

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

  private def processAccountCreation(lisaManager: String, creationRequest: CreateLisaAccountCreationRequest)(implicit hc: HeaderCarrier,startTime:Long) = {
    service.createAccount(lisaManager, creationRequest).map { result =>

      result match {
        case CreateLisaAccountSuccessResponse(accountId) => {
          auditService.audit(
            auditType = "accountCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap + (ZREF -> lisaManager)
          )
          val data = ApiResponseData(message = "Account Created.", accountId = Some(accountId))
          LisaMetrics.incrementMetrics(startTime,LisaMetricKeys.ACCOUNT)

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
        }
        case CreateLisaAccountInvestorNotFoundResponse => {
          auditService.audit(
            auditType = "accountNotCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorInvestorNotFound.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorInvestorNotFound))
        }
        case CreateLisaAccountInvestorNotEligibleResponse => {
          auditService.audit(
            auditType = "accountNotCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorInvestorNotEligible.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorInvestorNotEligible))
        }
        case CreateLisaAccountInvestorComplianceCheckFailedResponse => {
          auditService.audit(
            auditType = "accountNotCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorInvestorComplianceCheckFailed.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailed))
        }
        case CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse => {
          auditService.audit(
            auditType = "accountNotCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorAccountAlreadyClosedOrVoid.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
        }
        case CreateLisaAccountAlreadyExistsResponse => {
          val result = ErrorAccountAlreadyExists (creationRequest.accountId)
          auditService.audit(
            auditType = "accountNotCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> result.errorCode)
          )
          LisaMetrics.startMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(CONFLICT,LisaMetricKeys.ACCOUNT))

          Conflict(Json.toJson(result))
        }
        case _ => {
          auditService.audit(
            auditType = "accountNotCreated",
            path = getEndpointUrl(lisaManager),
            auditData = creationRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorInternalServerError.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.ACCOUNT))

          Logger.error(s"AccontController :createAccount unknown case from DES returning internal server error" )
          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    } recover {
      case e:Exception =>
        Logger.error(s"AccontController : An error occurred due to ${e.getMessage} returning internal server error")
        LisaMetrics.startMetrics(System.currentTimeMillis(),
          LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.ACCOUNT))

        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  private def processAccountTransfer(lisaManager: String, transferRequest: CreateLisaAccountTransferRequest)(implicit hc: HeaderCarrier,startTime:Long) = {
    service.transferAccount(lisaManager, transferRequest).map { result =>

      result match {
        case CreateLisaAccountSuccessResponse(accountId) => {
          auditService.audit(
            auditType = "accountTransferred",
            path = getEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap + (ZREF -> lisaManager)
          )
          val data = ApiResponseData(message = "Account Transferred.", accountId = Some(accountId))
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
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorInvestorNotFound))
        }
        case CreateLisaAccountInvestorComplianceCheckFailedResponse => {
          auditService.audit(
            auditType = "accountNotTransferred",
            path = getEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorInvestorComplianceCheckFailed.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailed))
        }
        case CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse => {
          auditService.audit(
            auditType = "accountNotTransferred",
            path = getEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorPreviousAccountDoesNotExist.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorPreviousAccountDoesNotExist))
        }
        case CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse => {
          auditService.audit(
            auditType = "accountNotTransferred",
            path = getEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorAccountAlreadyClosedOrVoid.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.ACCOUNT))

          Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
        }
        case CreateLisaAccountAlreadyExistsResponse => {
          val result = ErrorAccountAlreadyExists (transferRequest.accountId)
          auditService.audit(
            auditType = "accountNotTransferred",
            path = getEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> result.errorCode)
          )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(CONFLICT,LisaMetricKeys.ACCOUNT))

          Conflict(Json.toJson(result))
        }
        case _ => {
          auditService.audit(
            auditType = "accountNotTransferred",
            path = getEndpointUrl(lisaManager),
            auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "reasonNotCreated" -> ErrorInternalServerError.errorCode)
          )
          Logger.error(s"AccontController : transferAccount unknown case from DES returning internal server error" )
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.ACCOUNT))

          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    } recover {
      case e:Exception  =>     Logger.error(s"AccontController : An error occurred in due to ${e.getMessage} returning internal server error")
        LisaMetrics.incrementMetrics(System.currentTimeMillis(),
          LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.ACCOUNT))

        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  private def processAccountClosure(lisaManager: String, accountId: String, closeLisaAccountRequest: CloseLisaAccountRequest)(implicit hc: HeaderCarrier, startTime:Long) = {
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

          val data = ApiResponseData(message = "LISA Account Closed", accountId = Some(accountId))

          Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = 200)))
        }
        case CloseLisaAccountAlreadyClosedResponse => {
          auditService.audit(
            auditType = "accountNotClosed",
            path = getCloseEndpointUrl(lisaManager, accountId),
            auditData = closeLisaAccountRequest.toStringMap ++ Map(ZREF -> lisaManager,
              "accountId" -> accountId,
              "reasonNotClosed" -> ErrorAccountAlreadyClosedOrVoid.errorCode)
          )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.CLOSE))

          Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
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
            LisaMetricKeys.lisaError(NOT_FOUND,LisaMetricKeys.CLOSE))

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
          Logger.error(s"AccountController: closeAccount unknown case from DES returning internal server error" )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.CLOSE))

          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    } recover {
        case e:Exception  =>     Logger.error(s"AccountController: closeAccount: An error occurred due to ${e.getMessage} returning internal server error")
                              LisaMetrics.incrementMetrics(startTime,
                                LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.CLOSE))
                              InternalServerError(Json.toJson(ErrorInternalServerError))
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
