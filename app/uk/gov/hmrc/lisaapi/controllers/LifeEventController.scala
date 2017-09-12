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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global

class LifeEventController extends LisaController {

  val service: LifeEventService = LifeEventService
  val auditService: AuditService = AuditService

  def reportLisaLifeEvent(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime,LisaMetricKeys.EVENT)

      withValidLMRN(lisaManager) {
        withValidJson[ReportLifeEventRequest](req =>
          service.reportLifeEvent(lisaManager, accountId, req) map { res =>
            Logger.debug("Entering LifeEvent Controller and the response is " + res.toString)
            res match {
              case ReportLifeEventSuccessResponse(lifeEventId) => {
                Logger.debug("Matched Valid Response ")

                doAudit(lisaManager, accountId, req, "lifeEventReported")

                val data = ApiResponseData(message = "Life Event Created", lifeEventId = Some(lifeEventId))

                LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.EVENT)

                Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
              }
              case ReportLifeEventInappropriateResponse => {
                Logger.debug("Matched Inappropriate")

                doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorLifeEventInappropriate.errorCode))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.INVESTOR))

                Forbidden(Json.toJson(ErrorLifeEventInappropriate))
              }
              case ReportLifeEventAccountClosedResponse => {
                Logger.error(("Account Closed or VOID"))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.INVESTOR))

                Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
              }
              case ReportLifeEventAlreadyExistsResponse => {
                Logger.debug("Matched Already Exists")

                doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorLifeEventAlreadyExists.errorCode))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.INVESTOR))

                Conflict(Json.toJson(ErrorLifeEventAlreadyExists))
              }
              case ReportLifeEventAccountNotFoundResponse => {
                Logger.debug("Matched Not Found")

                doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(NOT_FOUND,LisaMetricKeys.INVESTOR))

                NotFound(Json.toJson(ErrorAccountNotFound))
              }
              case _ => {
                Logger.debug("Matched Error")

                doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorInternalServerError.errorCode))

                Logger.error(s"Life Event Not reported : DES unknown case , returning internal server error")
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.INVESTOR))

                InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            }
          }, lisaManager = lisaManager
        )
      }
  }

  private def doAudit(lisaManager: String, accountId: String, req: ReportLifeEventRequest, auditType: String, extraData: Map[String, String] = Map())(implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = auditType,
      path = getEndpointUrl(lisaManager, accountId),
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/events"
  }

}
