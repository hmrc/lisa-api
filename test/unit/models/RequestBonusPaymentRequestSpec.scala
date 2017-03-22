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
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.controllers.JsonFormats
import uk.gov.hmrc.lisaapi.models.{Bonuses, HelpToBuyTransfer, InboundPayments, RequestBonusPaymentRequest}

import scala.io.Source

class RequestBonusPaymentRequestSpec extends PlaySpec with JsonFormats {

  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString

  "Bonus Payment Request" must {

    "serialize from json" in {
      val res = Json.parse(validBonusPaymentJson).validate[RequestBonusPaymentRequest]

      res match {
        case JsError(errors) => fail(s"Failed to serialize. Errors: ${errors.toString()}")
        case JsSuccess(data, path) => {
          data.lifeEventID mustBe "1234567891"
          data.periodStartDate mustBe new DateTime("2016-05-22")
          data.periodEndDate mustBe new DateTime("2017-05-22")
          data.transactionType mustBe "Bonus"
          data.htbTransfer mustBe HelpToBuyTransfer(0f, 0f)
          data.inboundPayments mustBe InboundPayments(4000f, 4000f, 4000f, 4000f)
          data.bonuses mustBe Bonuses(1000f, 1000f, None, "Life Event")
        }
      }
    }

  }

}
