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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.DesResponse
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusPaymentService}
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class BonusPaymentController extends LisaController with LisaConstants {

  val service: BonusPaymentService = BonusPaymentService
  val auditService: AuditService = AuditService
  val validator: BonusPaymentValidator = BonusPaymentValidator

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime,LisaMetricKeys.BONUS_PAYMENT)

      withValidLMRN(lisaManager) {
        withValidJson[RequestBonusPaymentRequest](req =>
          (req.bonuses.claimReason, req.lifeEventId) match {
            case ("Life Event", None) =>
              handleLifeEventNotProvided(lisaManager, accountId, req)
            case _ =>
              withValidData(req)(lisaManager, accountId) { () =>
                service.requestBonusPayment(lisaManager, accountId, req) map { res =>
                  Logger.debug("Entering Bonus Payment Controller and the response is " + res.toString)

                  res match {
                    case successResponse:RequestBonusPaymentSuccessResponse =>
                      LisaMetrics.incrementMetrics(System.currentTimeMillis(), LisaMetricKeys.BONUS_PAYMENT)
                      handleSuccess(lisaManager, accountId, req, successResponse)
                    case errorResponse: RequestBonusPaymentErrorResponse =>
                      handleFailure(lisaManager, accountId, req, errorResponse)
                  }
                } recover {
                  case e: Exception => Logger.error(s"requestBonusPayment : An error occurred due to ${e.getMessage} returning internal server error")
                    handleError(lisaManager, accountId, req)

                }
              }
          }, lisaManager = lisaManager
        )
      }
  }

  def getBonusPayment(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime, LisaMetricKeys.BONUS_PAYMENT)
      withValidLMRN(lisaManager) {
        withValidAccountId(accountId) {
          processGetBonusPayment(lisaManager, accountId, transactionId)
        }
      }
    }

  private def processGetBonusPayment(lisaManager:String, accountId:String, transactionId: String)(implicit hc: HeaderCarrier, startTime:Long) = {
      service.getBonusPayment(lisaManager, accountId, transactionId).map { result =>
        result match {
        case response : GetBonusPaymentSuccessResponse => {
          LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.BONUS_PAYMENT)
          Ok(Json.toJson(response))
        }

        case GetBonusPaymentLmrnDoesNotExistResponse => {
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
          LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT))
          BadRequest(Json.toJson(ErrorBadRequestLmrn))
        }

        case GetBonusPaymentTransactionNotFoundResponse => {
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
            LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT))
          NotFound(Json.toJson(ErrorTransactionNotFound))
        }

        case GetBonusPaymentInvestorNotFoundResponse => {
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
          LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT))
          NotFound(Json.toJson(ErrorAccountNotFound))
        }

        case _ => {
          LisaMetrics.incrementMetrics(System.currentTimeMillis(),
          LisaMetricKeys.lisaError(FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT))
          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    }
  }

  private def withValidData(data: RequestBonusPaymentRequest)
                           (lisaManager: String, accountId: String)
                           (callback: () => Future[Result])
                           (implicit hc: HeaderCarrier) = {
    val errors = validator.validate(data)

    if (errors.isEmpty) {
      callback()
    }
    else {
      auditFailure(lisaManager, accountId, data, "FORBIDDEN")
      LisaMetrics.incrementMetrics(System.currentTimeMillis(),
        LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.BONUS_PAYMENT))

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
    }
  }

  private def handleSuccess(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, resp: RequestBonusPaymentSuccessResponse)(implicit hc: HeaderCarrier) = {
    Logger.debug("Matched success response")
    val data = ApiResponseData(message = resp.message, transactionId = Some(resp.transactionId))

    auditService.audit(
      auditType = "bonusPaymentRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = (createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> (if(resp.message == NOTIFICATION_MSG) "yes" else "no")))
    )

    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
  }

  private def handleFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, errorResponse: RequestBonusPaymentErrorResponse)
                           (implicit hc: HeaderCarrier) = {
    Logger.debug("Matched failure response")

    auditFailure(lisaManager, accountId, req, errorResponse.data.code)
    LisaMetrics.incrementMetrics(System.currentTimeMillis(),
      LisaMetricKeys.lisaError(errorResponse.status,LisaMetricKeys.BONUS_PAYMENT))

    Status(errorResponse.status).apply(Json.toJson(errorResponse.data))
  }

  private def handleError(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)(implicit hc: HeaderCarrier) = {
    Logger.debug("An error occurred")

    auditFailure(lisaManager, accountId, req, ErrorInternalServerError.errorCode)
    LisaMetrics.incrementMetrics(System.currentTimeMillis(),
      LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.BONUS_PAYMENT))

    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def handleLifeEventNotProvided(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)(implicit hc: HeaderCarrier) = {
    Logger.debug("Life event not provided")

    auditFailure(lisaManager, accountId, req, ErrorLifeEventNotProvided.errorCode)
    LisaMetrics.incrementMetrics(System.currentTimeMillis(),
      LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.BONUS_PAYMENT))

    Future.successful(Forbidden(Json.toJson(ErrorLifeEventNotProvided)))
  }

  private def auditFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, failureReason: String)(implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> failureReason)
    )
  }

  private def createAuditData(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest): Map[String, String] = {
    req.toStringMap ++ Map(ZREF -> lisaManager,
      "accountId" -> accountId)
  }

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/transactions"
  }

  private def getBonusPaymentEndPointUrl(lisaManager: String, accountId: String, transactionId: Int): String = {
    s"/manager/$lisaManager/accounts/$accountId/transactions/$transactionId"
  }
}
