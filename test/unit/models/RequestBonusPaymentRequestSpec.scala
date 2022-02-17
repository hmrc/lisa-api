/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models._

import scala.io.Source

class RequestBonusPaymentRequestSpec extends PlaySpec {

  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString
  val validBonusPaymentDesJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.des.json")).mkString
  val validBonusPayment = RequestBonusPaymentRequest(
    lifeEventId = Some("1234567891"),
    periodStartDate = new DateTime("2017-04-06"),
    periodEndDate = new DateTime("2017-05-05"),
    htbTransfer = Some(HelpToBuyTransfer(10f, 10f)),
    inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
    bonuses = Bonuses(1000f, 1000f, Some(1000f), "Life Event"),
    supersede = Some(BonusRecovery(100.00f, "234567890", 100.00f, 200.00f))
  )

  "Bonus Payment Request" must {

    "serialize from json" in {
      val res = Json.parse(validBonusPaymentJson).validate[RequestBonusPaymentRequest]

      res mustBe JsSuccess(validBonusPayment)
    }

    "deserialize to json" in {
      val json = Json.toJson[RequestBonusPaymentRequest](validBonusPayment)

      json mustBe Json.parse(validBonusPaymentDesJson)
    }

    "catch an invalid life event id" in {
      val res = Json.parse(validBonusPaymentJson.replace("1234567891", "X")).validate[RequestBonusPaymentRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[JsonValidationError]) => {
              path.toString() == "/lifeEventId" && errors.contains(JsonValidationError("error.formatting.lifeEventId"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

  }

}
