/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.utils

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.*
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.utils.ErrorConverter

case class SimpleClass(str: String, num: Int)

case class MultipleDataTypes(str: String, num: Int, arr: List[SimpleClass], obj: SimpleClass)

class ErrorConverterSpec extends BaseTestFixture with GuiceOneAppPerSuite {

  implicit val simpleFormats: OFormat[SimpleClass]         = Json.format[SimpleClass]
  implicit val multipleFormats: OFormat[MultipleDataTypes] = Json.format[MultipleDataTypes]

  val errorConverter: ErrorConverter.type = ErrorConverter

  "Error Converter" must {
    "catch invalid data types" in {
      val validate = Json.parse("""{"str": 1, "num": "text", "arr": {}, "obj": 123}""").validate[MultipleDataTypes]

      assert(validate.isInstanceOf[JsError])
      val e   = validate.asInstanceOf[JsError]
      val res = errorConverter.convert(e.errors)

      res.size mustBe 4

      res must contain(ErrorValidation("INVALID_DATA_TYPE", "Invalid data type has been used", Some("/str")))
      res must contain(ErrorValidation("INVALID_DATA_TYPE", "Invalid data type has been used", Some("/num")))
      res must contain(ErrorValidation("INVALID_DATA_TYPE", "Invalid data type has been used", Some("/arr")))
      res must contain(ErrorValidation("INVALID_DATA_TYPE", "Invalid data type has been used", Some("/obj")))

    }

    "catch invalid date" in {
      val investorJson =
        """{
                         "investorNINO" : "AB123456D",
                         "firstName" : "Ex first Name",
                         "lastName" : "Ample",
                         "dateOfBirth" : "1973-13-24"
                       }""".stripMargin

      val validate = Json.parse(investorJson).validate[CreateLisaInvestorRequest]

      assert(validate.isInstanceOf[JsError])
      val e   = validate.asInstanceOf[JsError]
      val res = errorConverter.convert(e.errors)
      res.size mustBe 1
      res        must contain(ErrorValidation("INVALID_DATE", "Date is invalid", Some("/dateOfBirth")))

    }

    "handle error.formatting.currencyNegativeDisallowed" in {
      val errors = List((JsPath \ "amount", Seq(JsonValidationError("error.formatting.currencyNegativeDisallowed"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_MONETARY_AMOUNT"
      result.head.message   mustBe "Amount cannot be negative, and can only have up to 2 decimal places"
    }

    "handle error.formatting.currencyNegativeAllowed" in {
      val errors = List((JsPath \ "amount", Seq(JsonValidationError("error.formatting.currencyNegativeAllowed"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_MONETARY_AMOUNT"
      result.head.message   mustBe "Amount can only have up to 2 decimal places"
    }

    "handle error.formatting.annualFigures" in {
      val errors = List((JsPath \ "amount", Seq(JsonValidationError("error.formatting.annualFigures"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_MONETARY_AMOUNT"
      result.head.message   mustBe "Amount cannot be negative"
    }

    "handle error.formatting.* generic" in {
      val errors = List((JsPath \ "field", Seq(JsonValidationError("error.formatting.something"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_FORMAT"
      result.head.message   mustBe "Invalid format has been used"
    }

    "handle emptyNameOrNumber" in {
      val errors = List((JsPath \ "nameOrNumber", Seq(JsonValidationError("emptyNameOrNumber"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_NAME_OR_NUMBER"
      result.head.message   mustBe "Enter nameOrNumber"
    }

    "handle tooLongNameOrNumber" in {
      val errors = List((JsPath \ "nameOrNumber", Seq(JsonValidationError("tooLongNameOrNumber"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_NAME_OR_NUMBER"
      result.head.message   mustBe "nameOrNumber must be 35 characters or less"
    }

    "handle invalidNameOrNumber" in {
      val errors = List((JsPath \ "nameOrNumber", Seq(JsonValidationError("invalidNameOrNumber"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_NAME_OR_NUMBER"
      result.head.message   mustBe "nameOrNumber must only include letters a to z, numbers 0 to 9, colons, forward slashes, hyphen and spaces"
    }

    "handle emptyPostalCode" in {
      val errors = List((JsPath \ "postalCode", Seq(JsonValidationError("emptyPostalCode"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_POSTAL_CODE"
      result.head.message   mustBe "Enter a postcode"
    }

    "handle invalidPostalCode" in {
      val errors = List((JsPath \ "postalCode", Seq(JsonValidationError("invalidPostalCode"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "INVALID_POSTAL_CODE"
      result.head.message   mustBe "Postcode must only include letters a to z and numbers 0 to 9, like AA1 1AA"
    }

    "handle error.path.missing" in {
      val errors = List((JsPath \ "field", Seq(JsonValidationError("error.path.missing"))))
      val result = errorConverter.convert(errors)

      result.size           mustBe 1
      result.head.errorCode mustBe "MISSING_FIELD"
      result.head.message   mustBe "This field is required"
    }

    "throw MatchError for unknown error type" in {
      val errors = List((JsPath \ "field", Seq(JsonValidationError("unknown.error.type"))))

      assertThrows[MatchError] {
        errorConverter.convert(errors)
      }
    }
  }

}
