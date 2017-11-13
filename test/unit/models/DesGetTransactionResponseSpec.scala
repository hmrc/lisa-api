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

package unit.models

import org.joda.time.DateTime
import org.scalatest.MustMatchers
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.models.des._

class DesGetTransactionResponseSpec extends PlaySpec with MustMatchers {

  "Valid 'Paid' transactions" must {
    "serialize to a DesGetTransactionPaid class" in {
      val json = """{
                   |    "status": "Paid",
                   |    "paymentDate": "2000-01-01",
                   |    "paymentReference": "002630000993",
                   |    "paymentAmount": 1.00
                   |}""".stripMargin

      val result = Json.parse(json).as[DesGetTransactionResponse]

      result mustBe DesGetTransactionPaid(
        paymentDate = new DateTime("2000-01-01"),
        paymentReference = "002630000993",
        paymentAmount = 1
      )
    }
  }

  "Valid 'Pending' transactions" must {
    "serialize to a DesGetTransactionPending class" in {
      val json = """{
                   |    "status": "Pending",
                   |    "paymentDueDate": "2000-01-01",
                   |    "paymentAmount": 1.00
                   |}""".stripMargin

      val result = Json.parse(json).as[DesGetTransactionResponse]

      result mustBe DesGetTransactionPending(
        paymentDueDate = new DateTime("2000-01-01"),
        paymentAmount = 1
      )
    }
  }

  "Valid 'Cancelled' transactions" must {
    "serialize to a DesGetTransactionCancelled object" in {
      val json = """{
                   |    "status": "Cancelled"
                   |}""".stripMargin

      val result = Json.parse(json).as[DesGetTransactionResponse]

      result mustBe DesGetTransactionCancelled
    }
  }

  "Valid 'Charge' transactions" must {
    "serialize to a DesGetTransactionCharge class" in {
      val json = """{
                   |    "status": "Due",
                   |    "chargeReference": "XM00261010895"
                   |}""".stripMargin

      val result = Json.parse(json).as[DesGetTransactionResponse]

      result mustBe DesGetTransactionCharge(
        status = "Due",
        chargeReference = "XM00261010895"
      )
    }
  }

  "Invalid transactions" must {
    "fail validation" in {
      val json = """{
                   |    "status": "Due"
                   |}""".stripMargin

      val result = Json.parse(json).validate[DesGetTransactionResponse]

      result.isSuccess mustBe false
    }
  }

}
