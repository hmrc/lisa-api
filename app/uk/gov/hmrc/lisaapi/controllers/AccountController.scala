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
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class AccountController extends LisaController with LisaConstants {

  val service: AccountService = AccountService
  val auditService: AuditService = AuditService

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
        case CreateLisaAccountServiceUnavailableResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorServiceUnavailable, SERVICE_UNAVAILABLE, action)
      } recover {
        case e: Exception =>
          Logger.error(s"AccountController: An error occurred due to ${e.getMessage} returning internal server error")
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInternalServerError, INTERNAL_SERVER_ERROR, action)
      }
    }
  }

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
        case CreateLisaAccountServiceUnavailableResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorServiceUnavailable, SERVICE_UNAVAILABLE, action)
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

}