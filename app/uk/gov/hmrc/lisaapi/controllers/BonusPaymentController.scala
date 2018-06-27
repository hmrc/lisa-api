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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusPaymentService, CurrentDateService}
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BonusPaymentController extends LisaController with LisaConstants {

  val service: BonusPaymentService = BonusPaymentService
  val auditService: AuditService = AuditService
  val validator: BonusPaymentValidator = BonusPaymentValidator
  val dateTimeService: CurrentDateService = CurrentDateService

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestBonusPaymentRequest](req =>
            (req.bonuses.claimReason, req.lifeEventId) match {
              case ("Life Event", None) =>
                handleLifeEventNotProvided(lisaManager, accountId, req)
              case _ =>
                withValidData(req)(lisaManager, accountId) { () =>
                  withValidClaimPeriod(req)(lisaManager, accountId) { () =>
                    withValidHtb(req)(lisaManager, accountId) { () =>
                      service.requestBonusPayment(lisaManager, accountId, req) map { res =>
                        Logger.debug("Entering Bonus Payment Controller and the response is " + res.toString)

                        res match {
                          case successResponse: RequestBonusPaymentSuccessResponse =>
                            handleSuccess(lisaManager, accountId, req, successResponse)
                          case errorResponse: RequestBonusPaymentErrorResponse =>
                            handleFailure(lisaManager, accountId, req, errorResponse)
                        }
                      } recover {
                        case e: Exception =>
                          Logger.error(s"requestBonusPayment: An error occurred due to ${e.getMessage} returning internal server error")
                          handleError(lisaManager, accountId, req)
                      }
                    }
                  }
                }
            }, lisaManager = lisaManager
          )
        }
      }
  }

  def getBonusPayment(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            processGetBonusPayment(lisaManager, accountId, transactionId)
          }
        }
      }
    }

  private def processGetBonusPayment(lisaManager:String, accountId:String, transactionId: String)
                                    (implicit hc: HeaderCarrier, startTime: Long) = {
    service.getBonusPayment(lisaManager, accountId, transactionId).map {
      case response: GetBonusPaymentSuccessResponse =>
        LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.BONUS_PAYMENT)
        Ok(Json.toJson(response))

      case GetBonusPaymentLmrnDoesNotExistResponse =>
        LisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.BONUS_PAYMENT)
        BadRequest(Json.toJson(ErrorBadRequestLmrn))

      case GetBonusPaymentTransactionNotFoundResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        NotFound(Json.toJson(ErrorTransactionNotFound))

      case GetBonusPaymentInvestorNotFoundResponse =>
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)
        NotFound(Json.toJson(ErrorAccountNotFound))

      case _ =>
        LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.BONUS_PAYMENT)
        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  private def withValidData(data: RequestBonusPaymentRequest)
                           (lisaManager: String, accountId: String)
                           (callback: () => Future[Result])
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    val errors = validator.validate(data)

    if (errors.isEmpty) {
      callback()
    }
    else {
      auditFailure(lisaManager, accountId, data, "FORBIDDEN")

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(errors.toList))))
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
      case _:RequestBonusPaymentOnTimeResponse =>
        val data = ApiResponseData(message = "Bonus transaction created", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "bonusPaymentRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "no")
        )

        data
      case _:RequestBonusPaymentLateResponse =>
        val data = ApiResponseData(message = "Bonus transaction created - late notification", transactionId = Some(resp.transactionId))

        auditService.audit(
          auditType = "bonusPaymentRequested",
          path = getEndpointUrl(lisaManager, accountId),
          auditData = createAuditData(lisaManager, accountId, req) + (NOTIFICATION -> "yes")
        )

        data
      case _:RequestBonusPaymentSupersededResponse =>
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

  // scalastyle:off cyclomatic.complexity method.length
  private def handleFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, errorResponse: RequestBonusPaymentErrorResponse)
                           (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("Matched failure response")

    errorResponse match {
      case RequestBonusPaymentBonusClaimError =>
        auditFailure(lisaManager, accountId, req, ErrorBonusClaimError.errorCode)
        LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

        Forbidden(Json.toJson(ErrorBonusClaimError))
      case RequestBonusPaymentAccountClosed =>
        auditFailure(lisaManager, accountId, req, ErrorAccountAlreadyClosedOrVoid.errorCode)
        LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

        Forbidden(Json.toJson(ErrorAccountAlreadyClosedOrVoid))
      case RequestBonusPaymentSupersededAmountMismatch =>
        auditFailure(lisaManager, accountId, req, ErrorBonusSupersededAmountMismatch.errorCode)
        LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

        Forbidden(Json.toJson(ErrorBonusSupersededAmountMismatch))
      case RequestBonusPaymentSupersededOutcomeError =>
        auditFailure(lisaManager, accountId, req, ErrorBonusSupersededOutcomeError.errorCode)
        LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.BONUS_PAYMENT)

        Forbidden(Json.toJson(ErrorBonusSupersededOutcomeError))
      case RequestBonusPaymentAccountNotFound =>
        auditFailure(lisaManager, accountId, req, ErrorAccountNotFound.errorCode)
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)

        NotFound(Json.toJson(ErrorAccountNotFound))
      case RequestBonusPaymentLifeEventNotFound =>
        auditFailure(lisaManager, accountId, req, ErrorLifeEventIdNotFound.errorCode)
        LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.BONUS_PAYMENT)

        NotFound(Json.toJson(ErrorLifeEventIdNotFound))
      case RequestBonusPaymentClaimAlreadyExists =>
        auditFailure(lisaManager, accountId, req, ErrorBonusClaimAlreadyExists.errorCode)
        LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.BONUS_PAYMENT)

        Conflict(Json.toJson(ErrorBonusClaimAlreadyExists))
      case RequestBonusPaymentAlreadySuperseded =>
        auditFailure(lisaManager, accountId, req, ErrorBonusClaimAlreadySuperseded.errorCode)
        LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.BONUS_PAYMENT)

        Conflict(Json.toJson(ErrorBonusClaimAlreadySuperseded))
      case _ =>
        auditFailure(lisaManager, accountId, req, ErrorInternalServerError.errorCode)
        LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.BONUS_PAYMENT)

        InternalServerError(Json.toJson(ErrorInternalServerError))
    }
  }

  private def handleError(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier, startTime: Long) = {
    Logger.debug("An error occurred")

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
    req.toStringMap ++ Map(ZREF -> lisaManager,
      "accountId" -> accountId)
  }

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/transactions"
  }
}
