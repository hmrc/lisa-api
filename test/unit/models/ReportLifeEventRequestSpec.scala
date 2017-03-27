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
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.models.ReportLifeEventRequest
/**
  * Created by mark on 17/03/17.
  */
class ReportLifeEventRequestSpec extends PlaySpec with JsonFormats {

  val validRequestJson = """{"eventType":"LISA Investor Terminal Ill Health", "eventDate":"2017-04-06"}"""

  "ReportLifeEventRequest" must {

    "serialize from json" in {
      val res = Json.parse(validRequestJson).validate[ReportLifeEventRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.eventType mustBe "LISA Investor Terminal Ill Health"
          data.eventDate.getYear mustBe 2017
          data.eventDate.getMonthOfYear mustBe 4
          data.eventDate.getDayOfMonth mustBe 6
        }
      }
    }

    "deserialize to json" in {
      val request = ReportLifeEventRequest( "LISA Investor Terminal Ill Health", new DateTime("2017-04-06"))

      val json = Json.toJson[ReportLifeEventRequest](request)

      json mustBe Json.parse(validRequestJson)
    }


  }

}
