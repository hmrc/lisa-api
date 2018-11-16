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

import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusOrWithdrawalService, BonusPaymentService, CurrentDateService}
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class BonusPaymentController extends LisaController with LisaConstants {

  val postService: BonusPaymentService = BonusPaymentService
  val getService: BonusOrWithdrawalService = BonusOrWithdrawalService
  val auditService: AuditService = AuditService
  val validator: BonusPaymentValidator = BonusPaymentValidator
  val dateTimeService: CurrentDateService = CurrentDateService

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = {
    implicit val startTime: Long = System.currentTimeMillis()
    validateHeader().async { implicit request =>

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withApiVersion {
            case Some(VERSION_1) => processRequestBonusPayment(lisaManager, accountId, RequestBonusPaymentRequest.requestBonusPaymentReadsV1)
            case Some(VERSION_2) => processRequestBonusPayment(lisaManager, accountId, RequestBonusPaymentRequest.requestBonusPaymentReadsV2)
          }
        }
      }
    }
  }

  def getBonusPayment(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateHeader().async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            processGetBonusPayment(lisaManager, accountId, transactionId)
          }
        }
      }
    }

  private def processRequestBonusPayment(lisaManager: String, accountId: String, reads: Reads[RequestBonusPaymentRequest])
                                        (implicit request: Request[AnyContent], startTime: Long, hc: HeaderCarrier) = {
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
    )(request, reads, startTime)
  }

  private def processGetBonusPayment(lisaManager: String, accountId: String, transactionId: String)
                                    (implicit hc: HeaderCarrier, startTime: Long, request: Request[AnyContent]): Future[Result] = {
    getService.getBonusOrWithdrawal(lisaManager, accountId, transactionId).flatMap {
      case response: GetBonusResponse =>
        LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.BONUS_PAYMENT)
        withApiVersion {
          case Some(VERSION_1) => {
            if (response.bonuses.claimReason == "Superseded Bonus") {
              Logger.warn(s"API v1 received a superseded bonus claim. ID was $transactionId")
              Future.successful(InternalServerError(Json.toJson(ErrorInternalServerError)))
            }
            else {
              Future.successful(Ok(Json.toJson(response.copy(supersededBy = None))))
            }
          }
          case Some(VERSION_2) => Future.successful(Ok(Json.toJson(response)))
        }

      case _: GetWithdrawalResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(NotFound(Json.toJson(ErrorTransactionNotFound)))

      case GetBonusOrWithdrawalTransactionNotFoundResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(NotFound(Json.toJson(ErrorTransactionNotFound)))

      case GetBonusOrWithdrawalInvestorNotFoundResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(NotFound(Json.toJson(ErrorAccountNotFound)))

      case _ =>
        LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.BONUS_PAYMENT)
        Future.successful(InternalServerError(Json.toJson(ErrorInternalServerError)))
    }
  }

  private def withValidData(data: RequestBonusPaymentRequest)
                           (lisaManager: String, accountId: String)
                           (callback: () => Future[Result])
                           (implicit hc: HeaderCarrier, startTime: Long) = {

    (data.bonuses.claimReason, data.lifeEventId) match {
      case ("Life Event", None) =>
        handleLifeEventNotProvided(lisaManager, accountId, data)
      case _ => {
        val errors = validator.validate(data)

        if (errors.isEmpty) {
          withValidClaimPeriod(data)(lisaManager, accountId) { () =>
            withValidHtb(data)(lisaManager, accountId) { () =>
              callback()
            }
          }
        }
        else {
          auditFailure(lisaManager, accountId, data, "FORBIDDEN")

          LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

          Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
        }
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
    }
    else {
      auditFailure(lisaManager, accountId, data, ErrorBonusClaimTimescaleExceeded.errorCode)

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

      Future.successful(Forbidden(Json.toJson(ErrorBonusClaimTimescaleExceeded)))
    }
  }

  private def withValidHtb(data: RequestBonusPaymentRequest)
                          (lisaManager: String, accountId: String)
                          (callback: () => Future[Result])
                          (implicit hc: HeaderCarrier, startTime: Long) = {

    data.htbTransfer match {
      case None => callback()
      case Some(htb) => {
        val htbFiguresSubmitted = htb.htbTransferInForPeriod > 0 || htb.htbTransferTotalYTD > 0
        val lastValidHtbStartDate = new DateTime("2018-03-06")

        if (htbFiguresSubmitted && data.periodStartDate.isAfter(lastValidHtbStartDate)) {
          auditFailure(lisaManager, accountId, data, ErrorBonusHelpToBuyNotApplicable.errorCode)

          LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

          Future.successful(Forbidden(Json.toJson(ErrorBonusHelpToBuyNotApplicable)))
        }
        else {
          callback()
        }
      }
    }
  }

  private def handleSuccess(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, resp: RequestBonusPaymentSuccessResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Matched success response")

    val responseData = resp match {
      case _: RequestBonusPaymentOnTimeResponse =>
        val data = ApiResponseData(message = "Bonus transaction created", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "bonusPaymentRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "no")
        )

        data
      case _: RequestBonusPaymentLateResponse =>
        val data = ApiResponseData(message = "Bonus transaction created - late notification", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "bonusPaymentRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "yes")
        )

        data
      case _: RequestBonusPaymentSupersededResponse =>
        val data = ApiResponseData(message = "Bonus transaction superseded", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "bonusPaymentRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req)
        )

        data
    }

    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.BONUS_PAYMENT)

    Created(Json.toJson(ApiResponse(data = Some(responseData), success = true, status = CREATED)))
  }

  private def handleFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, errorResponse: RequestBonusPaymentErrorResponse)
                           (implicit hc: HeaderCarrier, request: Request[AnyContent], startTime: Long) = {
    Logger.debug("Matched failure response")

    val response: ErrorResponse = errorResponse match {
      case e: RequestBonusPaymentClaimAlreadyExists =>
        getAPIVersionFromRequest(request) match {
          case Some(VERSION_1) => ErrorInternalServerError
          case Some(VERSION_2) => ErrorBonusClaimAlreadyExists(e.transactionId)
        }
      case e: RequestBonusPaymentAlreadySuperseded =>
        getAPIVersionFromRequest(request) match {
          case Some(VERSION_1) => ErrorInternalServerError
          case Some(VERSION_2) => ErrorBonusClaimAlreadySuperseded(e.transactionId)
        }
      case _ =>
        getAPIVersionFromRequest(request) match {
          case Some(VERSION_1) => requestBonusErrorsV1.getOrElse(errorResponse, ErrorInternalServerError)
          case Some(VERSION_2) => requestBonusErrorsV2.getOrElse(errorResponse, ErrorInternalServerError)
        }
    }
    auditFailure(lisaManager, accountId, req, response.errorCode)
    LisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.BONUS_PAYMENT)
    response.asResult
  }

  private val requestBonusErrors = Map[RequestBonusPaymentErrorResponse, ErrorResponse](
    RequestBonusPaymentBonusClaimError -> ErrorBonusClaimError,
    RequestBonusPaymentNoSubscriptions -> ErrorNoSubscriptions,
    RequestBonusPaymentAccountNotFound -> ErrorAccountNotFound,
    RequestBonusPaymentLifeEventNotFound -> ErrorLifeEventIdNotFound
  )

  private val requestBonusErrorsV1 = requestBonusErrors ++ Map[RequestBonusPaymentErrorResponse, ErrorResponse](
    RequestBonusPaymentAccountClosedOrVoid -> ErrorAccountAlreadyClosedOrVoid
  )

  private val requestBonusErrorsV2 = requestBonusErrors ++ Map[RequestBonusPaymentErrorResponse, ErrorResponse](
    RequestBonusPaymentAccountClosed -> ErrorAccountAlreadyClosed,
    RequestBonusPaymentAccountCancelled -> ErrorAccountAlreadyCancelled,
    RequestBonusPaymentAccountVoid -> ErrorAccountAlreadyVoided,
    RequestBonusPaymentSupersededAmountMismatch -> ErrorBonusSupersededAmountMismatch,
    RequestBonusPaymentSupersededOutcomeError -> ErrorBonusSupersededOutcomeError
  )

  private def handleError(e: Exception, lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.error(s"requestBonusPayment: An error occurred due to ${e.getMessage} returning internal server error")

    auditFailure(lisaManager, accountId, req, ErrorInternalServerError.errorCode)
    LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.BONUS_PAYMENT)

    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def handleLifeEventNotProvided(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)
                                        (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Life event not provided")

    auditFailure(lisaManager, accountId, req, ErrorLifeEventNotProvided.errorCode)
    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

    Future.successful(Forbidden(Json.toJson(ErrorLifeEventNotProvided)))
  }

  private def auditFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, failureReason: String)
                          (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
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

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/transactions"
  }
}
