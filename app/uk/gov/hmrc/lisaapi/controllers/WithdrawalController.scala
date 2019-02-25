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
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusOrWithdrawalService, CurrentDateService, WithdrawalService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.lisaapi.utils.WithdrawalChargeValidator

import scala.concurrent.{ExecutionContext, Future}

class WithdrawalController @Inject() (
                                       val authConnector: AuthConnector,
                                       val appContext: AppContext,
                                       postService: WithdrawalService,
                                       getService: BonusOrWithdrawalService,
                                       auditService: AuditService,
                                       validator: WithdrawalChargeValidator,
                                       dateTimeService: CurrentDateService,
                                       val lisaMetrics: LisaMetrics
                                     )(implicit ec: ExecutionContext) extends LisaController {

  override val validateVersion: String => Boolean = _ == "2.0"

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
        lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.WITHDRAWAL_CHARGE)
        Ok(Json.toJson(response))

      case _: GetBonusResponse =>
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(Json.toJson(ErrorWithdrawalNotFound))

      case GetBonusOrWithdrawalTransactionNotFoundResponse =>
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(Json.toJson(ErrorWithdrawalNotFound))

      case GetBonusOrWithdrawalInvestorNotFoundResponse =>
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(Json.toJson(ErrorAccountNotFound))

      case GetBonusOrWithdrawalServiceUnavailableResponse =>
        lisaMetrics.incrementMetrics(startTime, SERVICE_UNAVAILABLE, LisaMetricKeys.WITHDRAWAL_CHARGE)
        ServiceUnavailable(Json.toJson(ErrorServiceUnavailable))

      case _ =>
        lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.WITHDRAWAL_CHARGE)
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

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

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

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

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
          path = endpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "no")
        )

        data
      case _:ReportWithdrawalChargeLateResponse =>
        val data = ApiResponseData(message = "Unauthorised withdrawal transaction created - late notification", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "withdrawalChargeRequested",
          path = endpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "yes")
        )

        data
      case _:ReportWithdrawalChargeSupersededResponse =>
        val data = ApiResponseData(message = "Unauthorised withdrawal transaction superseded", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "withdrawalChargeRequested",
          path = endpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req)
        )

        data
    }

    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.WITHDRAWAL_CHARGE)

    Created(Json.toJson(ApiResponse(data = Some(responseData), success = true, status = CREATED)))
  }

  private def handleFailure(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest, errorResponse: ReportWithdrawalChargeErrorResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    val error = errorOutcomes.applyOrElse(errorResponse, {_ : ReportWithdrawalChargeErrorResponse => ErrorInternalServerError})

    auditFailure(lisaManager, accountId, req, error.errorCode)
    lisaMetrics.incrementMetrics(startTime, error.httpStatusCode, LisaMetricKeys.WITHDRAWAL_CHARGE)

    error.asResult
  }

  val errorOutcomes:  PartialFunction[ReportWithdrawalChargeErrorResponse, ErrorResponse] = {
    case ReportWithdrawalChargeServiceUnavailable => ErrorServiceUnavailable
    case ReportWithdrawalChargeAlreadyExists(transactionId) => ErrorWithdrawalExists(transactionId)
    case ReportWithdrawalChargeAccountNotFound => ErrorAccountNotFound
    case ReportWithdrawalChargeSupersedeOutcomeError => ErrorWithdrawalSupersededOutcomeError
    case ReportWithdrawalChargeSupersedeAmountMismatch => ErrorWithdrawalSupersededAmountMismatch
    case ReportWithdrawalChargeAlreadySuperseded(transactionId) => ErrorWithdrawalAlreadySuperseded(transactionId)
    case ReportWithdrawalChargeReportingError => ErrorWithdrawalReportingError
    case ReportWithdrawalChargeAccountVoid => ErrorAccountAlreadyVoided
    case ReportWithdrawalChargeAccountCancelled => ErrorAccountAlreadyCancelled
  }

  private def auditFailure(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest, failureReason: String)
                          (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "withdrawalChargeNotRequested",
      path = endpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> failureReason)
    )
  }

  private def createAuditData(lisaManager: String, accountId: String, req: ReportWithdrawalChargeRequest): Map[String, String] = {
    req.toStringMap ++ Map(ZREF -> lisaManager,
      "accountId" -> accountId)
  }

  private def endpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"
  }
}
