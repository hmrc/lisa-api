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

package unit.utils

import org.mockito.Mockito._
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models.{ReportWithdrawalChargeRequest, SupersededWithdrawalChargeRequest, WithdrawalIncrease}
import uk.gov.hmrc.lisaapi.services.CurrentDateService
import uk.gov.hmrc.lisaapi.utils.WithdrawalChargeValidator

import scala.io.Source

class WithdrawalChargeValidatorSpec extends PlaySpec
  with MockitoSugar
  with BeforeAndAfter {

  before {
    reset(mockDateService)
    when(mockDateService.now()).thenReturn(DateTime.now)
  }

  val validWithdrawalJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.withdrawal-charge.json")).mkString
  val validWithdrawal = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]

  "claimPeriodStartDate" should {

    "pass validation" when {

      "the current date is the sixth and they're submitting for today" in {
        val today = new DateTime("2017-04-06")
        val periodEndDate = new DateTime("2017-05-05")

        reset(mockDateService)
        when(mockDateService.now()).thenReturn(today)

        val request = validWithdrawal.copy(claimPeriodStartDate = today, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors mustBe List()
      }

    }

    "return an error" when {

      "it is not the 6th day of the month" in {
        val periodStartDate = new DateTime("2017-05-01")
        val periodEndDate = new DateTime("2017-06-05")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodStartDate must be the 6th day of the month",
            path = Some("/claimPeriodStartDate")
          )
        )
      }

      "the supplied date is in the future" in {
        val nextMonth = DateTime.now.plusMonths(1).withDayOfMonth(6)
        val periodEndDate = nextMonth.plusMonths(1).withDayOfMonth(5)
        val request = validWithdrawal.copy(claimPeriodStartDate = nextMonth, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodStartDate may not be a future date",
            path = Some("/claimPeriodStartDate")
          )
        )
      }

      "the supplied date is prior to 6 April 2017" in {
        val periodStartDate = new DateTime("2017-03-06")
        val periodEndDate = new DateTime("2017-04-05")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors must contain(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodStartDate cannot be before 6 April 2017",
            path = Some("/claimPeriodStartDate")
          )
        )
      }

    }

  }

  "claimPeriodEndDate" should {

    "pass validation" when {

      "the end date crosses into another year" in {
        val periodStartDate = new DateTime("2017-12-06")
        val periodEndDate = new DateTime("2018-01-05")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors mustBe List()
      }

    }

    "return an error" when {

      "it is not the 5th day of the month" in {
        val request = validWithdrawal.copy(claimPeriodEndDate = new DateTime("2017-05-01"))

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            path = Some("/claimPeriodEndDate")
          )
        )
      }

      "it is two months after the claimPeriodStartDate" in {
        val periodStartDate = new DateTime("2017-12-06")
        val periodEndDate = new DateTime("2018-02-05")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            path = Some("/claimPeriodEndDate")
          )
        )
      }

      "it is before the claimPeriodStartDate" in {
        val periodStartDate = new DateTime("2017-06-06")
        val periodEndDate = new DateTime("2017-05-05")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            path = Some("/claimPeriodEndDate")
          )
        )
      }

      "the supplied date is prior to 6 April 2017" in {
        val periodStartDate = new DateTime("2017-03-06")
        val periodEndDate = new DateTime("2017-04-05")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors must contain(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The claimPeriodEndDate cannot be before 6 April 2017",
            path = Some("/claimPeriodEndDate")
          )
        )
      }

    }

  }

  "supersede" should {
    "pass validation" when {
      "withdrawalReason is Regular withdrawal and supersede is not set" in {
        val request = validWithdrawal.copy(withdrawalReason = "Regular withdrawal", supersede = None)

        val errors = SUT.validate(request)

        errors mustBe List()
      }
    }

    "return an error" when {
      "withdrawalReason is Regular withdrawal and supersede is set" in {
        val request = validWithdrawal.copy(withdrawalReason = "Regular withdrawal", supersede = Some(WithdrawalIncrease(
          "2345678901",
          250.00,
          250.00,
          "Additional Withdrawal"
        )))

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "SUPERSEDE_NOT_ALLOWED",
            message = "Supersede details are not allowed",
            path = Some("/withdrawalReason")
          )
        )
      }
    }
  }

  "withdrawalAmount" should {

    "pass validation" when {

      "it is zero and fundsDeductedDuringWithdrawal is false" in {
        val request = validWithdrawal.copy(automaticRecoveryAmount = Some(0), fundsDeductedDuringWithdrawal = false)

        val errors = SUT.validate(request)

        errors mustBe List()
      }

      "it is less than the withdrawalChargeAmount and fundsDeductedDuringWithdrawal is false" in {
        val request = validWithdrawal.copy(automaticRecoveryAmount = Some(validWithdrawal.withdrawalChargeAmount - 0.01), fundsDeductedDuringWithdrawal = false)

        val errors = SUT.validate(request)

        errors mustBe List()
      }

      "it is equal to the withdrawalChargeAmount" in {
        val request = validWithdrawal.copy(automaticRecoveryAmount = Some(validWithdrawal.withdrawalChargeAmount))

        val errors = SUT.validate(request)

        errors mustBe List()
      }

    }

    "return an error" when {

      "it is greater than the withdrawalChargeAmount and fundsDeductedDuringWithdrawal is false" in {
        val request = validWithdrawal.copy(automaticRecoveryAmount = Some(validWithdrawal.withdrawalChargeAmount + 0.01), fundsDeductedDuringWithdrawal = false)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_MONETARY_AMOUNT",
            message = "automaticRecoveryAmount cannot be more than withdrawalChargeAmount",
            path = Some("/automaticRecoveryAmount")
          )
        )
      }

      "it is greater than the withdrawalChargeAmount and fundsDeductedDuringWithdrawal is true" in {
        val request = validWithdrawal.copy(automaticRecoveryAmount = Some(validWithdrawal.withdrawalChargeAmount + 0.01), fundsDeductedDuringWithdrawal = true)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "AMOUNT_MISMATCH",
            message = "automaticRecoveryAmount and withdrawalChargeAmount must be the same",
            path = Some("/automaticRecoveryAmount")
          ),
          ErrorValidation(
            errorCode = "INVALID_MONETARY_AMOUNT",
            message = "automaticRecoveryAmount cannot be more than withdrawalChargeAmount",
            path = Some("/automaticRecoveryAmount")
          )
        )
      }

      "it is less than the withdrawalChargeAmount and fundsDeductedDuringWithdrawal is true" in {
        val request = validWithdrawal.copy(automaticRecoveryAmount = Some(validWithdrawal.withdrawalChargeAmount - 0.01), fundsDeductedDuringWithdrawal = true)

        val errors = SUT.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "AMOUNT_MISMATCH",
            message = "automaticRecoveryAmount and withdrawalChargeAmount must be the same",
            path = Some("/automaticRecoveryAmount")
          )
        )
      }

    }

  }

  "the validate method" should {

    "return no errors" when {
      "everything is valid" in {
        val errors = SUT.validate(validWithdrawal)

        errors.size mustBe 0
      }
    }

    "return multiple errors" when {
      "validation fails multiple conditions" in {
        val periodStartDate = new DateTime("2018-03-01")
        val periodEndDate = new DateTime("2018-05-01")
        val request = validWithdrawal.copy(claimPeriodStartDate = periodStartDate, claimPeriodEndDate = periodEndDate)

        val errors = SUT.validate(request)

        errors.size mustBe 2
        errors(0).path mustBe Some("/claimPeriodStartDate")
        errors(1).path mustBe Some("/claimPeriodEndDate")
      }
    }

  }

  val mockDateService: CurrentDateService = mock[CurrentDateService]

  object SUT extends WithdrawalChargeValidator(mockDateService)
}
