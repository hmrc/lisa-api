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
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusOrWithdrawalService, CurrentDateService, WithdrawalService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.lisaapi.utils.WithdrawalChargeValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WithdrawalController extends LisaController with LisaConstants {

  override val validateVersion: String => Boolean = _ == "2.0"
  val postService: WithdrawalService = WithdrawalService
  val getService: BonusOrWithdrawalService = BonusOrWithdrawalService
  val auditService: AuditService = AuditService
  val validator: WithdrawalChargeValidator = WithdrawalChargeValidator
  val dateTimeService: CurrentDateService = CurrentDateService

  def reportWithdrawalCharge(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader() andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()

    withValidJson[ReportWithdrawalChargeRequest](req =>
      withValidData(req)(lisaManager, accountId) { () =>
        withValidClaimPeriod(req)(lisaManager, accountId) { () =>
          postService.reportWithdrawalCharge(lisaManager, accountId, req) map { res =>
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
              handleFailure(lisaManager, accountId, req, ReportWithdrawalChargeError)
          }
        }
      },
      lisaManager = lisaManager
    )
  }

  def getWithdrawalCharge(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] = {
    validateHeader().async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            processGetWithdrawalCharge(lisaManager, accountId, transactionId)
          }
        }
      }

    }
  }

  private def processGetWithdrawalCharge(lisaManager:String, accountId:String, transactionId: String)
                                        (implicit hc: HeaderCarrier, startTime: Long) = {

    getService.getBonusOrWithdrawal(lisaManager, accountId, transactionId).map {
      case response: GetWithdrawalResponse =>
        LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.WITHDRAWAL_CHARGE)
        Ok(Json.toJson(response))

      case _: GetBonusResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(Json.toJson(ErrorWithdrawalNotFound))

      case GetBonusOrWithdrawalTransactionNotFoundResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(Json.toJson(ErrorWithdrawalNotFound))

      case GetBonusOrWithdrawalInvestorNotFoundResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(Json.toJson(ErrorAccountNotFound))

      case GetBonusOrWithdrawalServiceUnavailableResponse =>
        LisaMetrics.incrementMetrics(startTime, SERVICE_UNAVAILABLE, LisaMetricKeys.WITHDRAWAL_CHARGE)
        ServiceUnavailable(Json.toJson(ErrorServiceUnavailable))

      case _ =>
        LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.WITHDRAWAL_CHARGE)
        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  private def withValidClaimPeriod(data: ReportWithdrawalChargeRequest)
                                  (lisaManager: String, accountId: String)
                                  (callback: () => Future[Result])
                                  (implicit hc: HeaderCarrier, startTime: Long) = {

    val lastClaimDate = dateTimeService.now().withTime(0, 0, 0, 0).minusYears(6).minusDays(14)

    val claimCanStillBeMade = data.claimPeriodEndDate.isAfter(lastClaimDate.minusDays(1))

    if (claimCanStillBeMade) {
      callback()
    }
    else {
      auditFailure(lisaManager, accountId, data, ErrorWithdrawalTimescalesExceeded.errorCode)

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

      Future.successful(Forbidden(Json.toJson(ErrorWithdrawalTimescalesExceeded)))
    }
  }

  private def withValidData(data: ReportWithdrawalChargeRequest)
                           (lisaManager: String, accountId: String)
                           (callback: () => Future[Result])
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    val errors = validator.validate(data)

    if (errors.isEmpty) {
      callback()
    }
    else {
      auditFailure(lisaManager, accountId, data, "FORBIDDEN")

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
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

  private def handleFailure(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest, errorResponse: ReportWithdrawalChargeErrorResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    val error = errorMap.getOrElse(errorResponse, ErrorInternalServerError)

    auditFailure(lisaManager, accountId, req, error.errorCode)
    LisaMetrics.incrementMetrics(startTime, error.httpStatusCode, LisaMetricKeys.WITHDRAWAL_CHARGE)

    error.asResult
  }

  val errorMap = Map[ReportWithdrawalChargeErrorResponse, ErrorResponse](
    ReportWithdrawalChargeServiceUnavailable -> ErrorServiceUnavailable,
    ReportWithdrawalChargeAlreadyExists -> ErrorWithdrawalExists,
    ReportWithdrawalChargeAccountNotFound -> ErrorAccountNotFound,
    ReportWithdrawalChargeSupersedeOutcomeError -> ErrorWithdrawalSupersededOutcomeError,
    ReportWithdrawalChargeSupersedeAmountMismatch -> ErrorWithdrawalSupersededAmountMismatch,
    ReportWithdrawalChargeAlreadySuperseded -> ErrorWithdrawalAlreadySuperseded,
    ReportWithdrawalChargeReportingError -> ErrorWithdrawalReportingError,
    ReportWithdrawalChargeAccountVoid -> ErrorAccountAlreadyVoided,
    ReportWithdrawalChargeAccountCancelled -> ErrorAccountAlreadyCancelled
  )

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
