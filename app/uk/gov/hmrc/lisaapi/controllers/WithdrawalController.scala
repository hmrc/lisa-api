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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, CurrentDateService, WithdrawalService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global

class WithdrawalController extends LisaController with LisaConstants {

  val service: WithdrawalService = WithdrawalService
  val auditService: AuditService = AuditService
  val dateTimeService: CurrentDateService = CurrentDateService

  def reportWithdrawalCharge(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[ReportWithdrawalChargeRequest](req =>
            service.reportWithdrawalCharge(lisaManager, accountId, req) map { res =>
              Logger.debug("reportWithdrawalCharge: The response is " + res.toString)

              res match {
                case successResponse: ReportWithdrawalChargeSuccessResponse =>
                  handleSuccess(lisaManager, accountId, req, successResponse)
                case errorResponse: ReportWithdrawalChargeErrorResponse =>
                  handleFailure(lisaManager, accountId, req, errorResponse)
              }
            } recover {
              case e: Exception =>
                Logger.error(s"reportWithdrawalCharge: An error occurred due to ${e.getMessage}, returning internal server error")
                handleError(lisaManager, accountId, req)
            },
            lisaManager = lisaManager
          )
        }
      }
  }

  private def handleSuccess(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest, resp: ReportWithdrawalChargeSuccessResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Matched success response")

    val responseData = resp match {
      case _:ReportWithdrawalChargeOnTimeResponse =>
        val data = ApiResponseData(message = "Unauthorised withdrawal transaction created", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "withdrawalChargeRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "no")
        )

        data
      case _:ReportWithdrawalChargeLateResponse =>
        val data = ApiResponseData(message = "Unauthorised withdrawal transaction created - late notification", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "withdrawalChargeRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "yes")
        )

        data
      case _:ReportWithdrawalChargeSupersededResponse =>
        val data = ApiResponseData(message = "Unauthorised withdrawal transaction superseded", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "withdrawalChargeRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req)
        )

        data
    }

    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.WITHDRAWAL_CHARGE)

    Created(Json.toJson(ApiResponse(data = Some(responseData), success = true, status = CREATED)))
  }

  // scalastyle:off cyclomatic.complexity method.length
  private def handleFailure(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest, errorResponse: ReportWithdrawalChargeErrorResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Matched failure response")

    errorResponse match {
      case ReportWithdrawalChargeAccountNotFound => {
        auditFailure(lisaManager, accountId, req, ErrorAccountNotFound.errorCode)
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)

        NotFound(Json.toJson(ErrorAccountNotFound))
      }
      case _ =>
        auditFailure(lisaManager, accountId, req, ErrorInternalServerError.errorCode)
        LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.WITHDRAWAL_CHARGE)

        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  private def handleError(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest)
                         (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("An error occurred")

    auditFailure(lisaManager, accountId, req, ErrorInternalServerError.errorCode)
    LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.WITHDRAWAL_CHARGE)

    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def auditFailure(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest, failureReason: String)
                          (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "withdrawalChargeNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> failureReason)
    )
  }

  private def createAuditData(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest): Map[String, String] = {
    req.toStringMap ++ Map(ZREF -> lisaManager,
      "accountId" -> accountId)
  }

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"
  }
}
