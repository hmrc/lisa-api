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
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsObject, JsPath, Json}
import uk.gov.hmrc.lisaapi.models.{AnnualReturn, AnnualReturnSupersede}

class AnnualReturnSpec extends PlaySpec {

  "AnnualReturnSupersede" must {

    val validJson = Json.obj(
      "originalLifeEventId" -> "1234567890",
      "originalEventDate" -> "2018-05-01"
    )

    "serialize from json" in {
      val expected = AnnualReturnSupersede(originalLifeEventId = "1234567890", originalEventDate = new DateTime("2018-05-01"))

      validJson.as[AnnualReturnSupersede] mustBe expected
    }
    "deserialize to json" in {
      val input = AnnualReturnSupersede(originalLifeEventId = "1234567890", originalEventDate = new DateTime("2018-05-01"))

      Json.toJson[AnnualReturnSupersede](input) mustBe validJson
    }
    "not allow a future event date" in {
      val invalidJson = validJson ++ Json.obj("originalEventDate" -> "3018-05-01")

      invalidJson.validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalEventDate"), List(ValidationError("error.formatting.date")))
      ))
    }
    "not allow a badly formatted event date" in {
      val invalidJson = validJson ++ Json.obj("originalEventDate" -> "30-12-2017")

      invalidJson.validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalEventDate"), List(ValidationError("error.formatting.date")))
      ))
    }
    "not allow a badly formatted life event id" in {
      val invalidJson = validJson ++ Json.obj("originalLifeEventId" -> "x")

      invalidJson.validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalLifeEventId"), List(ValidationError("error.formatting.lifeEventId")))
      ))
    }
    "require all fields" in {
      Json.obj().validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalLifeEventId"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "originalEventDate"), List(ValidationError("error.path.missing")))
      ))
    }

  }

  "AnnualReturn" must {

    val validJson = Json.obj(
      "eventDate" -> "2018-04-05",
      "isaManagerName" -> "ISA Manager",
      "taxYear" -> 2018,
      "marketValueCash" -> 0,
      "marketValueStocksAndShares" -> 55,
      "annualSubsCash" -> 0,
      "annualSubsStocksAndShares" -> 55
    )

    "serialize from json" in {
      val expected = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        isaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )
      val actual = validJson.as[AnnualReturn]

      actual mustBe expected
    }
    "deserialize to json" in {
      val input = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        isaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      Json.toJson[AnnualReturn](input) mustBe validJson
    }
    "not allow a future event date" in {
      val invalidJson = validJson ++ Json.obj("eventDate" -> "3018-05-01")

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "eventDate"), List(ValidationError("error.formatting.date")))
      ))
    }
    "not allow a badly formatted event date" in {
      val invalidJson = validJson ++ Json.obj("eventDate" -> "30-12-2017")

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "eventDate"), List(ValidationError("error.formatting.date")))
      ))
    }
    "require all fields except supersede" in {
      val expectedErrors = List(
        ((JsPath \ "eventDate"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "isaManagerName"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "taxYear"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "marketValueCash"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "marketValueStocksAndShares"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "annualSubsCash"), List(ValidationError("error.path.missing"))),
        ((JsPath \ "annualSubsStocksAndShares"), List(ValidationError("error.path.missing")))
      )

      val result = Json.obj().validate[AnnualReturn]

      result.fold(
        errors => {
          expectedErrors.foreach( errors must contain(_) )
          errors.length mustBe expectedErrors.length
        },
        _ => fail("Invalid json passed validation")
      )
    }

  }

}
