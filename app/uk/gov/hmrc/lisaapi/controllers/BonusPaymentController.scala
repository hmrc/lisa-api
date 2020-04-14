/*
 * Copyright 2020 HM Revenue & Customs
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
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusOrWithdrawalService, BonusPaymentService, CurrentDateService}
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.{ExecutionContext, Future}

class BonusPaymentController @Inject()(
                                        authConnector: AuthConnector,
                                        appContext: AppContext,
                                        getService: BonusOrWithdrawalService,
                                        postService: BonusPaymentService,
                                        auditService: AuditService,
                                        validator: BonusPaymentValidator,
                                        dateTimeService: CurrentDateService,
                                        lisaMetrics: LisaMetrics,
                                        cc: ControllerComponents,
                                        parse: PlayBodyParsers
                                      )(implicit ec: ExecutionContext) extends LisaController(
  cc: ControllerComponents,
  lisaMetrics: LisaMetrics,
  appContext: AppContext,
  authConnector: AuthConnector
) {

  private val requestBonusErrors = Map[RequestBonusPaymentErrorResponse, ErrorResponse](
    RequestBonusPaymentBonusClaimError -> ErrorBonusClaimError,
    RequestBonusPaymentNoSubscriptions -> ErrorNoSubscriptions,
    RequestBonusPaymentAccountNotFound -> ErrorAccountNotFound,
    RequestBonusPaymentLifeEventNotFound -> ErrorLifeEventIdNotFound
  )

  private val requestBonusErrorsV2 = requestBonusErrors ++ Map[RequestBonusPaymentErrorResponse, ErrorResponse](
    RequestBonusPaymentAccountClosed -> ErrorAccountAlreadyClosed,
    RequestBonusPaymentAccountCancelled -> ErrorAccountAlreadyCancelled,
    RequestBonusPaymentAccountVoid -> ErrorAccountAlreadyVoided,
    RequestBonusPaymentSupersededAmountMismatch -> ErrorBonusSupersededAmountMismatch,
    RequestBonusPaymentSupersededOutcomeError -> ErrorBonusSupersededOutcomeError
  )

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = {
    implicit val startTime: Long = System.currentTimeMillis()
    validateHeader(parse).async { implicit request =>

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withApiVersion {
            case Some(VERSION_2) => processRequestBonusPayment(lisaManager, accountId, RequestBonusPaymentRequest.requestBonusPaymentReadsV2)
          }
        }
      }
    }
  }

  def getBonusPayment(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateHeader(parse).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { _ =>
          withValidAccountId(accountId) { () =>
            withValidTransactionId(transactionId) { () =>
              processGetBonusPayment(lisaManager, accountId, transactionId)
            }
          }
        }
      }
    }

  private def processGetBonusPayment(lisaManager: String, accountId: String, transactionId: String)
                                    (implicit hc: HeaderCarrier, startTime: Long, request: Request[AnyContent]): Future[Result] = {
    getService.getBonusOrWithdrawal(lisaManager, accountId, transactionId).flatMap {
      case response: GetBonusResponse =>
        lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.BONUS_PAYMENT)
        withApiVersion {
          case Some(VERSION_1) =>
            if (response.bonuses.claimReason == "Superseded Bonus") {
              getBonusPaymentAudit(lisaManager, accountId, transactionId, Some(ErrorInternalServerError.errorCode))
              Logger.warn(s"API v1 received a superseded bonus claim. ID was $transactionId")
              Future.successful(InternalServerError(Json.toJson(ErrorInternalServerError)))
            } else {
              getBonusPaymentAudit(lisaManager, accountId, transactionId)
              Future.successful(Ok(Json.toJson(response.copy(supersededBy = None))))
            }
          case Some(VERSION_2) =>
            getBonusPaymentAudit(lisaManager, accountId, transactionId)
            Future.successful(Ok(Json.toJson(response)))
        }

      case _: GetWithdrawalResponse | GetBonusOrWithdrawalTransactionNotFoundResponse =>
        getBonusPaymentAudit(lisaManager, accountId, transactionId, Some(ErrorBonusPaymentTransactionNotFound.errorCode))
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(NotFound(Json.toJson(ErrorBonusPaymentTransactionNotFound)))

      case GetBonusOrWithdrawalInvestorNotFoundResponse =>
        getBonusPaymentAudit(lisaManager, accountId, transactionId, Some(ErrorAccountNotFound.errorCode))
        lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(NotFound(Json.toJson(ErrorAccountNotFound)))

      case GetBonusOrWithdrawalServiceUnavailableResponse =>
        getBonusPaymentAudit(lisaManager, accountId, transactionId, Some(ErrorServiceUnavailable.errorCode))
        lisaMetrics.incrementMetrics(startTime, SERVICE_UNAVAILABLE, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(ServiceUnavailable(Json.toJson(ErrorServiceUnavailable)))

      case _ =>
        getBonusPaymentAudit(lisaManager, accountId, transactionId, Some(ErrorInternalServerError.errorCode))
        lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(InternalServerError(Json.toJson(ErrorInternalServerError)))
    }
  }

  private def getBonusPaymentAudit(lisaManager: String, accountId: String, transactionId: String, failureReason: Option[String] = None)
                                             (implicit hc: HeaderCarrier) = {
    val path = getBonusPaymentEndpointUrl(lisaManager, accountId, transactionId)
    val auditData = Map(
      ZREF -> lisaManager,
      "accountId" -> accountId,
      "transactionId" -> transactionId
    )

    failureReason map { reason =>
      auditService.audit(
        auditType = "getBonusPaymentNotReported",
        path = path,
        auditData = auditData ++ Map("reasonNotReported" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "getBonusPaymentReported",
      path = path,
      auditData = auditData
    )
  }

  private def processRequestBonusPayment(lisaManager: String, accountId: String, reads: Reads[RequestBonusPaymentRequest])
                                        (implicit request: Request[AnyContent], startTime: Long, hc: HeaderCarrier, ec: ExecutionContext) = {
    withValidJson[RequestBonusPaymentRequest](
      req => {
        withValidData(req)(lisaManager, accountId) { () =>
          postService.requestBonusPayment(lisaManager, accountId, req) map {
            case success: RequestBonusPaymentSuccessResponse => handleSuccess(lisaManager, accountId, req, success)
            case failure: RequestBonusPaymentErrorResponse => handleFailure(lisaManager, accountId, req, failure)
          } recover {
            case e: Exception => handleError(e, lisaManager, accountId, req)
          }
        }
      },
      lisaManager = lisaManager
    )(request, reads, startTime, ec)
  }

  private def withValidData(data: RequestBonusPaymentRequest)
                           (lisaManager: String, accountId: String)
                           (callback: () => Future[Result])
                           (implicit hc: HeaderCarrier, startTime: Long) = {

    (data.bonuses.claimReason, data.lifeEventId) match {
      case ("Life Event", None) =>
        handleLifeEventNotProvided(lisaManager, accountId, data)
      case _ =>
        val errors = validator.validate(data)

        if (errors.isEmpty) {
          withValidClaimPeriod(data)(lisaManager, accountId) { () =>
            withValidHtb(data)(lisaManager, accountId) { () =>
              callback()
            }
          }
        } else {
          requestBonusPaymentFailureAudit(lisaManager, accountId, data, "FORBIDDEN")

          lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

          Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
        }
    }
  }

  private def withValidClaimPeriod(data: RequestBonusPaymentRequest)
                                  (lisaManager: String, accountId: String)
                                  (callback: () => Future[Result])
                                  (implicit hc: HeaderCarrier, startTime: Long) = {

    val lastClaimDate = dateTimeService.now().withTime(0, 0, 0, 0).minusYears(6).minusDays(14)

    val claimCanStillBeMade = data.periodEndDate.isAfter(lastClaimDate.minusDays(1))

    if (claimCanStillBeMade) {
      callback()
    } else {
      requestBonusPaymentFailureAudit(lisaManager, accountId, data, ErrorBonusClaimTimescaleExceeded.errorCode)

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

      Future.successful(Forbidden(Json.toJson(ErrorBonusClaimTimescaleExceeded)))
    }
  }

  private def withValidHtb(data: RequestBonusPaymentRequest)
                          (lisaManager: String, accountId: String)
                          (callback: () => Future[Result])
                          (implicit hc: HeaderCarrier, startTime: Long) = {
    val lastValidHtbStartDate = new DateTime("2018-03-06")

    val htbResponse = for {
      htb <- data.htbTransfer
      htbFiguresSubmitted <- Some(htb.htbTransferInForPeriod > 0 || htb.htbTransferTotalYTD > 0)
      error <- Option(htbFiguresSubmitted && data.periodStartDate.isAfter(lastValidHtbStartDate))
        .collect { case true => ErrorBonusHelpToBuyNotApplicable }
    } yield {
      requestBonusPaymentFailureAudit(lisaManager, accountId, data, error.errorCode)
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)
      Future.successful(Forbidden(Json.toJson(error)))
    }

    htbResponse.getOrElse(callback())
  }

  private def handleSuccess(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, resp: RequestBonusPaymentSuccessResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Matched success response")

    val (responseData, notification) = resp match {
      case _: RequestBonusPaymentOnTimeResponse =>
        val data = ApiResponseData(message = "Bonus transaction created", transactionId = Some(resp.transactionId))
        (data, Some("no"))
      case _: RequestBonusPaymentLateResponse =>
        val data = ApiResponseData(message = "Bonus transaction created - late notification", transactionId = Some(resp.transactionId))
        (data, Some("yes"))
      case _: RequestBonusPaymentSupersededResponse =>
        val data = ApiResponseData(message = "Bonus transaction superseded", transactionId = Some(resp.transactionId))
        (data, None)
    }

    val auditData = notification map { notification =>
      createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> notification)
    } getOrElse createAuditData(lisaManager, accountId, req)

    auditService.audit(
      auditType = "bonusPaymentRequested",
      path = requestBonusPaymentEndpointUrl(lisaManager, accountId),
      auditData = auditData
    )

    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.BONUS_PAYMENT)

    Created(Json.toJson(ApiResponse(data = Some(responseData), success = true, status = CREATED)))
  }

  private def handleFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, errorResponse: RequestBonusPaymentErrorResponse)
                           (implicit hc: HeaderCarrier, request: Request[AnyContent], startTime: Long) = {
    Logger.debug("Matched failure response")

    val response: ErrorResponse = (errorResponse, getAPIVersionFromRequest(request)) match {
      case(e: RequestBonusPaymentClaimAlreadyExists, Some(VERSION_2)) => ErrorBonusClaimAlreadyExists(e.transactionId)
      case(e: RequestBonusPaymentAlreadySuperseded, Some(VERSION_2)) => ErrorBonusClaimAlreadySuperseded(e.transactionId)
      case (RequestBonusPaymentServiceUnavailable, _) => ErrorServiceUnavailable
      case (_, Some(VERSION_2)) => requestBonusErrorsV2.getOrElse(errorResponse, ErrorInternalServerError)
    }
    requestBonusPaymentFailureAudit(lisaManager, accountId, req, response.errorCode)
    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.BONUS_PAYMENT)
    response.asResult
  }

  private def handleError(e: Exception, lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.error(s"requestBonusPayment: An error occurred due to ${e.getMessage} returning internal server error")

    requestBonusPaymentFailureAudit(lisaManager, accountId, req, ErrorInternalServerError.errorCode)
    lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.BONUS_PAYMENT)

    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def handleLifeEventNotProvided(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)
                                        (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Life event not provided")

    requestBonusPaymentFailureAudit(lisaManager, accountId, req, ErrorLifeEventNotProvided.errorCode)
    lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

    Future.successful(Forbidden(Json.toJson(ErrorLifeEventNotProvided)))
  }

  private def requestBonusPaymentFailureAudit(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, failureReason: String)
                          (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = requestBonusPaymentEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> failureReason)
    )
  }

  private def createAuditData(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest): Map[String, String] = {
    val result = req.toStringMap ++ Map(ZREF -> lisaManager, "accountId" -> accountId)

    req.supersede.fold(result) {
      case _: AdditionalBonus => result ++ Map("reason" -> "Additional bonus")
      case _: BonusRecovery => result ++ Map("reason" -> "Bonus recovery")
    }
  }

  private def requestBonusPaymentEndpointUrl(lisaManager: String, accountId: String): String =
    s"/manager/$lisaManager/accounts/$accountId/transactions"

  private def getBonusPaymentEndpointUrl(lisaManager: String, accountId: String, transactionId: String): String =
    s"/manager/$lisaManager/accounts/$accountId/transactions/$transactionId"
}
