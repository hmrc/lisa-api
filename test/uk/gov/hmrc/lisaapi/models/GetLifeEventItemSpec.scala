/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsObject, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.models.{GetLifeEventItem, GetLifeEventItemPropertyDetails, GetLifeEventItemSupersede}

import java.time.LocalDate

class GetLifeEventItemSpec extends PlaySpec {

  val validInputJson: JsObject = Json.obj(
    fields = "lifeEventId" -> "1234567890",
    "lifeEventType"              -> "TERMINAL_ILLNESS",
    "lifeEventDate"              -> "2018-01-30",
    "isaManagerName"             -> "Company Name",
    "taxYear"                    -> "2018",
    "marketValueCash"            -> 1,
    "marketValueStocksAndShares" -> 2,
    "annualSubsCash"             -> 3,
    "annualSubsStocksAndShares"  -> 4,
    "withdrawalAmount"           -> 5.5,
    "conveyancerReference"       -> "X",
    "fundsReleaseLifeEventId"    -> "1",
    "propertyDetails"            -> Json.obj(
      fields = "nameOrNumber" -> "1",
      "postcode"       -> "AB1 1AB",
      "purchaseValue"  -> 6,
      "purchaseResult" -> "PURCHASE_COMPLETE"
    ),
    "supersededLifeEventId"      -> "3",
    "supersededLifeEventDate"    -> "2018-01-30",
    "lifeEventSupersededById"    -> "5"
  )

  val validItemWithAllFields: GetLifeEventItem = GetLifeEventItem(
    lifeEventId = "1234567890",
    eventType = "LISA Investor Terminal Ill Health",
    eventDate = LocalDate.parse("2018-01-30"),
    lisaManagerName = Some("Company Name"),
    taxYear = Some(2018),
    marketValueCash = Some(1),
    marketValueStocksAndShares = Some(2),
    annualSubsCash = Some(3),
    annualSubsStocksAndShares = Some(4),
    withdrawalAmount = Some(5.5),
    conveyancerReference = Some("X"),
    fundReleaseId = Some("1"),
    propertyPurchaseValue = Some(6),
    propertyPurchaseResult = Some("Purchase completed"),
    propertyDetails = Some(
      GetLifeEventItemPropertyDetails(
        nameOrNumber = "1",
        postalCode = "AB1 1AB"
      )
    ),
    supersede = Some(
      GetLifeEventItemSupersede(
        originalLifeEventId = "3",
        originalEventDate = LocalDate.parse("2018-01-30")
      )
    ),
    supersededBy = Some("5")
  )

  val validItemWithMinimumFields: GetLifeEventItem = GetLifeEventItem(
    lifeEventId = "1234567890",
    eventType = "Extension one",
    eventDate = LocalDate.parse("2018-01-01")
  )

  "GetLifeEventItem" must {

    "serialize from json" in {
      val res = validInputJson.validate[GetLifeEventItem]

      res mustBe JsSuccess(validItemWithAllFields)
    }

    "deserialize to json" in {
      val json = Json.toJson[GetLifeEventItem](validItemWithAllFields)

      json mustBe Json.obj(
        fields = "lifeEventId" -> "1234567890",
        "eventType"                  -> "LISA Investor Terminal Ill Health",
        "eventDate"                  -> "2018-01-30",
        "lisaManagerName"            -> "Company Name",
        "taxYear"                    -> 2018,
        "marketValueCash"            -> 1,
        "marketValueStocksAndShares" -> 2,
        "annualSubsCash"             -> 3,
        "annualSubsStocksAndShares"  -> 4,
        "withdrawalAmount"           -> 5.5,
        "conveyancerReference"       -> "X",
        "fundReleaseId"              -> "1",
        "propertyPurchaseValue"      -> 6,
        "propertyPurchaseResult"     -> "Purchase completed",
        "propertyDetails"            -> Json.obj(fields = "nameOrNumber" -> "1", "postalCode" -> "AB1 1AB"),
        "supersede"                  -> Json.obj(fields = "originalLifeEventId" -> "3", "originalEventDate" -> "2018-01-30"),
        "supersededBy"               -> "5"
      )
    }

    "map event types correctly" in {
      val eventOfType: String => GetLifeEventItem =
        (eventType: String) => (validInputJson ++ Json.obj(fields = "lifeEventType" -> eventType)).as[GetLifeEventItem]

      eventOfType("TERMINAL_ILLNESS").eventType mustBe "LISA Investor Terminal Ill Health"
      eventOfType("DEATH").eventType mustBe "LISA Investor Death"
      eventOfType("PURCHASE_FUNDS_RELEASE").eventType mustBe "Funds release"
      eventOfType("EXTENSION_ONE").eventType mustBe "Extension one"
      eventOfType("EXTENSION_TWO").eventType mustBe "Extension two"
      eventOfType("PURCHASE_RESULT").eventType mustBe "Purchase outcome"
      eventOfType("STATUTORY_SUBMISSION").eventType mustBe "Statutory Submission"
    }

    "map purchase results correctly" in {
      val eventOfResult: String => GetLifeEventItem = (purchaseResult: String) =>
        (validInputJson ++ Json.obj(fields =
          "propertyDetails" -> Json.obj(fields = "purchaseResult" -> purchaseResult)
        )).as[GetLifeEventItem]

      eventOfResult("PURCHASE_COMPLETE").propertyPurchaseResult mustBe Some("Purchase completed")
      eventOfResult("PURCHASE_FAILED").propertyPurchaseResult mustBe Some("Purchase failed")
    }

    "serialize with the minimum fields required" in {
      val res = Json
        .obj(
          fields = "lifeEventId" -> "1234567890",
          "lifeEventType" -> "EXTENSION_ONE",
          "lifeEventDate" -> "2018-01-01"
        )
        .validate[GetLifeEventItem]

      res mustBe JsSuccess(validItemWithMinimumFields)
    }

    "deserialize with the minimum fields required" in {
      val json = Json.toJson[GetLifeEventItem](validItemWithMinimumFields)

      json mustBe Json.obj(
        fields = "lifeEventId" -> "1234567890",
        "eventType" -> "Extension one",
        "eventDate" -> "2018-01-01"
      )
    }

  }

}
