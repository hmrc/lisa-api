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
import play.api.libs.json.Reads.of
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetBulkPaymentNotFoundResponse, GetBulkPaymentServiceUnavailableResponse, GetBulkPaymentSuccessResponse}
import uk.gov.hmrc.lisaapi.services.{AuditService, BulkPaymentService, CurrentDateService}

import java.time.LocalDate
import java.time.format.{DateTimeFormatter, DateTimeParseException}
import scala.collection.immutable
import scala.concurrent.{ExecutionContext, Future}

class BulkPaymentController @Inject()(
                                       authConnector: AuthConnector,
                                       appContext: AppContext,
                                       currentDateService: CurrentDateService,
                                       service: BulkPaymentService,
                                       auditService: AuditService,
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

  def getBulkPayment(lisaManager: String, startDate: String, endDate: String): Action[AnyContent] =
    validateHeader(parse).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      logger.info(s"""[BulkPaymentController][getBulkPayment] lisaManager : $lisaManager""")

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { _ =>
          withValidDates(startDate, endDate, lisaManager) { (start, end) =>
            val response = service.getBulkPayment(lisaManager, start, end)

            response flatMap {
              case s: GetBulkPaymentSuccessResponse =>
                logger.info(s"""[BulkPaymentController][getBulkPayment] GetBulkPaymentSuccessResponse lisaManager : $lisaManager""")
                getBulkPaymentAudit(lisaManager)
                lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.TRANSACTION)
                withApiVersion {
                  case Some(VERSION_1) => Future.successful(transformV1Response(Json.toJson(s)))
                  case Some(VERSION_2) => Future.successful(Ok(Json.toJson(s)))
                }
              case GetBulkPaymentNotFoundResponse =>
                logger.info(s"""[BulkPaymentController][getBulkPayment] GetBulkPaymentNotFoundResponse lisaManager : $lisaManager""")
                lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.TRANSACTION)
                withApiVersion {
                  case Some(VERSION_1) =>
                    getBulkPaymentAudit(lisaManager, Some(ErrorBulkTransactionNotFoundV1.errorCode))
                    Future.successful(NotFound(ErrorBulkTransactionNotFoundV1.asJson))
                  case Some(VERSION_2) =>
                    getBulkPaymentAudit(lisaManager, Some(ErrorBulkTransactionNotFoundV2.errorCode))
                    Future.successful(NotFound(ErrorBulkTransactionNotFoundV2.asJson))
                }
              case GetBulkPaymentServiceUnavailableResponse =>
                logger.error(s"[BulkPaymentController][getBulkPayment] GetBulkPaymentServiceUnavailableResponse lisaManager : $lisaManager")
                getBulkPaymentAudit(lisaManager, Some(ErrorServiceUnavailable.errorCode))
                Future.successful(ErrorServiceUnavailable.asResult)
              case _ =>
                logger.info(s"[BulkPaymentController][getBulkPayment] other cases lisaManager : $lisaManager")
                getBulkPaymentAudit(lisaManager, Some(ErrorInternalServerError.errorCode))
                lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.TRANSACTION)
                Future.successful(InternalServerError(ErrorInternalServerError.asJson))
            }
          }
        }
      }
    }

  def transformV1Response(json: JsValue): Result = {
    val jsonTransformer = (__ \ Symbol("payments")).json.update(
      of[JsArray] map { case JsArray(arr) =>
        JsArray(arr map { case JsObject(o) =>
          JsObject(o.to(immutable.Map) -- Set("transactionType", "status"))
        })
      }
    )

    json
      .transform(jsonTransformer)
      .fold(
        _ => InternalServerError(ErrorInternalServerError.asJson),
        success => Ok(Json.toJson(success))
      )
  }

  private def withValidDates(startDate: String, endDate: String, lisaManager: String)(
    success: (LocalDate, LocalDate) => Future[Result]
  )(implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {

    val start = parseDate(startDate)
    val end = parseDate(endDate)

    (start, end) match {
      case (Some(s), Some(e)) =>
        withDatesWithinBusinessRules(s, e, lisaManager) { () =>
          success(s, e)
        }
      case (None, Some(_)) =>
        getBulkPaymentAudit(lisaManager, Some(ErrorBadRequestStart.errorCode))
        lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.TRANSACTION)
        Future.successful(BadRequest(ErrorBadRequestStart.asJson))
      case (Some(_), None) =>
        getBulkPaymentAudit(lisaManager, Some(ErrorBadRequestEnd.errorCode))
        lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.TRANSACTION)
        Future.successful(BadRequest(ErrorBadRequestEnd.asJson))
      case _ =>
        getBulkPaymentAudit(lisaManager, Some(ErrorBadRequestStartEnd.errorCode))
        lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.TRANSACTION)
        Future.successful(BadRequest(ErrorBadRequestStartEnd.asJson))
    }
  }

  private def withDatesWithinBusinessRules(startDate: LocalDate, endDate: LocalDate, lisaManager: String)(
    success: () => Future[Result]
  )(implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {
    val errorResponse: Option[ErrorResponse] = if (endDate.isAfter(currentDateService.now())) {
      Some(ErrorBadRequestEndInFuture)
    } else if (endDate.isBefore(startDate)) {
      Some(ErrorBadRequestEndBeforeStart)
    } else if (startDate.isBefore(LISA_START_DATE)) {
      Some(ErrorBadRequestStartBefore6April2017)
    } else if (endDate.isAfter(startDate.plusYears(1))) {
      Some(ErrorBadRequestOverYearBetweenStartAndEnd)
    } else {
      None
    }

    errorResponse
      .map { error =>
        getBulkPaymentAudit(lisaManager, Some(error.errorCode))
        lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.TRANSACTION)
        Future.successful(Forbidden(error.asJson))
      }
      .getOrElse(success())
  }

  private def getBulkPaymentAudit(lisaManager: String, failureReason: Option[String] = None)(implicit
                                                                                             hc: HeaderCarrier
  ) = {
    val path = getBulkPaymentEndpointUrl(lisaManager)
    val auditData = Map(ZREF -> lisaManager)

    failureReason map { reason =>
      auditService.audit(
        auditType = "getBulkPaymentNotReported",
        path = path,
        auditData = auditData ++ Map("reasonNotReported" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "getBulkPaymentReported",
      path = path,
      auditData = auditData
    )
  }

  private def parseDate(input: String): Option[LocalDate] = {
    val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    if (input.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
      try {
        Some(LocalDate.parse(input, dateFormat))
      } catch {
        case _: DateTimeParseException => None
      }
    } else {
      None
    }
  }

  private def getBulkPaymentEndpointUrl(lisaManagerReferenceNumber: String): String =
    s"/manager/$lisaManagerReferenceNumber/payments"

}
