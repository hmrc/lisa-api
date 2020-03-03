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

package uk.gov.hmrc.lisaapi

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._
import play.api.libs.functional.syntax._

package object models {

  type Amount = BigDecimal
  type Name = String
  type Nino = String
  type InvestorId = String
  type AccountId = String
  type TransactionId = String
  type LifeEventId = String
  type LifeEventType = String
  type LisaManagerReferenceNumber = String
  type AccountClosureReason = String
  type BonusClaimReason = String
  type BonusClaimSupersedeReason = String
  type WithdrawalReason = String
  type WithdrawalSupersedeReason = String
  type PropertyPurchaseResult = String
  type FundReleaseId = String
  type ExtensionId = String
  type LisaManagerName = String

  private val MIN_AMOUNT = BigDecimal("-99999999999999.98")
  private val MAX_AMOUNT = BigDecimal("99999999999999.98")

  object JsonReads {
    val amount: Reads[Amount] = Reads
      .of[JsNumber]
      .filter(JsonValidationError("error.formatting.currencyNegativeAllowed"))(
        value => {
          val amount = value.as[BigDecimal]

          amount >= MIN_AMOUNT && amount.scale < 3 && amount <= MAX_AMOUNT
        }
      ).map((value: JsNumber) => value.as[BigDecimal])
    val nonNegativeAmount: Reads[Amount] = Reads
      .of[JsNumber]
      .filter(JsonValidationError("error.formatting.currencyNegativeDisallowed"))(
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
    val accountId: Reads[AccountId] = Reads.pattern("^[a-zA-Z0-9 :/-]{1,20}$".r, "error.formatting.accountId")
    val transactionId: Reads[TransactionId] = Reads.pattern("^[0-9]{1,10}$".r, "error.formatting.transactionId")
    val lifeEventId: Reads[LifeEventId] = Reads.pattern("^\\d{10}$".r, "error.formatting.lifeEventId")
    val fundReleaseId: Reads[FundReleaseId] = Reads.pattern("^\\d{10}$".r, "error.formatting.fundReleaseId")
    val lifeEventType: Reads[LifeEventType] = Reads.pattern("^(LISA Investor Terminal Ill Health|LISA Investor Death)$".r, "error.formatting.lifeEventType")
    val accountClosureReason: Reads[AccountClosureReason] = Reads.pattern(
      "^(All funds withdrawn|Cancellation)$".r,
      "error.formatting.accountClosureReason")
    val bonusClaimReasonV2: Reads[BonusClaimReason] = Reads.pattern(
      "^(Life Event|Regular Bonus|Superseded Bonus)$".r,
      "error.formatting.claimReason"
    )
    val bonusClaimReasonV1: Reads[BonusClaimReason] = Reads.pattern(
      "^(Life Event|Regular Bonus)$".r,
      "error.formatting.claimReason"
    )
    val lisaManagerName: Reads[LisaManagerName] = Reads.pattern("^[a-zA-Z0-9 '/,&().-]{1,50}$".r, "error.formatting.lisaManagerName")
    val taxYearReads: Reads[Int] = Reads.filter[Int](JsonValidationError("error.formatting.taxYear"))((p:Int) => p > 999 && p < 10000)
    val annualFigures: Reads[Int] = Reads.filter[Int](JsonValidationError("error.formatting.annualFigures"))((p:Int) => p >= 0)

    val isoDate: Reads[DateTime] = isoDateReads()
    val notFutureDate: Reads[DateTime] = isoDateReads(false)

    private def isoDateReads(allowFutureDates: Boolean = true): Reads[DateTime] = new Reads[DateTime] {

      val dateFormat = "yyyy-MM-dd"
      val dateValidationMessage = "error.formatting.date"

      def reads(json: JsValue): JsResult[DateTime] = json match {
        case JsString(s) => parseDate(s) match {
          case Some(d: DateTime) => {
            if (!allowFutureDates && d.isAfterNow) {
              JsError(Seq(JsPath() -> Seq(JsonValidationError(dateValidationMessage))))
            }
            else {
              JsSuccess(d)
            }
          }
          case None => JsError(Seq(JsPath() -> Seq(JsonValidationError(dateValidationMessage))))
        }
        case _ => JsError(Seq(JsPath() -> Seq(JsonValidationError("error.expected.jsstring"))))
      }

      private def parseDate(input: String): Option[DateTime] = {
        input.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}") match {
          case true => scala.util.control.Exception.allCatch[DateTime] opt (DateTime.parse(input, DateTimeFormat.forPattern(dateFormat)))
          case _ => None
        }
      }

    }
    
  }

}