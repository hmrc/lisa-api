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
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetBulkPaymentNotFoundResponse, GetBulkPaymentSuccessResponse}
import uk.gov.hmrc.lisaapi.services.{BulkPaymentService, CurrentDateService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkPaymentController extends LisaController with LisaConstants {

  val currentDateService: CurrentDateService = CurrentDateService
  val service: BulkPaymentService = BulkPaymentService

  def getBulkPayment(lisaManager: String, start: String, end: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime, LisaMetricKeys.TRANSACTION)
      withValidLMRN(lisaManager) { () =>
        withValidDates(start, end) { (startDate, endDate) =>
          val response = service.getBulkPayment(lisaManager, startDate, endDate)

          response map {
            case s: GetBulkPaymentSuccessResponse => Ok(Json.toJson(s))
            case GetBulkPaymentNotFoundResponse => NotFound(Json.toJson(ErrorPaymentNotFound))
            case _ => InternalServerError(Json.toJson(ErrorInternalServerError))
          }
        }
      }
    }

  private def withValidDates(startDate: String, endDate: String)
                            (success: (DateTime, DateTime) => Future[Result]): Future[Result] = {
    val start = parseDate(startDate)
    val end = parseDate(endDate)

    (start, end) match {
      case (Some(s), Some(e)) => {
        withDatesWithinBusinessRules(s, e) { () =>
          success(s, e)
        }
      }
      case (None, Some(_)) => Future.successful(BadRequest(Json.toJson(ErrorBadRequestStart)))
      case (Some(_), None) => Future.successful(BadRequest(Json.toJson(ErrorBadRequestEnd)))
      case _ => Future.successful(BadRequest(Json.toJson(ErrorBadRequestStartEnd)))
    }
  }

  private def withDatesWithinBusinessRules(startDate: DateTime, endDate: DateTime)
                                          (success: () => Future[Result]): Future[Result] = {

    // end date is in the future
    if (endDate.isAfter(currentDateService.now())) {
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestEndInFuture)))
    }

    // end date is before start date
    else if (endDate.isBefore(startDate)) {
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestEndBeforeStart)))
    }

    // start date is before 6 april 2017
    else if (startDate.isBefore(LISA_START_DATE)) {
      Future.successful(Forbidden(Json.toJson(ErrorBadRequestStartBefore6April2017)))
    }

    // there's more than a year between start date and end date
    else if (endDate.isAfter(startDate.plusYears(1))) {
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

}
