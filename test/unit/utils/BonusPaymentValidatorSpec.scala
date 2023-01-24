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

package unit.utils

import helpers.BaseTestFixture
import org.joda.time.DateTime
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.CurrentDateService
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator

import scala.io.Source

class BonusPaymentValidatorSpec extends BaseTestFixture with BeforeAndAfter {

  val mockDateService: CurrentDateService = mock[CurrentDateService]
  val bonusPaymentValidator: BonusPaymentValidator = new BonusPaymentValidator(mockDateService)

  before {
    reset(mockDateService)
    when(mockDateService.now()).thenReturn(DateTime.now)
  }

  val validBonusPaymentJson: String = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.additional-bonus.json")).mkString
  val validBonusPayment: RequestBonusPaymentRequest = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]

  "newSubsForPeriod and htbTransferForPeriod" should {

    "return two errors" when {

      "they are both 0" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(0))
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 2
        errors(0).path mustBe Some("/inboundPayments/newSubsForPeriod")
        errors(1).path mustBe Some("/htbTransfer/htbTransferInForPeriod")
      }

      "they are both none" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = None)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = None)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 2
        errors(0).path mustBe Some("/inboundPayments/newSubsForPeriod")
        errors(1).path mustBe Some("/htbTransfer/htbTransferInForPeriod")
      }

    }

    "return one error" when {

      "newSubsForPeriod is 0 and htbTransfer is none" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(0))
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = None)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/inboundPayments/newSubsForPeriod")
      }

      "htbTransfer is 0 and newSubsForPeriod is none" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = None)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/htbTransfer/htbTransferInForPeriod")
      }

    }

  }

  "newSubsYTD" should {

    "return an error" when {

      "it is zero and newSubsForPeriod is not" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(1), newSubsYTD = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/inboundPayments/newSubsYTD")
      }

    }

  }

  "htbTransferTotalYTD" should {

    "return an error" when {

      "it is zero and htbTransferInForPeriod is not" in {
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 1, htbTransferTotalYTD = 0)
        val request = validBonusPayment.copy(htbTransfer = Some(htb))

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/htbTransfer/htbTransferTotalYTD")
      }

    }

  }

  "totalSubsForPeriod" should {

    "return an error" when {

      "it is zero" in {
        val ibp = validBonusPayment.inboundPayments.copy(totalSubsForPeriod = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/inboundPayments/totalSubsForPeriod")
      }

    }

  }

  "totalSubsYTD" should {

    "return an error" when {

      "it is less than totalSubsForPeriod" in {
        val ibp = validBonusPayment.inboundPayments.copy(totalSubsForPeriod = 10, totalSubsYTD = 5)
        val request = validBonusPayment.copy(inboundPayments = ibp)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/inboundPayments/totalSubsYTD")
      }

    }

  }

  "bonusDueForPeriod" should {

    "return an error" when {

      "it is zero or less" in {
        val bon = validBonusPayment.bonuses.copy(bonusDueForPeriod = 0)
        val request = validBonusPayment.copy(bonuses = bon)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/bonuses/bonusDueForPeriod")
      }

    }

  }

  "totalBonusDueYTD" should {

    "return an error" when {

      "it is zero or less" in {
        val bon = validBonusPayment.bonuses.copy(totalBonusDueYTD = 0)
        val request = validBonusPayment.copy(bonuses = bon)

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 1
        errors(0).path mustBe Some("/bonuses/totalBonusDueYTD")
      }

    }

  }

  "periodStartDate" should {

    "validate correctly" when {

      "the current date is the sixth and they're submitting for today" in {
        val today = new DateTime("2017-04-06")
        val periodEndDate = new DateTime("2017-05-05")

        reset(mockDateService)
        when(mockDateService.now()).thenReturn(today)

        val request = validBonusPayment.copy(periodStartDate = today, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List()
      }

    }

    "return an error" when {

      "it is not the 6th day of the month" in {
        val periodStartDate = new DateTime("2017-05-01")
        val periodEndDate = new DateTime("2017-06-05")
        val request = validBonusPayment.copy(periodStartDate = periodStartDate, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodStartDate must be the 6th day of the month",
            path = Some("/periodStartDate")
          )
        )
      }

      "the supplied date is in the future" in {
        val nextMonth = DateTime.now.plusMonths(1).withDayOfMonth(6)
        val periodEndDate = nextMonth.plusMonths(1).withDayOfMonth(5)
        val request = validBonusPayment.copy(periodStartDate = nextMonth, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodStartDate may not be a future date",
            path = Some("/periodStartDate")
          )
        )
      }

      "the supplied date is prior to 6 April 2017" in {
        val periodStartDate = new DateTime("2017-03-06")
        val periodEndDate = new DateTime("2017-04-05")
        val request = validBonusPayment.copy(periodStartDate = periodStartDate, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors must contain(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodStartDate cannot be before 6 April 2017",
            path = Some("/periodStartDate")
          )
        )
      }

    }

  }

  "periodEndDate" should {

    "validate correctly" when {

      "the end date crosses into another year" in {
        val periodStartDate = new DateTime("2017-12-06")
        val periodEndDate = new DateTime("2018-01-05")
        val request = validBonusPayment.copy(periodStartDate = periodStartDate, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List()
      }

    }

    "return an error" when {

      "it is not the 5th day of the month" in {
        val request = validBonusPayment.copy(periodEndDate = new DateTime("2017-05-01"))

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodEndDate must be the 5th day of the month which occurs after the periodStartDate",
            path = Some("/periodEndDate")
          )
        )
      }

      "it is two months after the periodStartDate" in {
        val periodStartDate = new DateTime("2017-12-06")
        val periodEndDate = new DateTime("2018-02-05")
        val request = validBonusPayment.copy(periodStartDate = periodStartDate, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodEndDate must be the 5th day of the month which occurs after the periodStartDate",
            path = Some("/periodEndDate")
          )
        )
      }

      "it is before the periodStartDate" in {
        val periodStartDate = new DateTime("2017-06-06")
        val periodEndDate = new DateTime("2017-05-05")
        val request = validBonusPayment.copy(periodStartDate = periodStartDate, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodEndDate must be the 5th day of the month which occurs after the periodStartDate",
            path = Some("/periodEndDate")
          )
        )
      }

      "the supplied date is prior to 6 April 2017" in {
        val periodStartDate = new DateTime("2017-03-06")
        val periodEndDate = new DateTime("2017-04-05")
        val request = validBonusPayment.copy(periodStartDate = periodStartDate, periodEndDate = periodEndDate)

        val errors = bonusPaymentValidator.validate(request)

        errors must contain(
          ErrorValidation(
            errorCode = "INVALID_DATE",
            message = "The periodEndDate cannot be before 6 April 2017",
            path = Some("/periodEndDate")
          )
        )
      }

    }

  }

  "supersede" should {

    "return no errors" when {

      "a bonus claim is superseded down to zero" in {
        val request = validBonusPayment.copy(
          inboundPayments = InboundPayments(None, 0, 0, 0),
          bonuses = Bonuses(0, 0, None, "Superseded bonus claim"),
          supersede = Some(BonusRecovery(100, "1", 100, -100)),
          htbTransfer = None
        )

        bonusPaymentValidator.validate(request) mustBe Nil
      }

    }

    "return an error" when {

      "it is not populated and the claimReason is 'Superseded Bonus'" in {
        val request = validBonusPayment.copy(supersede = None, bonuses = Bonuses(10f, 10f, Some(10f), "Superseded Bonus"))

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "MISSING_FIELD",
            message = "This field is required",
            path = Some("/supersede")
          )
        )
      }

      "it is populated and the claimReason is 'Regular Bonus'" in {
        val request = validBonusPayment.copy(
          inboundPayments = InboundPayments(None, 0, 0, 0),
          bonuses = Bonuses(0, 0, None, "Regular Bonus"),
          supersede = Some(BonusRecovery(100, "1", 100, -100)),
          htbTransfer = None
        )

        val errors = bonusPaymentValidator.validate(request)

        errors mustBe List(
          ErrorValidation(
            errorCode = "SUPERSEDE_NOT_ALLOWED",
            message = "Supersede details are not allowed",
            path = Some("/claimReason")
          )
        )
      }

    }

  }

  "the validate method" should {

    "return no errors" when {

      "everything is valid" in {
        val errors = bonusPaymentValidator.validate(validBonusPayment)

        errors.size mustBe 0
      }

    }

    "return multiple errors" when {

      "validation fails multiple conditions" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(1), newSubsYTD = 0, totalSubsForPeriod = 0)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 1, htbTransferTotalYTD = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))

        val errors = bonusPaymentValidator.validate(request)

        errors.size mustBe 3
        errors(0).path mustBe Some("/htbTransfer/htbTransferTotalYTD")
        errors(1).path mustBe Some("/inboundPayments/newSubsYTD")
        errors(2).path mustBe Some("/inboundPayments/totalSubsForPeriod")
      }

    }

  }
}
