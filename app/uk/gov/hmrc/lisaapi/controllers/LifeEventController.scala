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
import play.api.mvc.{Action, AnyContent, Result}
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
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[ReportLifeEventRequest](
            req => {
              withValidDates(lisaManager, accountId, req) { () =>
                service.reportLifeEvent(lisaManager, accountId, req) map { res =>
                  Logger.debug("Entering LifeEvent Controller and the response is " + res.toString)
                  handleReportLifeEventResponse(lisaManager, accountId, startTime, req, res)
                }
              }
            },
            lisaManager = lisaManager
          )
        }
      }
  }

  private def handleReportLifeEventResponse(lisaManager: String,
                                             accountId: String,
                                             startTime: Long,
                                             req: ReportLifeEventRequest,
                                             res: ReportLifeEventResponse)
                                           (implicit hc: HeaderCarrier) = {
    res match {
      case ReportLifeEventSuccessResponse(lifeEventId) => {
        Logger.debug("Matched Valid Response ")
        doAudit(lisaManager, accountId, req, "lifeEventReported")
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(CREATED, LisaMetricKeys.EVENT))
        val data = ApiResponseData(message = "Life event created", lifeEventId = Some(lifeEventId))
        Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
      }
      case ReportLifeEventInappropriateResponse => {
        Logger.debug("Matched Inappropriate")
        doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorLifeEventInappropriate.errorCode))
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(FORBIDDEN, LisaMetricKeys.EVENT))
        Forbidden(Json.toJson(ErrorLifeEventInappropriate))
      }
      case ReportLifeEventAccountClosedResponse => {
        Logger.debug("Account Closed or VOID")
        doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorAccountAlreadyClosedOrVoid.errorCode))
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(FORBIDDEN, LisaMetricKeys.EVENT))
        Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
      }
      case ReportLifeEventAlreadyExistsResponse(lifeEventId) => {
        val result = ErrorLifeEventAlreadyExists(lifeEventId)
        Logger.debug("Matched Already Exists")
        doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> result.errorCode))
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(CONFLICT, LisaMetricKeys.EVENT))
        Conflict(Json.toJson(result))
      }
      case ReportLifeEventAccountNotFoundResponse => {
        Logger.debug("Matched Account Not Found")
        doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(NOT_FOUND, LisaMetricKeys.EVENT))
        NotFound(Json.toJson(ErrorAccountNotFound))
      }
      case _ => {
        Logger.debug("Matched Error")
        doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> ErrorInternalServerError.errorCode))
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(INTERNAL_SERVER_ERROR, LisaMetricKeys.EVENT))
        InternalServerError(Json.toJson(ErrorInternalServerError))
      }
    }
  }

  private def withValidDates(lisaManager: String, accountId: String, req: ReportLifeEventRequest)
                            (success: () => Future[Result])
                            (implicit hc: HeaderCarrier, startTime: Long) = {
    if (req.eventDate.isBefore(LISA_START_DATE)) {
      Logger.debug("Life event not reported - invalid event date")

      doAudit(lisaManager, accountId, req, "lifeEventNotReported", Map("reasonNotReported" -> "FORBIDDEN"))
      LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(FORBIDDEN, LisaMetricKeys.EVENT))

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
      )))))
    }
    else {
      success()
    }
  }

  def getLifeEvent(lisaManager: String, accountId: String, eventId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            service.getLifeEvent(lisaManager, accountId, eventId) map { res =>
              Logger.debug("Entering LifeEvent Controller GET and the response is " + res.toString)

              res match {
                case success: RequestLifeEventSuccessResponse => {
                  Logger.debug("Matched Valid Response ")

                  LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(OK, LisaMetricKeys.EVENT))

                  Ok(Json.toJson(success))
                }
                case ReportLifeEventAccountNotFoundResponse => {
                  Logger.debug("Matched Account Not Found")

                  LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(NOT_FOUND, LisaMetricKeys.EVENT))

                  NotFound(Json.toJson(ErrorAccountNotFound))
                }
                case ReportLifeEventIdNotFoundResponse => {
                  Logger.debug("Matched Life Event Not Found")

                  LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(NOT_FOUND, LisaMetricKeys.EVENT))

                  NotFound(Json.toJson(ErrorLifeEventIdNotFound))
                }
                case _ => {
                  Logger.debug("Matched Error")
                  Logger.error("Life Event Not returned : DES unknown case , returning internal server error")

                  LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(INTERNAL_SERVER_ERROR, LisaMetricKeys.EVENT))

                  InternalServerError(Json.toJson(ErrorInternalServerError))
                }
              }
            }
          }
        }
      }
    }

  private def doAudit(lisaManager: String, accountId: String, req: ReportLifeEventRequest, auditType: String, extraData: Map[String, String] = Map())
                     (implicit hc: HeaderCarrier) = {
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
