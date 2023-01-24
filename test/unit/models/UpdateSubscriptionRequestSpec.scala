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

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.models.UpdateSubscriptionRequest

class UpdateSubscriptionRequestSpec extends PlaySpec {

  val validRequestJson = """{"firstSubscriptionDate":"2017-01-01"}"""

  "UpdateSubscriptionRequest" must {

    "serialize from json" in {
      val res = Json.parse(validRequestJson).validate[UpdateSubscriptionRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.firstSubscriptionDate.getYear mustBe 2017
          data.firstSubscriptionDate.getMonthOfYear mustBe 1
          data.firstSubscriptionDate.getDayOfMonth mustBe 1
        }
      }
    }

    "deserialize to json" in {
      val request = UpdateSubscriptionRequest(new DateTime("2017-01-01"))

      val json = Json.toJson[UpdateSubscriptionRequest](request)

      json mustBe Json.parse(validRequestJson)
    }


  }

}
