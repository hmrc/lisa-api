/*
 * Copyright 2018 HM Revenue & Customs
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
import uk.gov.hmrc.lisaapi.models.ReportLifeEventRequest

class ReportLifeEventRequestSpec extends PlaySpec {

  val validRequestJson = """{"eventType":"LISA Investor Terminal Ill Health", "eventDate":"2017-01-01"}"""

  "ReportLifeEventRequest" must {

    "serialize from json" in {
      val res = Json.parse(validRequestJson).validate[ReportLifeEventRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.eventType mustBe "LISA Investor Terminal Ill Health"
          data.eventDate.getYear mustBe 2017
          data.eventDate.getMonthOfYear mustBe 1
          data.eventDate.getDayOfMonth mustBe 1
        }
      }
    }

    "deserialize to json" in {
      val request = ReportLifeEventRequest( "LISA Investor Terminal Ill Health", new DateTime("2017-01-01"))

      val json = Json.toJson[ReportLifeEventRequest](request)

      json mustBe Json.parse(validRequestJson)
    }


  }

}
