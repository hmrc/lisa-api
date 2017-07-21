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

    "return no errors" when {

      "everything is good" in {

        val request = BonusPaymentValidationRequest(data = validBonusPayment)

        SUT.validateNewSubsOrHtbTransferGtZero(request) mustBe request

      }

    }

    "return two errors" when {

      "newSubsForPeriod and htbTransferForPeriod are both 0" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(0))
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 0)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb)))

        val res = SUT.validateNewSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 2
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsForPeriod"
        res.errors(1)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferInForPeriod"

      }

      "newSubsForPeriod and htbTransferForPeriod are both none" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = None)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = None))

        val res = SUT.validateNewSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 2
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsForPeriod"
        res.errors(1)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferInForPeriod"

      }

    }

    "return one error" when {

      "newSubsForPeriod is 0 and htbTransfer is none" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(0))
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = None))

        val res = SUT.validateNewSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "inboundPayments" \ "newSubsForPeriod"

      }

      "htbTransfer is 0 and newSubsForPeriod is none" in {

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = None)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 0)
        val request = BonusPaymentValidationRequest(data = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb)))

        val res = SUT.validateNewSubsOrHtbTransferGtZero(request)

        res.data mustBe request.data
        res.errors.size mustBe 1
        res.errors(0)._1 mustBe JsPath \ "htbTransfer" \ "htbTransferInForPeriod"

      }

    }

  }

  val SUT = BonusPaymentValidator

}
