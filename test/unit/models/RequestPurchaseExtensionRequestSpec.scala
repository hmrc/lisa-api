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
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import uk.gov.hmrc.lisaapi.models._

class RequestPurchaseExtensionRequestSpec extends PlaySpec {

  "RequestStandardPurchaseExtension" must {

    "serialize from json" in {

      val json = """{"fundReleaseId":"3456789001","eventDate":"2017-05-10","eventType":"Extension one"}"""

      Json.parse(json).as[RequestPurchaseExtension] mustBe RequestStandardPurchaseExtension(
        eventDate = new DateTime("2017-05-10"),
        eventType = "Extension one",
        fundReleaseId = "3456789001"
      )

    }

    "deserialize to json" in {

      val input = RequestStandardPurchaseExtension(
        eventDate = new DateTime("2017-05-10"),
        eventType = "Extension one",
        fundReleaseId = "3456789001"
      )

      val expected = """{"eventType":"Extension one","eventDate":"2017-05-10","fundsReleaseLifeEventID":"3456789001"}"""

      Json.toJson(input)(RequestPurchaseExtension.standardWrites).toString() mustBe expected

    }

    "validate the fund release id" in {

      val json = """{"fundReleaseId":"x","eventDate":"2017-05-10","eventType":"Extension one"}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "fundReleaseId", Seq(JsonValidationError("error.formatting.fundReleaseId"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

    "validate the event type" in {

      val json = """{"fundReleaseId":"3456789001","eventDate":"2017-05-10","eventType":"Extension 1"}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "eventType", Seq(JsonValidationError("error.formatting.extensionType"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

    "validate the event date" in {

      val json = """{"fundReleaseId":"3456789001","eventDate":"3000-05-10","eventType":"Extension one"}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "eventDate", Seq(JsonValidationError("error.formatting.date"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

  }

  "RequestSupersededPurchaseExtension" must {

    "serialize from json" in {

      val json = """{"eventDate":"2017-05-11","eventType":"Extension one","supersede":{"originalEventDate":"2017-05-10","originalLifeEventId":"6789000001"}}"""

      Json.parse(json).as[RequestPurchaseExtension] mustBe RequestSupersededPurchaseExtension(
        eventDate = new DateTime("2017-05-11"),
        eventType = "Extension one",
        supersede = RequestExtensionSupersedeDetails(
          originalEventDate = new DateTime("2017-05-10"),
          originalLifeEventId = "6789000001"
        )
      )

    }

    "deserialize to json" in {

      val input = RequestSupersededPurchaseExtension(
        eventDate = new DateTime("2017-05-10"),
        eventType = "Extension one",
        supersede = RequestExtensionSupersedeDetails(
          originalEventDate = new DateTime("2017-05-10"),
          originalLifeEventId = "6789000001"
        )
      )

      val expected = """{"eventType":"Extension one","eventDate":"2017-05-10","supersededLifeEventDate":"2017-05-10","supersededLifeEventID":"6789000001"}"""

      Json.toJson(input)(RequestPurchaseExtension.supersededWrites).toString() mustBe expected

    }

    "validate the event type" in {

      val json = """{"eventDate":"2017-05-11","eventType":"Extension 1","supersede":{"originalEventDate":"2017-05-10","originalLifeEventId":"6789000001"}}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "eventType", Seq(JsonValidationError("error.formatting.extensionType"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

    "validate the event date" in {

      val json = """{"eventDate":"3000-05-11","eventType":"Extension one","supersede":{"originalEventDate":"2017-05-10","originalLifeEventId":"6789000001"}}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "eventDate", Seq(JsonValidationError("error.formatting.date"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

    "validate the supersede object" in {

      val json = """{"eventDate":"2017-05-11","eventType":"Extension one", "supersede": {}}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors.length mustBe 2
          errors must contain((JsPath \ "supersede" \ "originalEventDate", Seq(JsonValidationError("error.path.missing"))))
          errors must contain((JsPath \ "supersede" \ "originalLifeEventId", Seq(JsonValidationError("error.path.missing"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

    "validate the originalEventDate" in {

      val json = """{"eventDate":"2017-05-11","eventType":"Extension one","supersede":{"originalEventDate":"3000-05-10","originalLifeEventId":"6789000001"}}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "supersede" \ "originalEventDate", Seq(JsonValidationError("error.formatting.date"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

    "validate the originalLifeEventId" in {

      val json = """{"eventDate":"2017-05-11","eventType":"Extension one","supersede":{"originalEventDate":"2017-05-10","originalLifeEventId":"one"}}"""

      val res = Json.parse(json).validate[RequestPurchaseExtension]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "supersede" \ "originalLifeEventId", Seq(JsonValidationError("error.formatting.lifeEventId"))))
        }
        case _ => fail("Parsed invalid json as being valid")
      }

    }

  }

}
