/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.libs.json.{JsObject, JsPath, Json, JsonValidationError}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.{ExecutionContext, Future}

class AccountController @Inject()(
                                   authConnector: AuthConnector,
                                   appContext: AppContext,
                                   service: AccountService,
                                   auditService: AuditService,
                                   lisaMetrics: LisaMetrics,
                                   cc: ControllerComponents,
                                   parse: PlayBodyParsers
                                 )(implicit ec: ExecutionContext) extends LisaController(
  cc: ControllerComponents,
  lisaMetrics: LisaMetrics,
  appContext: AppContext,
  authConnector: AuthConnector
) {

  def createOrTransferLisaAccount(lisaManager: String): Action[AnyContent] =
    (validateHeader(parse) andThen validateLMRN(lisaManager)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidJson[CreateLisaAccountRequest]({
        case createRequest: CreateLisaAccountCreationRequest =>
          if (hasAccountTransferData(request.body.asJson.get.as[JsObject])) {
            lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

            Future.successful(Forbidden(ErrorTransferAccountDataProvided.asJson))
          } else {
            processAccountCreation(lisaManager, createRequest)
          }
        case transferRequest: CreateLisaAccountTransferRequest => processAccountTransfer(lisaManager, transferRequest)
      },
        Some(
          errors => {
            logger.warn("[AccountController][createOrTransferLisaAccount] The errors are " + errorConverter.convert(errors))

            val transferAccountDataNotProvided = errors.exists {
              case (path: JsPath, errors: Seq[JsonValidationError]) =>
                path.toString().contains("/transferAccount") && errors.contains(JsonValidationError("error.path.missing"))
            }

            if (transferAccountDataNotProvided) {
              lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

              Future.successful(Forbidden(ErrorTransferAccountDataNotProvided.asJson))
            } else {
              lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.ACCOUNT)

              Future.successful(BadRequest(ErrorBadRequest(errorConverter.convert(errors)).asJson))
            }
          }
        ), lisaManager = lisaManager
      )
    }

  private def hasAccountTransferData(js: JsObject): Boolean = js.keys.contains("transferAccount")

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

          lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.ACCOUNT)

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
        case CreateLisaAccountAlreadyExistsResponse =>
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorAccountAlreadyExists(creationRequest.accountId), action)
        case error: CreateLisaAccountResponse =>
          val errorResponse = createLisaAccountErrorMap.getOrElse(error, ErrorInternalServerError)
          handleCreateOrTransferFailure(lisaManager, creationRequest, errorResponse, action)
      } recover {
        case e: Exception =>
          logger.error(s"AccountController: An error occurred due to ${e.getMessage} returning internal server error")
          handleCreateOrTransferFailure(lisaManager, creationRequest, ErrorInternalServerError, action)
      }
    }
  }

  private val createLisaAccountErrorMap = Map[CreateLisaAccountResponse, ErrorResponse](
    CreateLisaAccountInvestorNotEligibleResponse -> ErrorInvestorNotEligible,
    CreateLisaAccountInvestorNotFoundResponse -> ErrorInvestorNotFound,
    CreateLisaAccountInvestorComplianceCheckFailedResponse -> ErrorInvestorComplianceCheckFailedCreateTransfer,
    CreateLisaAccountInvestorAccountAlreadyClosedResponse -> ErrorAccountAlreadyClosed,
    CreateLisaAccountInvestorAccountAlreadyCancelledResponse -> ErrorAccountAlreadyCancelled,
    CreateLisaAccountInvestorAccountAlreadyVoidResponse -> ErrorAccountAlreadyVoided,
    CreateLisaAccountErrorResponse -> ErrorInternalServerError,
    CreateLisaAccountServiceUnavailableResponse -> ErrorServiceUnavailable
  )

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

          lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.ACCOUNT)

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
        case CreateLisaAccountAlreadyExistsResponse =>
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorAccountAlreadyExists(transferRequest.accountId), action)
        case error: CreateLisaAccountResponse =>
          val errorResponse = transferLisaAccountErrorMap.getOrElse(error, ErrorInternalServerError)
          handleCreateOrTransferFailure(lisaManager, transferRequest, errorResponse, action)
      } recover {
        case e: Exception =>
          logger.error(s"AccountController: An error occurred in due to ${e.getMessage} returning internal server error")
          handleCreateOrTransferFailure(lisaManager, transferRequest, ErrorInternalServerError, action)
      }
    }
  }

  private val transferLisaAccountErrorMap = Map[CreateLisaAccountResponse, ErrorResponse](
    CreateLisaAccountInvestorNotFoundResponse -> ErrorInvestorNotFound,
    CreateLisaAccountInvestorComplianceCheckFailedResponse -> ErrorInvestorComplianceCheckFailedCreateTransfer,
    CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse -> ErrorPreviousAccountDoesNotExist,
    CreateLisaAccountInvestorAccountAlreadyClosedResponse -> ErrorAccountAlreadyClosed,
    CreateLisaAccountInvestorAccountAlreadyCancelledResponse -> ErrorAccountAlreadyCancelled,
    CreateLisaAccountInvestorAccountAlreadyVoidResponse -> ErrorAccountAlreadyVoided,
    CreateLisaAccountErrorResponse -> ErrorInternalServerError,
    CreateLisaAccountServiceUnavailableResponse -> ErrorServiceUnavailable
  )

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

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

      Future.successful(Forbidden(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("firstSubscriptionDate"), Some("/firstSubscriptionDate"))
      )).asJson))
    } else {
      success()
    }
  }

  private def hasValidDatesForTransfer(lisaManager: String, transferRequest: CreateLisaAccountTransferRequest)
                                      (success: () => Future[Result])
                                      (implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {
    val firstSubscriptionDateError = Option(transferRequest.firstSubscriptionDate.isBefore(LISA_START_DATE))
      .collect { case true => ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("firstSubscriptionDate"), Some("/firstSubscriptionDate")) }
    val transferInDateError = Option(transferRequest.transferAccount.transferInDate.isBefore(LISA_START_DATE))
      .collect { case true => ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("transferInDate"), Some("/transferAccount/transferInDate")) }
    val errors = List(firstSubscriptionDateError, transferInDateError).flatten

    if (errors.nonEmpty) {
      auditService.audit(
        auditType = "accountNotTransferred",
        path = getCreateOrTransferEndpointUrl(lisaManager),
        auditData = transferRequest.toStringMap ++ Map(ZREF -> lisaManager,
          "reasonNotCreated" -> "FORBIDDEN")
      )

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.ACCOUNT)

      Future.successful(Forbidden(ErrorForbidden(errors).asJson))
    } else {
      success()
    }
  }

  private def handleCreateOrTransferFailure(lisaManager: String,
                                            requestData: Product,
                                            e: ErrorResponse,
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

    lisaMetrics.incrementMetrics(startTime, e.httpStatusCode, LisaMetricKeys.ACCOUNT)

    Status(e.httpStatusCode).apply(Json.toJson(e))
  }

  private def getCreateOrTransferEndpointUrl(lisaManagerReferenceNumber: String): String =
    s"/manager/$lisaManagerReferenceNumber/accounts"

}
