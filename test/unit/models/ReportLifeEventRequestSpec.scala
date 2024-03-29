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
import play.api.libs.json.{JsError, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.models._

import java.time.LocalDate

class ReportLifeEventRequestSpec extends PlaySpec {

  val validRequestJson = """{"eventType":"LISA Investor Terminal Ill Health", "eventDate":"2017-01-01"}"""

  "ReportLifeEventRequest" must {

    "serialize from json" in {
      val res = Json.parse(validRequestJson).validate[ReportLifeEventRequest]

      res match {
        case JsError(errors)       => fail()
        case JsSuccess(data, path) =>
          data.eventType mustBe "LISA Investor Terminal Ill Health"
          data.eventDate.getYear mustBe 2017
          data.eventDate.getMonthValue mustBe 1
          data.eventDate.getDayOfMonth mustBe 1
      }
    }

    "deserialize to json" in {
      val request = ReportLifeEventRequest("LISA Investor Terminal Ill Health", LocalDate.parse("2017-01-01"))

      val json = Json.toJson[ReportLifeEventRequestBase](request)

      json mustBe Json.parse(validRequestJson)
    }

  }

  "ReportLifeEventRequestBase" must {

    "correctly serialise a standard life event" in {
      val input  = ReportLifeEventRequest("LISA Investor Terminal Ill Health", LocalDate.parse("2017-01-01"))
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType" -> "LISA Investor Terminal Ill Health",
        "eventDate" -> "2017-01-01"
      )
    }

    "correctly serialise a standard fund release" in {
      val input  = InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = Some(
          FundReleasePropertyDetails(
            nameOrNumber = "1",
            postalCode = "AA11 1AA"
          )
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"            -> "Funds Release",
        "eventDate"            -> "2017-05-10",
        "withdrawalAmount"     -> 4000,
        "conveyancerReference" -> "CR12345-6789",
        "propertyDetails"      -> Json.obj(
          "nameOrNumber" -> "1",
          "postalCode"   -> "AA11 1AA"
        )
      )
    }

    "correctly serialise a standard fund release without conveyancerReference" in {
      val input  = InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = None,
        propertyDetails = Some(
          FundReleasePropertyDetails(
            nameOrNumber = "1",
            postalCode = "AA11 1AA"
          )
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"        -> "Funds Release",
        "eventDate"        -> "2017-05-10",
        "withdrawalAmount" -> 4000,
        "propertyDetails"  -> Json.obj(
          "nameOrNumber" -> "1",
          "postalCode"   -> "AA11 1AA"
        )
      )
    }

    "correctly serialise a standard fund release without propertyDetails" in {
      val input  = InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = None
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"            -> "Funds Release",
        "eventDate"            -> "2017-05-10",
        "withdrawalAmount"     -> 4000,
        "conveyancerReference" -> "CR12345-6789"
      )
    }

    "correctly serialise a superseded fund release" in {
      val input  = SupersedeFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-05"),
        withdrawalAmount = 5000,
        supersede = FundReleaseSupersedeDetails(
          originalLifeEventId = "3456789000",
          originalEventDate = LocalDate.parse("2017-05-10")
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"               -> "Funds Release",
        "eventDate"               -> "2017-05-05",
        "withdrawalAmount"        -> 5000,
        "supersededLifeEventDate" -> "2017-05-10",
        "supersededLifeEventID"   -> "3456789000"
      )
    }

    "correctly serialise a standard purchase extension" in {
      val input  = RequestStandardPurchaseExtension(
        eventDate = LocalDate.parse("2017-05-10"),
        eventType = "Extension one",
        fundReleaseId = "3456789001"
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"               -> "Extension one",
        "eventDate"               -> "2017-05-10",
        "fundsReleaseLifeEventID" -> "3456789001"
      )
    }

    "correctly serialise a superseded purchase extension" in {
      val input  = RequestSupersededPurchaseExtension(
        eventDate = LocalDate.parse("2017-05-10"),
        eventType = "Extension two",
        supersede = RequestExtensionSupersedeDetails(
          originalEventDate = LocalDate.parse("2017-05-10"),
          originalLifeEventId = "6789000001"
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"               -> "Extension two",
        "eventDate"               -> "2017-05-10",
        "supersededLifeEventID"   -> "6789000001",
        "supersededLifeEventDate" -> "2017-05-10"
      )
    }

    "correctly serialise a standard purchase outcome" in {
      val input  = RequestPurchaseOutcomeCompletedRequest(
        fundReleaseId = "3456789000",
        eventDate = LocalDate.parse("2017-05-05"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"               -> "Purchase Result",
        "eventDate"               -> "2017-05-05",
        "fundsReleaseLifeEventID" -> "3456789000",
        "propertyDetails"         -> Json.obj(
          "purchaseResult" -> "Purchase completed",
          "purchaseValue"  -> 250000
        )
      )
    }

    "correctly serialise a superseded purchase outcome" in {
      val input  = RequestPurchaseOutcomeSupersededCompletedRequest(
        eventDate = LocalDate.parse("2017-06-10"),
        propertyPurchaseResult = "Purchase completed",
        propertyPurchaseValue = 250000,
        supersede = PurchaseOutcomeSupersede(
          originalLifeEventId = "5678900001",
          originalEventDate = LocalDate.parse("2017-05-05")
        )
      )
      val output = Json.toJson[ReportLifeEventRequestBase](input)

      output mustBe Json.obj(
        "eventType"               -> "Purchase Result",
        "eventDate"               -> "2017-06-10",
        "propertyDetails"         -> Json.obj(
          "purchaseResult" -> "Purchase completed",
          "purchaseValue"  -> 250000
        ),
        "supersededLifeEventID"   -> "5678900001",
        "supersededLifeEventDate" -> "2017-05-05"
      )
    }

    "correctly serialise a standard annual return" in {
      val input = AnnualReturn(
        eventDate = LocalDate.parse("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 65,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      Json.toJson[ReportLifeEventRequestBase](input) mustBe Json.obj(
        "eventType"                      -> "Statutory Submission",
        "eventDate"                      -> "2018-04-05",
        "isaManagerName"                 -> "ISA Manager",
        "lisaAnnualCashSubs"             -> 0,
        "lisaAnnualStocksAndSharesSubs"  -> 55,
        "lisaMarketValueCash"            -> 0,
        "lisaMarketValueStocksAndShares" -> 65,
        "taxYear"                        -> "2018"
      )
    }

    "correctly serialise a superseded annual return" in {
      val input = AnnualReturn(
        eventDate = LocalDate.parse("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 65,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55,
        supersede = Some(
          AnnualReturnSupersede(
            originalLifeEventId = "1234567890",
            originalEventDate = LocalDate.parse("2017-04-01")
          )
        )
      )

      Json.toJson[ReportLifeEventRequestBase](input) mustBe Json.obj(
        "eventType"                      -> "Statutory Submission",
        "eventDate"                      -> "2018-04-05",
        "isaManagerName"                 -> "ISA Manager",
        "lisaAnnualCashSubs"             -> 0,
        "lisaAnnualStocksAndSharesSubs"  -> 55,
        "lisaMarketValueCash"            -> 0,
        "lisaMarketValueStocksAndShares" -> 65,
        "taxYear"                        -> "2018",
        "supersededLifeEventID"          -> "1234567890",
        "supersededLifeEventDate"        -> "2017-04-01"
      )
    }

  }

}
