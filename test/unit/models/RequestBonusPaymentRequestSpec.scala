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
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
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
          data.lifeEventId mustBe Some("1234567891")
          data.periodStartDate mustBe new DateTime("2017-04-06")
          data.periodEndDate mustBe new DateTime("2017-05-05")
          data.htbTransfer mustBe Some(HelpToBuyTransfer(0f, 0f))
          data.inboundPayments mustBe InboundPayments(Some(4000f), 4000f, 4000f, 4000f)
          data.bonuses mustBe Bonuses(1000f, 1000f, Some(1000f), "Life Event")
        }
      }
    }

    "deserialize to json" in {
      val data = RequestBonusPaymentRequest(
        lifeEventId = Some("1234567891"),
        periodStartDate = new DateTime("2017-04-06"),
        periodEndDate = new DateTime("2017-05-05"),
        htbTransfer = Some(HelpToBuyTransfer(0f, 0f)),
        inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
        bonuses = Bonuses(1000f, 1000f, Some(1000f), "Life Event")
      )

      val json = Json.toJson[RequestBonusPaymentRequest](data)

      json mustBe Json.parse(validBonusPaymentJson)
    }

    "catch an invalid life event id" in {
      val res = Json.parse(validBonusPaymentJson.replace("1234567891", "X")).validate[RequestBonusPaymentRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/lifeEventId" && errors.contains(ValidationError("error.formatting.lifeEventId"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }
  }

}
