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
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Reads.of
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetBulkPaymentNotFoundResponse, GetBulkPaymentServiceUnavailableResponse, GetBulkPaymentSuccessResponse}
import uk.gov.hmrc.lisaapi.services.{AuditService, BulkPaymentService, CurrentDateService}

import scala.concurrent.{ExecutionContext, Future}

class BulkPaymentController @Inject()(
                                       val authConnector: AuthConnector,
                                       val appContext: AppContext,
                                       currentDateService: CurrentDateService,
                                       service: BulkPaymentService,
                                       auditService: AuditService,
                                       val lisaMetrics: LisaMetrics
                                     )(implicit ec: ExecutionContext) extends LisaController {

  def getBulkPayment(lisaManager: String, startDate: String, endDate: String): Action[AnyContent] =
    validateHeader().async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { _ =>
          withValidDates(startDate, endDate) { (start, end) =>
            val response = service.getBulkPayment(lisaManager, start, end)

            response flatMap {
              case s: GetBulkPaymentSuccessResponse => {
                auditService.audit(
                  auditType = "getBulkPaymentReported",
                  path = getEndpointUrl(lisaManager),
                  auditData = Map(
                    "lisaManagerReferenceNumber" -> lisaManager
                  ))
                lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.TRANSACTION)
                withApiVersion {
                  case Some(VERSION_1) => Future.successful(transformV1Response(Json.toJson(s)))
                  case Some(VERSION_2) => Future.successful(Ok(Json.toJson(s)))
                }
              }
              case GetBulkPaymentNotFoundResponse => {
                lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.TRANSACTION)
                withApiVersion {
                  case Some(VERSION_1) => Future.successful(NotFound(Json.toJson(ErrorBulkTransactionNotFoundV1)))
                  case Some(VERSION_2) => Future.successful(NotFound(Json.toJson(ErrorBulkTransactionNotFoundV2)))
                }
              }
              case GetBulkPaymentServiceUnavailableResponse => Future.successful(ErrorServiceUnavailable.asResult)
              case _ => {
                lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.TRANSACTION)
                Future.successful(InternalServerError(Json.toJson(ErrorInternalServerError)))
              }
            }
          }
        }
      }
    }

  def transformV1Response(json: JsValue): Result = {
    val jsonTransformer = (__ \ 'payments).json.update(
      of[JsArray] map {
        case JsArray(arr) => JsArray(arr map {
          case JsObject(o) => JsObject(o - "transactionType" - "status")
        })
      }
    )

    json.transform(jsonTransformer).fold(
      _ => InternalServerError(Json.toJson(ErrorInternalServerError)),
      success => Ok(Json.toJson(success))
    )
  }

  private def withValidDates(startDate: String, endDate: String)
                            (success: (DateTime, DateTime) => Future[Result])
                            (implicit startTime: Long): Future[Result] = {

    val start = parseDate(startDate)
    val end = parseDate(endDate)

    (start, end) match {
      case (Some(s), Some(e)) => {
        withDatesWithinBusinessRules(s, e) { () =>
          success(s, e)
        }
      }
      case (None, Some(_)) =>
        lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.TRANSACTION)
        Future.successful(BadRequest(Json.toJson(ErrorBadRequestStart)))
      case (Some(_), None) =>
        lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.TRANSACTION)
        Future.successful(BadRequest(Json.toJson(ErrorBadRequestEnd)))
      case _ =>
        lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.TRANSACTION)
        Future.successful(BadRequest(Json.toJson(ErrorBadRequestStartEnd)))
    }
  }

  private def withDatesWithinBusinessRules(startDate: DateTime, endDate: DateTime)
                                          (success: () => Future[Result])
                                          (implicit startTime: Long): Future[Result] = {

    // end date is in the future
    if (endDate.isAfter(currentDateService.now())) {
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.TRANSACTION)
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestEndInFuture)))
    }

    // end date is before start date
    else if (endDate.isBefore(startDate)) {
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.TRANSACTION)
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestEndBeforeStart)))
    }

    // start date is before 6 april 2017
    else if (startDate.isBefore(LISA_START_DATE)) {
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.TRANSACTION)
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestStartBefore6April2017)))
    }

    // there's more than a year between start date and end date
    else if (endDate.isAfter(startDate.plusYears(1))) {
      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.TRANSACTION)
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestOverYearBetweenStartAndEnd)))
    }

    else {
      success()
    }

  }

  private def parseDate(input: String): Option[DateTime] = {
    val dateFormat = "yyyy-MM-dd"

    input.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") match {
      case true => {
        scala.util.control.Exception.allCatch[DateTime] opt (DateTime.parse(input, DateTimeFormat.forPattern(dateFormat)))
      }
      case _ => {
        None
      }
    }
  }

  private def getEndpointUrl(lisaManagerReferenceNumber: String): String = {
    s"/manager/$lisaManagerReferenceNumber/payments"
  }

}
