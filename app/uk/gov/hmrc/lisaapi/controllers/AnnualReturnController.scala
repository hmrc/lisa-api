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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class AnnualReturnController extends LisaController with LisaConstants {

  override val validateVersion: String => Boolean = _ == "2.0"
  val service: LifeEventService = LifeEventService
  val validator: AnnualReturnValidator = AnnualReturnValidator
  val auditService: AuditService = AuditService

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

                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.EVENT)
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  case error: ReportLifeEventResponse =>
                    val response = errors.getOrElse(error, ErrorInternalServerError).asResult

                    LisaMetrics.incrementMetrics(startTime, response.header.status, LisaMetricKeys.EVENT)
                    response
                }
              } recover {
                case e: Exception =>
                  Logger.error(s"submitAnnualReturn: An error occurred due to ${e.getMessage}, returning internal server error")

                  LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.EVENT)
                  InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            },
            lisaManager = lisaManager
          )
        }
      }
  }

  private val errors = Map[ReportLifeEventResponse, ErrorResponse](
    ReportLifeEventAccountNotFoundResponse -> ErrorAccountNotFound,
    ReportLifeEventAccountVoidResponse -> ErrorAccountAlreadyVoided,
    ReportLifeEventAccountCancelledResponse -> ErrorAccountAlreadyCancelled,
    ReportLifeEventMismatchResponse -> ErrorLifeEventMismatch,
    ReportLifeEventAlreadySupersededResponse -> ErrorLifeEventAlreadySuperseded,
    ReportLifeEventAlreadyExistsResponse -> ErrorLifeEventAlreadyExists
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
      auditService.audit(
        auditType = "lifeEventNotRequested",
        path = s"/manager/$lisaManager/accounts/$accountId/returns",
        auditData = req.toStringMap ++ Map(ZREF -> lisaManager, "accountId" -> accountId, "reasonNotRequested" -> "FORBIDDEN")
      )

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
    }
  }

}
