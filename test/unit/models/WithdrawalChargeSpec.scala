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

package unit.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsPath, Json, JsonValidationError}
import uk.gov.hmrc.lisaapi.models._

import java.time.LocalDate

class WithdrawalChargeSpec extends PlaySpec {

  val validRegular: JsObject = Json.obj(
    "claimPeriodStartDate"          -> "2017-12-06",
    "claimPeriodEndDate"            -> "2018-01-05",
    "withdrawalAmount"              -> 1000.00,
    "withdrawalChargeAmount"        -> 250.00,
    "withdrawalChargeAmountYTD"     -> 500.00,
    "fundsDeductedDuringWithdrawal" -> true,
    "withdrawalReason"              -> "Regular withdrawal"
  )

  val validSupersede: JsObject = Json.obj(
    "claimPeriodStartDate"          -> "2017-12-06",
    "claimPeriodEndDate"            -> "2018-01-05",
    "withdrawalAmount"              -> 1000.00,
    "withdrawalChargeAmount"        -> 250.00,
    "withdrawalChargeAmountYTD"     -> 500.00,
    "fundsDeductedDuringWithdrawal" -> true,
    "withdrawalReason"              -> "Superseded withdrawal",
    "automaticRecoveryAmount"       -> 250.00,
    "supersede"                     -> Json.obj(
      "originalTransactionId"          -> "2345678901",
      "originalWithdrawalChargeAmount" -> 250.00,
      "transactionResult"              -> 250.00,
      "reason"                         -> "Additional withdrawal"
    )
  )

  "Report withdrawal charge" must {

    "serialize for a regular bonus" in {
      val result = Json.parse(validRegular.toString()).as[ReportWithdrawalChargeRequest]

      result mustBe RegularWithdrawalChargeRequest(
        None,
        LocalDate.parse("2017-12-06"),
        LocalDate.parse("2018-01-05"),
        1000.00,
        250.00,
        500.00,
        fundsDeductedDuringWithdrawal = true,
        "Regular withdrawal"
      )
    }

    "serialize for a superseded bonus" in {
      val result = Json.parse(validSupersede.toString()).as[ReportWithdrawalChargeRequest]

      result mustBe SupersededWithdrawalChargeRequest(
        Some(250.00),
        LocalDate.parse("2017-12-06"),
        LocalDate.parse("2018-01-05"),
        1000.00,
        250.00,
        500.00,
        fundsDeductedDuringWithdrawal = true,
        Some(
          WithdrawalIncrease(
            "2345678901",
            250.00,
            250.00,
            "Additional withdrawal"
          )
        ),
        "Superseded withdrawal"
      )
    }

    "serialize for a withdrawal refund" in {
      val json   = validSupersede ++ Json.obj(
        "supersede" -> Json.obj(
          "originalTransactionId"          -> "2345678901",
          "originalWithdrawalChargeAmount" -> 250.00,
          "transactionResult"              -> 250.00,
          "reason"                         -> "Withdrawal refund"
        )
      )
      val result = Json.parse(json.toString()).as[ReportWithdrawalChargeRequest]

      result mustBe SupersededWithdrawalChargeRequest(
        Some(250.00),
        LocalDate.parse("2017-12-06"),
        LocalDate.parse("2018-01-05"),
        1000.00,
        250.00,
        500.00,
        fundsDeductedDuringWithdrawal = true,
        Some(
          WithdrawalRefund(
            "2345678901",
            250.00,
            250.00,
            "Withdrawal refund"
          )
        ),
        "Superseded withdrawal"
      )
    }

    "deserialize with des appropriate enum values for withdrawalReason and superseded reason" when {
      "using the des writes" in {
        val result = Json.toJson(
          SupersededWithdrawalChargeRequest(
            Some(250.00),
            LocalDate.parse("2017-12-06"),
            LocalDate.parse("2018-01-05"),
            1000.00,
            250.00,
            500.00,
            fundsDeductedDuringWithdrawal = true,
            Some(
              WithdrawalRefund(
                "2345678901",
                250.00,
                250.00,
                "Additional withdrawal"
              )
            ),
            "Superseded withdrawal"
          )
        )(ReportWithdrawalChargeRequest.desSupersededWithdrawalWrites)

        result mustBe Json.obj(
          "claimPeriodStartDate"          -> "2017-12-06",
          "claimPeriodEndDate"            -> "2018-01-05",
          "withdrawalAmount"              -> 1000.00,
          "withdrawalChargeAmount"        -> 250.00,
          "withdrawalChargeAmountYTD"     -> 500.00,
          "fundsDeductedDuringWithdrawal" -> true,
          "withdrawalReason"              -> "Superseded Withdrawal Charge",
          "automaticRecoveryAmount"       -> 250.00,
          "supersededDetail"              -> Json.obj(
            "transactionId"     -> "2345678901",
            "transactionAmount" -> 250.00,
            "transactionResult" -> 250.00,
            "reason"            -> "Withdrawal Refund"
          )
        )
      }
    }

    "error for an empty object" in {
      Json
        .parse("""{}""")
        .validate[ReportWithdrawalChargeRequest]
        .fold(
          errors => {
            val missingError = "error.path.missing"

            errors must contain((JsPath \ "claimPeriodStartDate", Seq(JsonValidationError(missingError))))
            errors must contain((JsPath \ "claimPeriodEndDate", Seq(JsonValidationError(missingError))))
            errors must contain((JsPath \ "withdrawalAmount", Seq(JsonValidationError(missingError))))
            errors must contain((JsPath \ "withdrawalChargeAmount", Seq(JsonValidationError(missingError))))
            errors must contain((JsPath \ "withdrawalChargeAmountYTD", Seq(JsonValidationError(missingError))))
            errors must contain((JsPath \ "fundsDeductedDuringWithdrawal", Seq(JsonValidationError(missingError))))
            errors must contain((JsPath \ "withdrawalReason", Seq(JsonValidationError(missingError))))
          },
          _ => fail("invalid json passed validation")
        )
    }

    "error for invalid data formats" in {
      val json = Json
        .obj(
          "claimPeriodStartDate"          -> "6th Dec 2017",
          "claimPeriodEndDate"            -> "5/1/2018",
          "withdrawalAmount"              -> -1000.00,
          "withdrawalChargeAmount"        -> 250.001,
          "withdrawalChargeAmountYTD"     -> -2.00,
          "fundsDeductedDuringWithdrawal" -> "true",
          "withdrawalReason"              -> "Withdrawal"
        )
        .toString()

      Json
        .parse(json)
        .validate[ReportWithdrawalChargeRequest]
        .fold(
          errors => {
            val dateFormatError       = "error.formatting.date"
            val withdrawalFormatError = "error.formatting.withdrawalReason"
            val numberFormatError     = "error.formatting.currencyNegativeDisallowed"

            errors must contain((JsPath \ "claimPeriodStartDate", Seq(JsonValidationError(dateFormatError))))
            errors must contain((JsPath \ "claimPeriodEndDate", Seq(JsonValidationError(dateFormatError))))
            errors must contain((JsPath \ "withdrawalAmount", Seq(JsonValidationError(numberFormatError))))
            errors must contain((JsPath \ "withdrawalChargeAmount", Seq(JsonValidationError(numberFormatError))))
            errors must contain((JsPath \ "withdrawalChargeAmountYTD", Seq(JsonValidationError(numberFormatError))))
            errors must contain(
              (JsPath \ "fundsDeductedDuringWithdrawal", Seq(JsonValidationError("error.expected.jsboolean")))
            )
            errors must contain((JsPath \ "withdrawalReason", Seq(JsonValidationError(withdrawalFormatError))))
          },
          _ => fail("invalid json passed validation")
        )
    }

  }

}
