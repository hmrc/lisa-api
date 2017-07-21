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

package unit.utils

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsPath, Json}
import uk.gov.hmrc.lisaapi.models.RequestBonusPaymentRequest
import uk.gov.hmrc.lisaapi.utils.{BonusPaymentValidationRequest, BonusPaymentValidator}

import scala.io.Source

class BonusPaymentValidatorSpec extends PlaySpec {

  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString
  val validBonusPayment = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]

  "New subs or transfer" should {

    "return two errors" when {

      "newSubsForPeriod and htbTransferForPeriod are both 0" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(0))
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))

        val errors = SUT.validate(request)

        errors.size mustBe 2
        errors(0).path mustBe Some("/inboundPayments/newSubsForPeriod")
        errors(1).path mustBe Some("/htbTransfer/htbTransferInForPeriod")

      }

      /*
      "newSubsForPeriod and htbTransferForPeriod are both none" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = None)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = None))

        val res = SUT.newSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 2
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsForPeriod"
        res.errors(1)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferInForPeriod"

      }
      */

    }

    /*
    "return one error" when {

      "newSubsForPeriod is 0 and htbTransfer is none" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(0))
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = None))

        val res = SUT.newSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsForPeriod"

      }

      "htbTransfer is 0 and newSubsForPeriod is none" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = None)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 0)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb)))

        val res = SUT.newSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferInForPeriod"

      }

    }
    */

  }

  /*
  "NewSubsYTD" should {

    "return an error" when {

      "it is zero and newSubsForPeriod is not" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(1), newSubsYTD = 0)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp))

        val res = SUT.newSubsYTDGtZeroIfNewSubsForPeriodGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsYTD"
      }

    }

  }

  "HtbTransferTotalYTD" should {

    "return an error" when {

      "it is zero and htbTransferInForPeriod is not" in {
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 1, htbTransferTotalYTD = 0)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(htbTransfer = Some(htb)))

        val res = SUT.htbTransferTotalYTDGtZeroIfHtbTransferInForPeriodGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferTotalYTD"
      }

    }

  }

  "totalSubsForPeriod" should {

    "return an error" when {

      "it is zero" in {
        val ibp = validBonusPayment.inboundPayments.copy(totalSubsForPeriod = 0)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp))

        val res = SUT.totalSubsForPeriodGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "totalSubsForPeriod"
      }

    }

  }

  "totalSubsYTD" should {

    "return an error" when {

      "it is less than totalSubsForPeriod" in {
        val ibp = validBonusPayment.inboundPayments.copy(totalSubsForPeriod = 10, totalSubsYTD = 5)
        val request = validBonusPayment.copy(inboundPayments = ibp)

        val res = SUT.validate(request)

        res.size mustBe 1
        res(0)._1 mustBe JsPath \ "inboundPayments" \ "totalSubsYTD"
      }

    }

  }

  "bonusDueForPeriod" should {

    "return an error" when {

      "it is zero or less" in {
        val bon = validBonusPayment.bonuses.copy(bonusDueForPeriod = 0)
        val request = validBonusPayment.copy(bonuses = bon)

        val res = SUT.validate(request)

        res.size mustBe 1
        res(0)._1 mustBe JsPath \ "bonuses" \ "bonusDueForPeriod"
      }

    }

  }

  "totalBonusDueYTD" should {

    "return an error" when {

      "it is zero or less" in {
        val bon = validBonusPayment.bonuses.copy(totalBonusDueYTD = 0)
        val request = validBonusPayment.copy(bonuses = bon)

        val res = SUT.validate(request)

        res.size mustBe 1
        res(0)._1 mustBe JsPath \ "bonuses" \ "totalBonusDueYTD"
      }

    }

  }

  "Validate" should {
    "return no errors" when {
      "everything is valid" in {
        val errors = SUT.validate(validBonusPayment)

        errors.size mustBe 0
      }
    }
    "return all errors" when {
      "validation fails multiple conditions" in {
        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(1), newSubsYTD = 0, totalSubsForPeriod = 0)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 1, htbTransferTotalYTD = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))

        val res = SUT.validate(request)

        res.size mustBe 3
        res(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsYTD"
        res(1)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferTotalYTD"
        res(2)._1 mustBe JsPath \ "inboundPayments" \ "totalSubsForPeriod"
      }
    }
  }
  */

  val SUT = BonusPaymentValidator

}
