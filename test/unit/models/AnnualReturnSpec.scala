/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, Json, JsonValidationError}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models.{AnnualReturn, AnnualReturnSupersede, AnnualReturnValidator}
import uk.gov.hmrc.lisaapi.services.CurrentDateService

class AnnualReturnSpec extends PlaySpec with LisaConstants with MockitoSugar with BeforeAndAfter {

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
        ((JsPath \ "originalEventDate"), List(JsonValidationError("error.formatting.date")))
      ))
    }
    "not allow a badly formatted event date" in {
      val invalidJson = validJson ++ Json.obj("originalEventDate" -> "30-12-2017")

      invalidJson.validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalEventDate"), List(JsonValidationError("error.formatting.date")))
      ))
    }
    "not allow a badly formatted life event id" in {
      val invalidJson = validJson ++ Json.obj("originalLifeEventId" -> "x")

      invalidJson.validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalLifeEventId"), List(JsonValidationError("error.formatting.lifeEventId")))
      ))
    }
    "require all fields" in {
      Json.obj().validate[AnnualReturnSupersede] mustBe JsError(errors = List(
        ((JsPath \ "originalLifeEventId"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "originalEventDate"), List(JsonValidationError("error.path.missing")))
      ))
    }

  }

  "AnnualReturn" must {

    val validJson = Json.obj(
      "eventDate" -> "2018-04-05",
      "lisaManagerName" -> "ISA Manager",
      "taxYear" -> 2018,
      "marketValueCash" -> 0,
      "marketValueStocksAndShares" -> 55,
      "annualSubsCash" -> 0,
      "annualSubsStocksAndShares" -> 55
    )

    "serialize from json" in {
      val expected = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
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
        lisaManagerName = "ISA Manager",
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
        ((JsPath \ "eventDate"), List(JsonValidationError("error.formatting.date")))
      ))
    }
    "not allow a badly formatted event date" in {
      val invalidJson = validJson ++ Json.obj("eventDate" -> "30-12-2017")

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "eventDate"), List(JsonValidationError("error.formatting.date")))
      ))
    }
    "ensure the lisaManagerName is less than 50 characters" in {
      val tooLong = "123456789012345678901234567890123456789012345678901"
      val invalidJson = validJson ++ Json.obj("lisaManagerName" -> tooLong)

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "lisaManagerName"), List(JsonValidationError("error.formatting.lisaManagerName")))
      ))
    }
    "ensure the lisaManagerName doesn't have unexpected characters" in {
      val invalidJson = validJson ++ Json.obj("lisaManagerName" -> "?")

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "lisaManagerName"), List(JsonValidationError("error.formatting.lisaManagerName")))
      ))
    }
    "ensure the lisaManagerName isn't empty" in {
      val invalidJson = validJson ++ Json.obj("lisaManagerName" -> "")

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "lisaManagerName"), List(JsonValidationError("error.formatting.lisaManagerName")))
      ))
    }
    "ensure the taxYear is four figures - it must be less than 10000" in {
      val invalidJson = validJson ++ Json.obj("taxYear" -> 10000)

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "taxYear"), List(JsonValidationError("error.formatting.taxYear")))
      ))
    }
    "ensure the taxYear is four figures - it must be more than 999" in {
      val invalidJson = validJson ++ Json.obj("taxYear" -> 999)

      invalidJson.validate[AnnualReturn] mustBe JsError(errors = List(
        ((JsPath \ "taxYear"), List(JsonValidationError("error.formatting.taxYear")))
      ))
    }
    "ensure all numeric fields are integers" in {
      val invalidJson = validJson ++ Json.obj(
        "taxYear" -> 2018.5,
        "marketValueCash" -> 1.5,
        "marketValueStocksAndShares" -> 1.5,
        "annualSubsCash" -> 1.5,
        "annualSubsStocksAndShares" -> 1.5
      )

      val expectedErrors = List(
        ((JsPath \ "taxYear"), List(JsonValidationError("error.expected.int"))),
        ((JsPath \ "marketValueCash"), List(JsonValidationError("error.expected.int"))),
        ((JsPath \ "marketValueStocksAndShares"), List(JsonValidationError("error.expected.int"))),
        ((JsPath \ "annualSubsCash"), List(JsonValidationError("error.expected.int"))),
        ((JsPath \ "annualSubsStocksAndShares"), List(JsonValidationError("error.expected.int")))
      )

      val result = invalidJson.validate[AnnualReturn]

      result.fold(
        errors => {
          expectedErrors.foreach( errors must contain(_) )
          errors.length mustBe expectedErrors.length
        },
        _ => fail("Invalid json passed validation")
      )
    }
    "ensure monetary figures are not negative" in {
      val invalidJson = validJson ++ Json.obj(
        "marketValueCash" -> -1,
        "marketValueStocksAndShares" -> -1,
        "annualSubsCash" -> -1,
        "annualSubsStocksAndShares" -> -1
      )

      val expectedErrors = List(
        ((JsPath \ "marketValueCash"), List(JsonValidationError("error.formatting.annualFigures"))),
        ((JsPath \ "marketValueStocksAndShares"), List(JsonValidationError("error.formatting.annualFigures"))),
        ((JsPath \ "annualSubsCash"), List(JsonValidationError("error.formatting.annualFigures"))),
        ((JsPath \ "annualSubsStocksAndShares"), List(JsonValidationError("error.formatting.annualFigures")))
      )

      val result = invalidJson.validate[AnnualReturn]

      result.fold(
        errors => {
          expectedErrors.foreach( errors must contain(_) )
          errors.length mustBe expectedErrors.length
        },
        _ => fail("Invalid json passed validation")
      )
    }
    "require all fields except supersede" in {
      val expectedErrors = List(
        ((JsPath \ "eventDate"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "lisaManagerName"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "taxYear"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "marketValueCash"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "marketValueStocksAndShares"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "annualSubsCash"), List(JsonValidationError("error.path.missing"))),
        ((JsPath \ "annualSubsStocksAndShares"), List(JsonValidationError("error.path.missing")))
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

  "AnnualReturnValidator" must {

    val mockDateService: CurrentDateService = mock[CurrentDateService]
    val cashAndStocksErrorMessage = "You can only give cash or stocks and shares values"

    before {
      reset(mockDateService)
      when(mockDateService.now()).thenReturn(new DateTime("2018-04-06")) // first day of 2019 tax year
    }

    "return no errors for a valid return" in {
      val req = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      SUT.validate(req) mustBe Nil
    }
    "return an error for a taxYear before the start of lisa" in {
      val req = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2016,
        marketValueCash = 0,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      SUT.validate(req) mustBe List(ErrorValidation(DATE_ERROR, "The taxYear cannot be before 2017", Some("/taxYear")))
    }
    "return an error if the taxYear is the current tax year" in {
      when(mockDateService.now()).thenReturn(new DateTime("2018-04-05")) // final day of the 2018 tax year

      val req = AnnualReturn(
        eventDate = new DateTime("2018-12-10"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      SUT.validate(req) mustBe List(ErrorValidation(DATE_ERROR, "The taxYear must be a previous tax year", Some("/taxYear")))
    }
    "return an error for a taxYear after the current year" in {
      when(mockDateService.now()).thenReturn(new DateTime("2018-04-05")) // final day of the 2018 tax year

      val req = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2019,
        marketValueCash = 0,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      SUT.validate(req) mustBe List(ErrorValidation(DATE_ERROR, "The taxYear cannot be in the future", Some("/taxYear")))
    }
    "return an error if marketValueCash and marketValueStocksAndShares are specified" in {
      val req = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 55,
        marketValueStocksAndShares = 55,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 0
      )

      SUT.validate(req) mustBe List(
        ErrorValidation(MONETARY_ERROR, cashAndStocksErrorMessage, Some("/marketValueCash")),
        ErrorValidation(MONETARY_ERROR, cashAndStocksErrorMessage, Some("/marketValueStocksAndShares"))
      )
    }
    "return an error if annualSubsCash and annualSubsStocksAndShares are specified" in {
      val req = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 0,
        marketValueStocksAndShares = 0,
        annualSubsCash = 55,
        annualSubsStocksAndShares = 55
      )

      SUT.validate(req) mustBe List(
        ErrorValidation(MONETARY_ERROR, cashAndStocksErrorMessage, Some("/annualSubsCash")),
        ErrorValidation(MONETARY_ERROR, cashAndStocksErrorMessage, Some("/annualSubsStocksAndShares"))
      )
    }
    "return an error if a mix of cash and stocks and shares are specified" in {
      val req = AnnualReturn(
        eventDate = new DateTime("2018-04-05"),
        lisaManagerName = "ISA Manager",
        taxYear = 2018,
        marketValueCash = 55,
        marketValueStocksAndShares = 0,
        annualSubsCash = 0,
        annualSubsStocksAndShares = 55
      )

      SUT.validate(req) mustBe List(
        ErrorValidation(MONETARY_ERROR, cashAndStocksErrorMessage, Some("/marketValueCash")),
        ErrorValidation(MONETARY_ERROR, cashAndStocksErrorMessage, Some("/annualSubsStocksAndShares"))
      )
    }

    object SUT extends AnnualReturnValidator {
      override val currentDateService: CurrentDateService = mockDateService
    }

  }

}