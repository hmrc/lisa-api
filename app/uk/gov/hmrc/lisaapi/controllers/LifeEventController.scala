/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.{ExecutionContext, Future}

class LifeEventController @Inject()(
                                     authConnector: AuthConnector,
                                     appContext: AppContext,
                                     service: LifeEventService,
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

  def reportLisaLifeEvent(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader(parse) andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidJson[ReportLifeEventRequest](
        req => {
          withValidDates(lisaManager, accountId, req) { () =>
            service.reportLifeEvent(lisaManager, accountId, req) flatMap { res =>
              Logger.debug("Entering LifeEvent Controller and the response is " + res.toString)

              res match {
                case ReportLifeEventSuccessResponse(lifeEventId) =>
                  Logger.debug("Matched Valid Response ")
                  auditReportLifeEvent(lisaManager, accountId, req, success = true)
                  lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.EVENT)
                  val data = ApiResponseData(message = "Life event created", lifeEventId = Some(lifeEventId))
                  Future.successful(Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED))))
                case res: ReportLifeEventResponse =>
                  withApiVersion {
                    case Some(VERSION_1) =>
                      val errorResponse = v1errors.applyOrElse(res, { _: ReportLifeEventResponse =>
                        Logger.debug(s"Matched an unexpected response: $res, returning a 500 error")
                        ErrorInternalServerError
                      })
                      Future.successful(error(errorResponse, lisaManager, accountId, req))
                    case Some(VERSION_2) =>
                      val errorResponse = v2errors.applyOrElse(res, { _: ReportLifeEventResponse =>
                        Logger.debug(s"Matched an unexpected response: $res, returning a 500 error")
                        ErrorInternalServerError
                      })
                      Future.successful(error(errorResponse, lisaManager, accountId, req))
                  }
              }
            }
          }
        }, lisaManager = lisaManager
      )
    }

  private val commonErrors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = {
    case ReportLifeEventInappropriateResponse => ErrorLifeEventInappropriate
    case ReportLifeEventAlreadyExistsResponse(lifeEventId) => ErrorLifeEventAlreadyExists(lifeEventId)
    case ReportLifeEventAccountNotFoundResponse => ErrorAccountNotFound
    case ReportLifeEventServiceUnavailableResponse => ErrorServiceUnavailable
  }

  private val v1errors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = commonErrors.orElse({
    case ReportLifeEventAccountClosedOrVoidResponse => ErrorAccountAlreadyClosedOrVoid
  })

  private val v2errors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = commonErrors.orElse({
    case ReportLifeEventAccountClosedResponse => ErrorAccountAlreadyClosed
    case ReportLifeEventAccountCancelledResponse => ErrorAccountAlreadyCancelled
    case ReportLifeEventAccountVoidResponse => ErrorAccountAlreadyVoided
  })

  private def error(e: ErrorResponse, lisaManager: String, accountId: String, req: ReportLifeEventRequest)
                   (implicit hc: HeaderCarrier, startTime: Long): Result = {
    Logger.debug("Matched an error response")
    auditReportLifeEvent(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> e.errorCode))
    lisaMetrics.incrementMetrics(startTime, e.httpStatusCode, LisaMetricKeys.EVENT)
    e.asResult
  }

  private def auditReportLifeEvent(lisaManager: String, accountId: String, req: ReportLifeEventRequest, success: Boolean, extraData: Map[String, String] = Map())
                     (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "lifeEventReported" else "lifeEventNotReported",
      path = reportLifeEventEndpointUrl(lisaManager, accountId),
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

  private def withValidDates(lisaManager: String, accountId: String, req: ReportLifeEventRequest)
                            (success: () => Future[Result])
                            (implicit hc: HeaderCarrier, startTime: Long) = {
    if (req.eventDate.isBefore(LISA_START_DATE)) {
      Logger.debug("Life event not reported - invalid event date")

      auditReportLifeEvent(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> "FORBIDDEN"))
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.EVENT)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
      )))))
    } else {
      success()
    }
  }

  private def reportLifeEventEndpointUrl(lisaManager: String, accountId: String): String =
    s"/manager/$lisaManager/accounts/$accountId/events"

}
