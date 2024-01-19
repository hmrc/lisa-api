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

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, Json}
import uk.gov.hmrc.lisaapi.models._

import java.time.LocalDate

class RequestPurchaseOutcomeRequestSpec extends PlaySpec {

  "RequestPurchaseOutcomeCompletedRequest" must {

    val standardJson =
      """{"fundReleaseId":"3456789000","eventDate":"2017-05-05","propertyPurchaseResult":"Purchase completed","propertyPurchaseValue":250000}"""

    "serialise from json" in {
      Json.parse(standardJson).as[RequestPurchaseOutcomeRequest] mustBe RequestPurchaseOutcomeCompletedRequest(
        fundReleaseId = "3456789000",
        eventDate = LocalDate.parse("2017-05-05"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000
      )
    }

    "serialise to json" in {
      val input = RequestPurchaseOutcomeCompletedRequest(
        fundReleaseId = "3456789000",
        eventDate = LocalDate.parse("2017-05-05"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000
      )

      Json
        .toJson[ReportLifeEventRequestBase](input)
        .toString() mustBe """{"eventType":"Purchase Result","eventDate":"2017-05-05","fundsReleaseLifeEventID":"3456789000","propertyDetails":{"purchaseResult":"Purchase completed","purchaseValue":250000}}"""
    }

    "validate the fundReleaseId" in {
      val json = standardJson.replace("3456789000", "x")

      Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "fundReleaseId",
        "error.formatting.fundReleaseId"
      )
    }

    "validate the eventDate" when {
      "the formatting is incorrect" in {
        val json = standardJson.replace("2017-05-05", "2017-05")

        Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "eventDate",
          "error.formatting.date"
        )
      }
      "the date is in the future" in {
        val json = standardJson.replace("2017-05-05", "3000-01-01")

        Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "eventDate",
          "error.formatting.date"
        )
      }
    }

    "validate the propertyPurchaseResult" in {
      val json = standardJson.replace("Purchase completed", "Purchase complete")

      Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "propertyPurchaseResult",
        "error.formatting.propertyPurchaseResult"
      )
    }

    "validate the propertyPurchaseValue" in {
      val json = standardJson.replace("250000", "-100")

      Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "propertyPurchaseValue",
        "error.formatting.currencyNegativeDisallowed"
      )
    }

  }

  "RequestPurchaseOutcomeFailedRequest" must {

    val failedRequest = Json.obj(
      "fundReleaseId"          -> "3456789000",
      "eventDate"              -> "2017-05-05",
      "propertyPurchaseResult" -> "Purchase failed"
    )

    "serialise from json" in {
      failedRequest.as[RequestPurchaseOutcomeRequest] mustBe RequestPurchaseOutcomeFailedRequest(
        fundReleaseId = "3456789000",
        eventDate = LocalDate.parse("2017-05-05"),
        propertyPurchaseResult = "Purchase failed"
      )
    }

    "serialise to json" in {
      val input = RequestPurchaseOutcomeFailedRequest(
        fundReleaseId = "3456789000",
        eventDate = LocalDate.parse("2017-05-05"),
        propertyPurchaseResult = "Purchase failed"
      )

      Json
        .toJson[ReportLifeEventRequestBase](input)
        .toString() mustBe """{"eventType":"Purchase Result","eventDate":"2017-05-05","fundsReleaseLifeEventID":"3456789000","propertyDetails":{"purchaseResult":"Purchase failed"}}"""
    }

    "validate the fundReleaseId" in {
      val json = failedRequest ++ Json.obj("fundReleaseId" -> "x")

      json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "fundReleaseId",
        "error.formatting.fundReleaseId"
      )
    }

    "validate the eventDate" when {
      "the formatting is incorrect" in {
        val json = failedRequest ++ Json.obj("eventDate" -> "2017-05")

        json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(JsPath \ "eventDate", "error.formatting.date")
      }
      "the date is in the future" in {
        val json = failedRequest ++ Json.obj("eventDate" -> "3000-01-01")

        json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(JsPath \ "eventDate", "error.formatting.date")
      }
    }

    "validate the propertyPurchaseResult" in {
      val json = failedRequest ++ Json.obj("propertyPurchaseResult" -> "Purchase failure")

      json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "propertyPurchaseResult",
        "error.formatting.propertyPurchaseResult"
      )
    }

  }

  "RequestPurchaseOutcomeSupersededCompletedRequest" must {

    val supersededJson =
      """{"eventDate":"2017-06-10","propertyPurchaseResult":"Purchase completed","propertyPurchaseValue":250000,"supersede":{"originalLifeEventId":"5678900001","originalEventDate":"2017-05-05"}}"""

    "serialise from json" in {
      val json = supersededJson

      Json.parse(json).as[RequestPurchaseOutcomeRequest] mustBe RequestPurchaseOutcomeSupersededCompletedRequest(
        eventDate = LocalDate.parse("2017-06-10"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000,
        supersede = PurchaseOutcomeSupersede(
          originalLifeEventId = "5678900001",
          originalEventDate = LocalDate.parse("2017-05-05")
        )
      )
    }

    "serialise to json" in {
      val input = RequestPurchaseOutcomeSupersededCompletedRequest(
        eventDate = LocalDate.parse("2017-06-10"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000,
        supersede = PurchaseOutcomeSupersede(
          originalLifeEventId = "5678900001",
          originalEventDate = LocalDate.parse("2017-05-05")
        )
      )

      Json
        .toJson(input)(RequestPurchaseOutcomeRequest.supersedeCompletedWrites)
        .toString() mustBe """{"eventType":"Purchase Result","eventDate":"2017-06-10","propertyDetails":{"purchaseResult":"Purchase completed","purchaseValue":250000},"supersededLifeEventID":"5678900001","supersededLifeEventDate":"2017-05-05"}"""
    }

    "validate the eventDate" when {
      "the formatting is incorrect" in {
        val json = supersededJson.replace("2017-06-10", "2017-06")

        Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "eventDate",
          "error.formatting.date"
        )
      }
      "the date is in the future" in {
        val json = supersededJson.replace("2017-06-10", "3000-01-01")

        Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "eventDate",
          "error.formatting.date"
        )
      }
    }

    "validate the propertyPurchaseResult" in {
      val json = supersededJson.replace("Purchase completed", "Purchase complete")

      Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "propertyPurchaseResult",
        "error.formatting.propertyPurchaseResult"
      )
    }

    "validate the propertyPurchaseValue" in {
      val json = supersededJson.replace("250000", "-100")

      Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "propertyPurchaseValue",
        "error.formatting.currencyNegativeDisallowed"
      )
    }

    "validate the originalLifeEventId" in {
      val json = supersededJson.replace("5678900001", "x")

      Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "supersede" \ "originalLifeEventId",
        "error.formatting.lifeEventId"
      )
    }

    "validate the originalEventDate" when {
      "the formatting is incorrect" in {
        val json = supersededJson.replace("2017-05-05", "2017-05")

        Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "supersede" \ "originalEventDate",
          "error.formatting.date"
        )
      }
      "the date is in the future" in {
        val json = supersededJson.replace("2017-05-05", "3000-01-01")

        Json.parse(json).validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "supersede" \ "originalEventDate",
          "error.formatting.date"
        )
      }
    }

  }

  "RequestPurchaseOutcomeSupersededFailedRequest" must {

    val supersededFailedRequest = Json.obj(
      "eventDate"              -> "2017-06-10",
      "propertyPurchaseResult" -> "Purchase failed",
      "supersede"              -> Json.obj(
        "originalLifeEventId" -> "5678900001",
        "originalEventDate"   -> "2017-05-05"
      )
    )

    "serialise from json" in {
      supersededFailedRequest.as[RequestPurchaseOutcomeRequest] mustBe RequestPurchaseOutcomeSupersededFailedRequest(
        eventDate = LocalDate.parse("2017-06-10"),
        propertyPurchaseResult = "Purchase failed",
        supersede = PurchaseOutcomeSupersede(
          originalLifeEventId = "5678900001",
          originalEventDate = LocalDate.parse("2017-05-05")
        )
      )
    }

    "serialise to json" in {
      val input = RequestPurchaseOutcomeSupersededFailedRequest(
        eventDate = LocalDate.parse("2017-06-10"),
        propertyPurchaseResult = "Purchase failed",
        supersede = PurchaseOutcomeSupersede(
          originalLifeEventId = "5678900001",
          originalEventDate = LocalDate.parse("2017-05-05")
        )
      )

      Json
        .toJson(input)(RequestPurchaseOutcomeRequest.supersedeFailedWrites)
        .toString() mustBe """{"eventType":"Purchase Result","eventDate":"2017-06-10","propertyDetails":{"purchaseResult":"Purchase failed"},"supersededLifeEventID":"5678900001","supersededLifeEventDate":"2017-05-05"}"""
    }

    "validate the eventDate" when {
      "the formatting is incorrect" in {
        val json = supersededFailedRequest ++ Json.obj("eventDate" -> "2017-06")

        json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(JsPath \ "eventDate", "error.formatting.date")
      }
      "the date is in the future" in {
        val json = supersededFailedRequest ++ Json.obj("eventDate" -> "3000-01-01")

        json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(JsPath \ "eventDate", "error.formatting.date")
      }
    }

    "validate the propertyPurchaseResult" in {
      val json = supersededFailedRequest ++ Json.obj("propertyPurchaseResult" -> "Purchase failure")

      json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "propertyPurchaseResult",
        "error.formatting.propertyPurchaseResult"
      )
    }

    "validate the originalLifeEventId" in {
      val json = supersededFailedRequest ++ Json.obj(
        "supersede" -> Json.obj(
          "originalLifeEventId" -> "x",
          "originalEventDate"   -> "2017-05-05"
        )
      )

      json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(
        JsPath \ "supersede" \ "originalLifeEventId",
        "error.formatting.lifeEventId"
      )
    }

    "validate the originalEventDate" when {
      "the formatting is incorrect" in {
        val json = supersededFailedRequest ++ Json.obj(
          "supersede" -> Json.obj(
            "originalLifeEventId" -> "5678900001",
            "originalEventDate"   -> "2017-05"
          )
        )

        json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "supersede" \ "originalEventDate",
          "error.formatting.date"
        )
      }
      "the date is in the future" in {
        val json = supersededFailedRequest ++ Json.obj(
          "supersede" -> Json.obj(
            "originalLifeEventId" -> "5678900001",
            "originalEventDate"   -> "3000-01-01"
          )
        )

        json.validate[RequestPurchaseOutcomeRequest] mustBe JsError(
          JsPath \ "supersede" \ "originalEventDate",
          "error.formatting.date"
        )
      }
    }

  }

}
