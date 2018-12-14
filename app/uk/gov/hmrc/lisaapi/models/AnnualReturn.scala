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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads, Writes}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.services.CurrentDateService

import scala.collection.mutable.ListBuffer

case class AnnualReturnSupersede (
  originalLifeEventId: LifeEventId,
  originalEventDate: DateTime
)

case class AnnualReturn (
  eventDate: DateTime,
  lisaManagerName: LisaManagerName,
  taxYear: Int,
  marketValueCash: Int,
  marketValueStocksAndShares: Int,
  annualSubsCash: Int,
  annualSubsStocksAndShares: Int,
  supersede: Option[AnnualReturnSupersede] = None
) extends ReportLifeEventRequestBase

object AnnualReturnSupersede {
  implicit val dateReads: Reads[DateTime] = JsonReads.notFutureDate
  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd")
  implicit val lifeEventReads: Reads[LifeEventId] = JsonReads.lifeEventId
  implicit val formats = Json.format[AnnualReturnSupersede]
}

object AnnualReturn {
  implicit val dateWrites = Writes.jodaDateWrites("yyyy-MM-dd")

  implicit val reads: Reads[AnnualReturn] = (
    (JsPath \ "eventDate").read(JsonReads.notFutureDate) and
    (JsPath \ "lisaManagerName").read(JsonReads.lisaManagerName) and
    (JsPath \ "taxYear").read(JsonReads.taxYearReads) and
    (JsPath \ "marketValueCash").read(JsonReads.annualFigures) and
    (JsPath \ "marketValueStocksAndShares").read(JsonReads.annualFigures) and
    (JsPath \ "annualSubsCash").read(JsonReads.annualFigures) and
    (JsPath \ "annualSubsStocksAndShares").read(JsonReads.annualFigures) and
    (JsPath \ "supersede").readNullable[AnnualReturnSupersede]
  )(AnnualReturn.apply _)

  implicit val writes = Json.writes[AnnualReturn]

  val desWrites: Writes[AnnualReturn] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[DateTime] and
    (JsPath \ "taxYear").write[Int] and
    (JsPath \ "isaManagerName").write[String] and
    (JsPath \ "lisaMarketValueCash").write[Int] and
    (JsPath \ "lisaMarketValueStocksAndShares").write[Int] and
    (JsPath \ "lisaAnnualCashSubs").write[Int] and
    (JsPath \ "lisaAnnualStocksAndSharesSubs").write[Int] and
    (JsPath \ "supersededLifeEventDate").writeNullable[DateTime] and
    (JsPath \ "supersededLifeEventID").writeNullable[LifeEventId]
  ){req: AnnualReturn =>
    val supersededLifeEventDate = req.supersede match {
      case None => None
      case Some(sup) => Some(sup.originalEventDate)
    }
    val supersededLifeEventID = req.supersede match {
      case None => None
      case Some(sup) => Some(sup.originalLifeEventId)
    }

    (
      "Statutory Submission",
      req.eventDate,
      req.taxYear,
      req.lisaManagerName,
      req.marketValueCash,
      req.marketValueStocksAndShares,
      req.annualSubsCash,
      req.annualSubsStocksAndShares,
      supersededLifeEventDate,
      supersededLifeEventID
    )
  }
}

trait AnnualReturnValidator extends LisaConstants {
  val currentDateService: CurrentDateService

  case class ValidationRequest(data: AnnualReturn, errors: Seq[ErrorValidation] = Nil)

  def validate(req: AnnualReturn): Seq[ErrorValidation] = {
    (
      taxYearIsAfter2016 andThen
      taxYearIsNotCurrent andThen
      taxYearIsNotInFuture andThen
      onlyCashOrStocksHaveBeenSpecified
    ).apply(ValidationRequest(req)).errors
  }

  private val taxYearIsAfter2016: PartialFunction[ValidationRequest, ValidationRequest] = {
    case req: ValidationRequest if req.data.taxYear < 2017 => {
      req.copy(errors = req.errors :+ ErrorValidation(DATE_ERROR, "The taxYear cannot be before 2017", Some("/taxYear")))
    }
    case req: ValidationRequest => req
  }

  private val taxYearIsNotCurrent: PartialFunction[ValidationRequest, ValidationRequest] = {
    case req: ValidationRequest => {
      val now = currentDateService.now()
      val currentYear = now.getYear()
      val currentTaxYearStart = new DateTime(currentYear, TAX_YEAR_START_MONTH, TAX_YEAR_START_DAY, 0, 0)
      val currentTaxYear = if (now.isBefore(currentTaxYearStart)) currentYear else currentYear + 1

      if (req.data.taxYear == currentTaxYear) {
        req.copy(errors = req.errors :+ ErrorValidation(DATE_ERROR, "The taxYear must be a previous tax year", Some("/taxYear")))
      }
      else {
        req
      }
    }
  }

  private val taxYearIsNotInFuture: PartialFunction[ValidationRequest, ValidationRequest] = {
    case req: ValidationRequest if req.data.taxYear > currentDateService.now().getYear => {
      req.copy(errors = req.errors :+ ErrorValidation(DATE_ERROR, "The taxYear cannot be in the future", Some("/taxYear")))
    }
    case req: ValidationRequest => req
  }

  private val onlyCashOrStocksHaveBeenSpecified: (ValidationRequest) => ValidationRequest = (req: ValidationRequest) => {
    val cashValue = req.data.marketValueCash
    val cashSubs = req.data.annualSubsCash
    val stockValue = req.data.marketValueStocksAndShares
    val stockSubs = req.data.annualSubsStocksAndShares

    val cashHasBeenSpecified = cashValue > 0 || cashSubs > 0
    val stocksHaveBeenSpecified = stockValue > 0 || stockSubs > 0

    if (cashHasBeenSpecified && stocksHaveBeenSpecified) {
      val newErrs = new ListBuffer[ErrorValidation]()
      val errorMessage = "You can only give cash or stocks and shares values"

      if (cashValue > 0) {
        newErrs += ErrorValidation(
          errorCode = MONETARY_ERROR,
          message = errorMessage,
          path = Some("/marketValueCash")
        )
      }

      if (cashSubs > 0) {
        newErrs += ErrorValidation(
          errorCode = MONETARY_ERROR,
          message = errorMessage,
          path = Some("/annualSubsCash")
        )
      }

      if (stockValue > 0) {
        newErrs += ErrorValidation(
          errorCode = MONETARY_ERROR,
          message = errorMessage,
          path = Some("/marketValueStocksAndShares")
        )
      }

      if (stockSubs > 0) {
        newErrs += ErrorValidation(
          errorCode = MONETARY_ERROR,
          message = errorMessage,
          path = Some("/annualSubsStocksAndShares")
        )
      }

      req.copy(errors = req.errors ++ newErrs)
    }
    else {
      req
    }
  }

}

object AnnualReturnValidator extends AnnualReturnValidator {
  val currentDateService: CurrentDateService = CurrentDateService
}