/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.validation.ValidationError
import play.api.libs.json._

package object models {

  type Amount = BigDecimal
  type Name = String
  type Nino = String
  type InvestorId = String
  type AccountId = String
  type LifeEventId = String
  type LifeEventType = String
  type LisaManagerReferenceNumber = String
  type AccountClosureReason = String
  type BonusClaimReason = String

  private val MAX_AMOUNT = BigDecimal("99999999999999.98")

  object JsonReads {
    val nonNegativeAmount: Reads[Amount] = Reads
      .of[JsNumber]
      .filter(ValidationError("error.formatting.currency"))(
        value => {
          val amount = value.as[BigDecimal]

          amount >= 0 && amount.scale < 3 && amount <= MAX_AMOUNT
        }
      ).map((value: JsNumber) => value.as[BigDecimal])

    val lmrn: Reads[LisaManagerReferenceNumber] = Reads.pattern("^Z([0-9]{4}|[0-9]{6})$".r, "error.formatting.lmrn")
    val nino: Reads[Nino] = Reads.pattern(
      "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$".r,
      "error.formatting.nino")
    val name: Reads[Name] = Reads.pattern(
      "^[a-zA-Z &`\\-\\'^]{1,35}$".r,
      "error.formatting.name")
    val investorId: Reads[InvestorId] = Reads.pattern("^\\d{10}$".r, "error.formatting.investorId")
    val accountId: Reads[AccountId] = Reads.pattern("^[a-zA-Z0-9 :\\-]{1,20}$".r, "error.formatting.accountId")
    val lifeEventId: Reads[LifeEventId] = Reads.pattern("^\\d{10}$".r, "error.formatting.lifeEventId")
    val lifeEventType: Reads[LifeEventType] = Reads.pattern("^(LISA Investor Terminal Ill Health|LISA Investor Death)$".r, "error.formatting.lifeEventType")
    val accountClosureReason: Reads[AccountClosureReason] = Reads.pattern(
      "^(All funds withdrawn|Cancellation)$".r,
      "error.formatting.accountClosureReason")
    val bonusClaimReason: Reads[BonusClaimReason] = Reads.pattern(
      "^(Life Event|Regular Bonus)$".r,
      "error.formatting.claimReason"
    )

    val isoDate: Reads[DateTime] = isoDateReads()
    val notFutureDate: Reads[DateTime] = isoDateReads(false)

    private def isoDateReads(allowFutureDates: Boolean = true): Reads[DateTime] = new Reads[DateTime] {

      val dateFormat = "yyyy-MM-dd"
      val dateValidationMessage = "error.formatting.date"

      def reads(json: JsValue): JsResult[DateTime] = json match {
        case JsString(s) => parseDate(s) match {
          case Some(d: DateTime) => {
            if (!allowFutureDates && d.isAfterNow) {
              JsError(Seq(JsPath() -> Seq(ValidationError(dateValidationMessage))))
            }
            else {
              JsSuccess(d)
            }
          }
          case None => JsError(Seq(JsPath() -> Seq(ValidationError(dateValidationMessage))))
        }
        case _ => JsError(Seq(JsPath() -> Seq(ValidationError("error.expected.jsstring"))))
      }

      private def parseDate(input: String): Option[DateTime] =
        scala.util.control.Exception.allCatch[DateTime] opt (DateTime.parse(input, DateTimeFormat.forPattern(dateFormat)))

    }
  }

}