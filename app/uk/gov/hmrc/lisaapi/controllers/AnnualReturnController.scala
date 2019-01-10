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
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.{LisaConstants, models}
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class AnnualReturnController @Inject()(
                                       val authConnector: AuthConnector,
                                       val appContext: AppContext,
                                       service: LifeEventService,
                                       auditService: AuditService,
                                       validator: AnnualReturnValidator,
                                       val lisaMetrics: LisaMetrics
                                      ) extends LisaController {

  override val validateVersion: String => Boolean = _ == "2.0"

  def submitReturn(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader().async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[AnnualReturn]( req =>
            withValidData(req)(lisaManager, accountId) { () =>
              service.reportLifeEvent(lisaManager, accountId, req) map { res =>
                Logger.debug("submitAnnualReturn: The response is " + res.toString)

                res match {
                  case success: ReportLifeEventSuccessResponse =>
                    val message = if (req.supersede.isEmpty) "Life event created" else "Life event superseded"
                    val data = ApiResponseData(message = message, lifeEventId = Some(success.lifeEventId))

                    audit(lisaManager, accountId, req)
                    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.EVENT)
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  case error: ReportLifeEventResponse =>
                    val response = errors.getOrElse(error, ErrorInternalServerError)

                    audit(lisaManager, accountId, req, Some(response.errorCode))
                    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.EVENT)
                    response.asResult
                }
              } recover {
                case e: Exception =>
                  Logger.error(s"submitAnnualReturn: An error occurred due to ${e.getMessage}, returning internal server error")

                  audit(lisaManager, accountId, req, Some("INTERNAL_SERVER_ERROR"))
                  lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.EVENT)
                  InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            },
            lisaManager = lisaManager
          )
        }
      }
  }

  private def audit(lisaManager: String, accountId: String, req: AnnualReturn, failureCode: Option[String] = None)
                   (implicit hc: HeaderCarrier) = {
    val (auditType, auditData) = failureCode match {
      case Some(code) => ("lifeEventNotRequested", req.toStringMap ++ Map("reasonNotRequested" -> code))
      case _ => ("lifeEventRequested", req.toStringMap)
    }
    auditService.audit(
      auditType = auditType,
      path = s"/manager/$lisaManager/accounts/$accountId/returns",
      auditData = auditData ++ Map(ZREF -> lisaManager, ACCOUNTID -> accountId, "eventType" -> "Statutory Submission")
    )
  }

  private val errors = Map[ReportLifeEventResponse, ErrorResponse](
    ReportLifeEventAccountNotFoundResponse -> ErrorAccountNotFound,
    ReportLifeEventAccountVoidResponse -> ErrorAccountAlreadyVoided,
    ReportLifeEventAccountCancelledResponse -> ErrorAccountAlreadyCancelled,
    ReportLifeEventMismatchResponse -> ErrorLifeEventMismatch,
    ReportLifeEventAlreadySupersededResponse -> ErrorLifeEventAlreadySuperseded,
    ReportLifeEventAlreadyExistsResponse -> ErrorLifeEventAlreadyExists,
    ReportLifeEventServiceUnavailableResponse -> ErrorServiceUnavailable
  )

  private def withValidData(req: AnnualReturn)
                           (lisaManager: String, accountId: String)
                           (callback: () => Future[Result])
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    val errors = validator.validate(req)

    if (errors.isEmpty) {
      callback()
    }
    else {
      audit(lisaManager, accountId, req, Some("FORBIDDEN"))
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)
      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
    }
  }

}
