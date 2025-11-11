/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc._
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
  authConnector: AuthConnector,
  appContext: AppContext,
  postService: WithdrawalService,
  getService: BonusOrWithdrawalService,
  auditService: AuditService,
  validator: WithdrawalChargeValidator,
  dateTimeService: CurrentDateService,
  lisaMetrics: LisaMetrics,
  cc: ControllerComponents,
  parse: PlayBodyParsers
)(implicit ec: ExecutionContext)
    extends LisaController(
      cc: ControllerComponents,
      lisaMetrics: LisaMetrics,
      appContext: AppContext,
      authConnector: AuthConnector
    ) {

  override val validateVersion: String => Boolean = _ == "2.0"

  def reportWithdrawalCharge(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader(parse) andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async {
      implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        logger.info(s"[WithdrawalController][reportWithdrawalCharge]  accountId : $accountId, lisaManager : $lisaManager")
        withValidJson[ReportWithdrawalChargeRequest](
          req =>
            withValidData(req)(lisaManager, accountId) { () =>
              withValidClaimPeriod(req)(lisaManager, accountId) { () =>
                postService.reportWithdrawalCharge(lisaManager, accountId, req) map { res =>
                  logger.info(s"[WithdrawalController][reportWithdrawalCharge]  response : ${res.toString} accountId : $accountId, lisaManager : $lisaManager")
                  res match {
                    case successResponse: ReportWithdrawalChargeSuccessResponse =>
                      handleSuccess(lisaManager, accountId, req, successResponse)
                    case errorResponse: ReportWithdrawalChargeErrorResponse     =>
                      logger.error(s"[WithdrawalController][reportWithdrawalCharge]  in errorResponse accountId : $accountId, lisaManager : $lisaManager")
                      handleFailure(lisaManager, accountId, req, errorResponse)
                  }
                } recover { case e: Exception =>
                  logger.error(
                    s"[WithdrawalController][reportWithdrawalCharge] An error occurred due to ${e.getMessage}, returning internal server error"
                  )
                  handleFailure(lisaManager, accountId, req, ReportWithdrawalChargeError)
                }
              }
            },
          lisaManager = lisaManager
        )
    }

  def getWithdrawalCharge(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateHeader(parse).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { () =>
          withValidAccountId(accountId) { () =>
            processGetWithdrawalCharge(lisaManager, accountId, transactionId)
          }
        }
      }

    }

  private def processGetWithdrawalCharge(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier,
    startTime: Long
  ) =
    getService.getBonusOrWithdrawal(lisaManager, accountId, transactionId).map {
      case response: GetWithdrawalResponse                 =>
        auditGetWithdrawalCharge(lisaManager, accountId, transactionId)
        lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.WITHDRAWAL_CHARGE)
        Ok(Json.toJson(response))
      case _: GetBonusResponse                             =>
        auditGetWithdrawalCharge(lisaManager, accountId, transactionId, Some(ErrorWithdrawalNotFound.errorCode))
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(ErrorWithdrawalNotFound.asJson)
      case GetBonusOrWithdrawalTransactionNotFoundResponse =>
        auditGetWithdrawalCharge(lisaManager, accountId, transactionId, Some(ErrorWithdrawalNotFound.errorCode))
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(ErrorWithdrawalNotFound.asJson)
      case GetBonusOrWithdrawalInvestorNotFoundResponse    =>
        auditGetWithdrawalCharge(lisaManager, accountId, transactionId, Some(ErrorAccountNotFound.errorCode))
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.WITHDRAWAL_CHARGE)
        NotFound(ErrorAccountNotFound.asJson)
      case GetBonusOrWithdrawalServiceUnavailableResponse  =>
        auditGetWithdrawalCharge(lisaManager, accountId, transactionId, Some(ErrorServiceUnavailable.errorCode))
        lisaMetrics.incrementMetrics(startTime, SERVICE_UNAVAILABLE, LisaMetricKeys.WITHDRAWAL_CHARGE)
        ServiceUnavailable(ErrorServiceUnavailable.asJson)
      case _                                               =>
        auditGetWithdrawalCharge(lisaManager, accountId, transactionId, Some(ErrorInternalServerError.errorCode))
        lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.WITHDRAWAL_CHARGE)
        InternalServerError(ErrorInternalServerError.asJson)
    }

  private def auditGetWithdrawalCharge(
    lisaManager: String,
    accountId: String,
    transactionId: String,
    failureReason: Option[String] = None
  )(implicit hc: HeaderCarrier) = {
    val path      = getWithdrawalChargeEndpointUrl(lisaManager, accountId, transactionId)
    val auditData = Map(
      ZREF            -> lisaManager,
      "accountId"     -> accountId,
      "transactionId" -> transactionId
    )

    failureReason map { reason =>
      auditService.audit(
        auditType = "getWithdrawalChargeNotReported",
        path = path,
        auditData = auditData ++ Map("reasonNotReported" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "getWithdrawalChargeReported",
      path = path,
      auditData = auditData
    )
  }

  private def withValidClaimPeriod(data: ReportWithdrawalChargeRequest)(lisaManager: String, accountId: String)(
    callback: () => Future[Result]
  )(implicit hc: HeaderCarrier, startTime: Long) = {

    //the deadline for making a monthly bonus claim to HMRC is the 20th day of the month following the end of the claim period - therefore 14 days
    //LISA Bonus claims can be made to HMRC or corrected by an ISA manager within 6 years after the end of the original bonus claim period - therefore 6 years
    val lastClaimDate       = dateTimeService.now().minusYears(6).minusDays(14)
    val claimCanStillBeMade = data.claimPeriodEndDate.isAfter(lastClaimDate.minusDays(1))
    if (claimCanStillBeMade) {
      callback()
    } else {
      auditReportWithdrawalChargeFailure(lisaManager, accountId, data, ErrorWithdrawalTimescalesExceeded.errorCode)

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

      Future.successful(Forbidden(ErrorWithdrawalTimescalesExceeded.asJson))
    }
  }

  private def withValidData(data: ReportWithdrawalChargeRequest)(lisaManager: String, accountId: String)(
    callback: () => Future[Result]
  )(implicit hc: HeaderCarrier, startTime: Long) = {
    val errors = validator.validate(data)
    if (errors.isEmpty) {
      callback()
    } else {
      auditReportWithdrawalChargeFailure(lisaManager, accountId, data, "FORBIDDEN")

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.WITHDRAWAL_CHARGE)

      Future.successful(Forbidden(ErrorForbidden(errors.toList).asJson))
    }
  }

  private def handleSuccess(
    lisaManager: String,
    accountId: String,
    req: ReportWithdrawalChargeRequest,
    resp: ReportWithdrawalChargeSuccessResponse
  )(implicit hc: HeaderCarrier, startTime: Long) = {
    logger.info("Matched success response")

    val (responseData, notification) = resp match {
      case _: ReportWithdrawalChargeOnTimeResponse     =>
        val data = ApiResponseData(
          message = "Unauthorised withdrawal transaction created",
          transactionId = Some(resp.transactionId)
        )
        (data, Some("no"))
      case _: ReportWithdrawalChargeLateResponse       =>
        val data = ApiResponseData(
          message = "Unauthorised withdrawal transaction created - late notification",
          transactionId = Some(resp.transactionId)
        )
        (data, Some("yes"))
      case _: ReportWithdrawalChargeSupersededResponse =>
        val data = ApiResponseData(
          message = "Unauthorised withdrawal transaction superseded",
          transactionId = Some(resp.transactionId)
        )
        (data, None)
    }

    notification map { notification =>
      auditService.audit(
        auditType = "withdrawalChargeRequested",
        path = reportWithdrawalChargeEndpointUrl(lisaManager, accountId),
        auditData = createReportWithdrawalChargeAuditData(lisaManager, accountId, req) + (NOTIFICATION -> notification)
      )
    } getOrElse auditService.audit(
      auditType = "withdrawalChargeRequested",
      path = reportWithdrawalChargeEndpointUrl(lisaManager, accountId),
      auditData = createReportWithdrawalChargeAuditData(lisaManager, accountId, req)
    )

    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.WITHDRAWAL_CHARGE)

    Created(Json.toJson(ApiResponse(data = Some(responseData), success = true, status = CREATED)))
  }

  private def handleFailure(
    lisaManager: String,
    accountId: String,
    req: ReportWithdrawalChargeRequest,
    errorResponse: ReportWithdrawalChargeErrorResponse
  )(implicit hc: HeaderCarrier, startTime: Long) = {
    val error =
      errorOutcomes.applyOrElse(errorResponse, { _: ReportWithdrawalChargeErrorResponse => ErrorInternalServerError })

    auditReportWithdrawalChargeFailure(lisaManager, accountId, req, error.errorCode)
    lisaMetrics.incrementMetrics(startTime, error.httpStatusCode, LisaMetricKeys.WITHDRAWAL_CHARGE)

    error.asResult
  }

  val errorOutcomes: PartialFunction[ReportWithdrawalChargeErrorResponse, ErrorResponse] = {
    case ReportWithdrawalChargeServiceUnavailable               => ErrorServiceUnavailable
    case ReportWithdrawalChargeAlreadyExists(transactionId)     => ErrorWithdrawalExists(transactionId)
    case ReportWithdrawalChargeAccountNotFound                  => ErrorAccountNotFound
    case ReportWithdrawalChargeSupersedeOutcomeError            => ErrorWithdrawalSupersededOutcomeError
    case ReportWithdrawalChargeSupersedeAmountMismatch          => ErrorWithdrawalSupersededAmountMismatch
    case ReportWithdrawalChargeAlreadySuperseded(transactionId) => ErrorWithdrawalAlreadySuperseded(transactionId)
    case ReportWithdrawalChargeReportingError                   => ErrorWithdrawalReportingError
    case ReportWithdrawalChargeAccountVoid                      => ErrorAccountAlreadyVoided
    case ReportWithdrawalChargeAccountCancelled                 => ErrorAccountAlreadyCancelled
  }

  private def auditReportWithdrawalChargeFailure(
    lisaManager: String,
    accountId: String,
    req: ReportWithdrawalChargeRequest,
    failureReason: String
  )(implicit hc: HeaderCarrier) =
    auditService.audit(
      auditType = "withdrawalChargeNotRequested",
      path = reportWithdrawalChargeEndpointUrl(lisaManager, accountId),
      auditData =
        createReportWithdrawalChargeAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> failureReason)
    )

  private def createReportWithdrawalChargeAuditData(
    lisaManager: String,
    accountId: String,
    req: ReportWithdrawalChargeRequest
  ): Map[String, String] =
    req.toStringMap ++ Map(ZREF -> lisaManager, "accountId" -> accountId)

  private def reportWithdrawalChargeEndpointUrl(lisaManager: String, accountId: String): String =
    s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"

  private def getWithdrawalChargeEndpointUrl(lisaManager: String, accountId: String, transactionId: String): String =
    s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"
}
