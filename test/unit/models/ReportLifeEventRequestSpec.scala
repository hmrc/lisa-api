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
import uk.gov.hmrc.lisaapi.models._

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

      val json = Json.toJson[ReportLifeEventRequestBase](request)

      json mustBe Json.parse(validRequestJson)
    }

  }

  "ReportLifeEventRequestBase" must {

    "correctly serialise a standard life event" in {
      val input = ReportLifeEventRequest( "LISA Investor Terminal Ill Health", new DateTime("2017-01-01"))
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "LISA Investor Terminal Ill Health",
        "eventDate" -> "2017-01-01"
      )
    }

    "correctly serialise a standard fund release" in {
      val input = InitialFundReleaseRequest(
        eventDate = new DateTime("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = "CR12345-6789",
        propertyDetails = FundReleasePropertyDetails(
          nameOrNumber = "1",
          postalCode = "AA11 1AA"
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "Funds Release",
        "eventDate" -> "2017-05-10",
        "withdrawalAmount" -> 4000,
        "conveyancerReference" -> "CR12345-6789",
        "propertyDetails" -> Json.obj(
          "nameOrNumber" -> "1",
          "postalCode" -> "AA11 1AA"
        )
      )
    }

    "correctly serialise a superseded fund release" in {
      val input = SupersedeFundReleaseRequest(
        eventDate = new DateTime("2017-05-05"),
        withdrawalAmount = 5000,
        supersede = FundReleaseSupersedeDetails(
          originalFundReleaseId = "3456789000",
          originalEventDate = new DateTime("2017-05-10")
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "Funds Release",
        "eventDate" -> "2017-05-05",
        "withdrawalAmount" -> 5000,
        "supersededLifeEventDate" -> "2017-05-10",
        "supersededLifeEventID" -> "3456789000"
      )
    }

    "correctly serialise a standard purchase extension" in {
      val input = RequestStandardPurchaseExtension(
        eventDate = new DateTime("2017-05-10"),
        eventType = "Extension one",
        fundReleaseId = "3456789001"
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "Extension one",
        "eventDate" -> "2017-05-10",
        "fundsReleaseLifeEventID" -> "3456789001"
      )
    }

    "correctly serialise a superseded purchase extension" in {
      val input = RequestSupersededPurchaseExtension(
        eventDate = new DateTime("2017-05-10"),
        eventType = "Extension two",
        supersede = RequestExtensionSupersedeDetails(
          originalEventDate = new DateTime("2017-05-10"),
          originalExtensionId = "6789000001"
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "Extension two",
        "eventDate" -> "2017-05-10",
        "supersededLifeEventID" -> "6789000001",
        "supersededLifeEventDate" -> "2017-05-10"
      )
    }

    "correctly serialise a standard purchase outcome" in {
      val input = RequestPurchaseOutcomeStandardRequest(
        fundReleaseId = "3456789000",
        eventDate = new DateTime("2017-05-05"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "Purchase Result",
        "eventDate" -> "2017-05-05",
        "fundsReleaseLifeEventID" -> "3456789000",
        "propertyDetails" -> Json.obj(
          "purchaseResult" -> "Purchase completed",
          "purchaseValue" -> 250000
        )
      )
    }

    "correctly serialise a superseded purchase outcome" in {
      val input = RequestPurchaseOutcomeSupersededRequest(
        fundReleaseId = "3456789000",
        eventDate = new DateTime("2017-06-10"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000,
        supersede = PurchaseOutcomeSupersede(
          originalPurchaseOutcomeId = "5678900001",
          originalEventDate = new DateTime("2017-05-05")
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "Purchase Result",
        "eventDate" -> "2017-06-10",
        "fundsReleaseLifeEventID" -> "3456789000",
        "propertyDetails" -> Json.obj(
          "purchaseResult" -> "Purchase completed",
          "purchaseValue" -> 250000
        ),
        "supersededLifeEventID" -> "5678900001",
        "supersededLifeEventDate" -> "2017-05-05"
      )
    }

    "correctly serialise a standard annual return" in {
      val input = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 65,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      Json.toJson[ReportLifeEventRequestBase](input) mustBe Json.obj(
        "eventType" -> "Statutory Submission",
        "eventDate" -> "2018-04-05",
        "isaManagerName" -> "ISA Manager",
        "lisaAnnualCashSubs" -> 0,
        "lisaAnnualStocksAndSharesSubs" -> 55,
        "lisaMarketValueCash" -> 0,
        "lisaMarketValueStocksAndShares" -> 65,
        "taxYear" -> 2018
      )
    }

    "correctly serialise a superseded annual return" in {
      val input = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 65,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55,
        supersede = Some(
          AnnualReturnSupersede(
            originalLifeEventId = "1234567890",
            originalEventDate = new DateTime("2017-04-01")
          )
        )
      )

      Json.toJson[ReportLifeEventRequestBase](input) mustBe Json.obj(
        "eventType" -> "Statutory Submission",
        "eventDate" -> "2018-04-05",
        "isaManagerName" -> "ISA Manager",
        "lisaAnnualCashSubs" -> 0,
        "lisaAnnualStocksAndSharesSubs" -> 55,
        "lisaMarketValueCash" -> 0,
        "lisaMarketValueStocksAndShares" -> 65,
        "taxYear" -> 2018,
        "supersededLifeEventID" -> "1234567890",
        "supersededLifeEventDate" -> "2017-04-01"
      )
    }

  }

}
