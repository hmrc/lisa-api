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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{MetricsEnum, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusPaymentService, LifeEventService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

class BonusPaymentController extends LisaController with LisaConstants {

  val service: BonusPaymentService = BonusPaymentService
  val auditService: AuditService = AuditService

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime,MetricsEnum.BONUS_PAYMENT)

      withValidLMRN(lisaManager) {
        withValidJson[RequestBonusPaymentRequest](req =>
          (req.bonuses.claimReason, req.lifeEventId) match {
            case ("Life Event", None) =>
              handleLifeEventNotProvided(lisaManager, accountId, req)
            case _ =>
              service.requestBonusPayment(lisaManager, accountId, req) map { res =>
                Logger.debug("Entering Bonus Payment Controller and the response is " + res.toString)

                LisaMetrics.incrementMetrics(System.currentTimeMillis(), MetricsEnum.BONUS_PAYMENT)
                res match {
                  case RequestBonusPaymentSuccessResponse(transactionID) =>
                    handleSuccess(lisaManager, accountId, req, transactionID)
                  case errorResponse: RequestBonusPaymentErrorResponse =>
                    handleFailure(lisaManager, accountId, req, errorResponse)
                }
              } recover {
                case e: Exception => Logger.error(s"requestBonusPayment : An error occurred due to ${e.getMessage} returning internal server error")
                  handleError(lisaManager, accountId, req)

              }
          }, lisaManager = lisaManager
        )
      }
  }

  private def handleLifeEventNotProvided(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)(implicit hc: HeaderCarrier) = {
    Logger.debug("Life event not provided")

    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> ErrorLifeEventNotProvided.errorCode)
    )

    Future.successful(Forbidden(Json.toJson(ErrorLifeEventNotProvided)))
  }

  private def handleSuccess(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, transactionID: String)(implicit hc: HeaderCarrier) = {
    Logger.debug("Matched success response")
    val data = ApiResponseData(message = "Bonus transaction created", transactionId = Some(transactionID))

    auditService.audit(
      auditType = "bonusPaymentRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req)
    )

    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
  }

  private def handleFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, errorResponse: RequestBonusPaymentErrorResponse)(implicit hc: HeaderCarrier) = {
    Logger.debug("Matched failure response")

    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> errorResponse.data.code)
    )

    Status(errorResponse.status).apply(Json.toJson(errorResponse.data))
  }

  private def handleError(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)(implicit hc: HeaderCarrier) = {
    Logger.debug("An error occurred")

    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> ErrorInternalServerError.errorCode)
    )

    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def createAuditData(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest): Map[String, String] = {
    req.toStringMap ++ Map(ZREF -> lisaManager,
      "accountId" -> accountId)
  }

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/transactions"
  }

}
