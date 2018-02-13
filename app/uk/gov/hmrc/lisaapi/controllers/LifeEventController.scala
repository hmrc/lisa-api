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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants

import scala.concurrent.Future

class LifeEventController extends LisaController with LisaConstants {

  val service: LifeEventService = LifeEventService
  val auditService: AuditService = AuditService

  def reportLisaLifeEvent(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime, LisaMetricKeys.EVENT)

      withValidLMRN(lisaManager) { () =>
        withValidJson[ReportLifeEventRequest](req => {
            if (req.eventDate.isBefore(LISA_START_DATE)) {
              Logger.debug("Life event not reported - invalid event date")

              doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> "FORBIDDEN"))
              LisaMetrics.incrementMetrics(startTime,
                LisaMetricKeys.lisaError(FORBIDDEN, request.uri))

              Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
              )))))
            }
            else {
              service.reportLifeEvent(lisaManager, accountId, req) map { res =>
                Logger.debug("Entering LifeEvent Controller and the response is " + res.toString)
                res match {
                  case ReportLifeEventSuccessResponse(lifeEventId) => {
                    Logger.debug("Matched Valid Response ")

                    doAudit(lisaManager, accountId, req, "lifeEventReported")

                    val data = ApiResponseData(message = "Life event created", lifeEventId = Some(lifeEventId))

                    LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.EVENT)

                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
                  }
                  case ReportLifeEventInappropriateResponse => {
                    Logger.debug("Matched Inappropriate")

                    doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorLifeEventInappropriate.errorCode))
                    LisaMetrics.incrementMetrics(startTime,
                      LisaMetricKeys.lisaError(FORBIDDEN, request.uri))

                    Forbidden(Json.toJson(ErrorLifeEventInappropriate))
                  }
                  case ReportLifeEventAccountClosedResponse => {
                    Logger.error(("Account Closed or VOID"))
                    LisaMetrics.incrementMetrics(startTime,
                      LisaMetricKeys.lisaError(FORBIDDEN, request.uri))

                    Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
                  }
                  case ReportLifeEventAlreadyExistsResponse(lifeEventId) => {
                    val result = ErrorLifeEventAlreadyExists(lifeEventId)
                    Logger.debug("Matched Already Exists")

                    doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> result.errorCode))
                    LisaMetrics.incrementMetrics(startTime,
                      LisaMetricKeys.getErrorKey(CONFLICT, request.uri))

                    Conflict(Json.toJson(result))
                  }
                  case ReportLifeEventAccountNotFoundResponse => {
                    Logger.debug("Matched Not Found")

                    doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime,
                      LisaMetricKeys.getErrorKey(NOT_FOUND, request.uri))

                    NotFound(Json.toJson(ErrorAccountNotFound))
                  }
                  case _ => {
                    Logger.debug("Matched Error")

                    doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorInternalServerError.errorCode))

                    Logger.error(s"Life Event Not reported : DES unknown case , returning internal server error")
                    LisaMetrics.incrementMetrics(startTime,
                      LisaMetricKeys.getErrorKey(INTERNAL_SERVER_ERROR, request.uri))

                    InternalServerError(Json.toJson(ErrorInternalServerError))
                  }
                }
              }
            }
          }, lisaManager = lisaManager
        )
      }
  }

  def getLifeEvent(lisaManager: String, accountId: String, eventId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime, LisaMetricKeys.EVENT)

      withValidLMRN(lisaManager) { () =>
        service.getLifeEvent(lisaManager, accountId, eventId) map { res =>
          Logger.debug("Entering LifeEvent Controller GET and the response is " + res.toString)
          res match {
            case success: RequestLifeEventSuccessResponse => {
              Logger.debug("Matched Valid Response ")

              LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.EVENT)

              Ok(Json.toJson(success))
            }
            case ReportLifeEventAccountNotFoundResponse => {
              Logger.debug("Matched Not Found")

              LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getErrorKey(NOT_FOUND, request.uri))

              NotFound(Json.toJson(ErrorAccountNotFound))
            }
            case ReportLifeEventIdNotFoundResponse => {
              Logger.debug("Matched Not Found")

              LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getErrorKey(NOT_FOUND, request.uri))

              NotFound(Json.toJson(ErrorLifeEventIdNotFound))
            }
            case _ => {
              Logger.debug("Matched Error")
              Logger.error("Life Event Not returned : DES unknown case , returning internal server error")

              LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getErrorKey(INTERNAL_SERVER_ERROR, request.uri))

              InternalServerError(Json.toJson(ErrorInternalServerError))
            }
          }
        }
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
