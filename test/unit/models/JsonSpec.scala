/*
 * Copyright 2022 HM Revenue & Customs
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
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models._

class JsonSpec extends PlaySpec {

  val property = "property"
  val currencyFormatError = "error.formatting.currencyNegativeDisallowed"

  implicit val testMonetaryReads: Reads[TestMonetaryClass] = (JsPath \ property).read[Amount](JsonReads.nonNegativeAmount).map(TestMonetaryClass.apply)
  implicit val testMonetaryWrites: Writes[TestMonetaryClass] = (JsPath \ property).write[Amount].contramap[TestMonetaryClass](_.property)

  implicit val testDateReads: Reads[TestDateClass] = (JsPath \ property).read[DateTime](JsonReads.isoDate).map(TestDateClass.apply)

  "Monetary reads" must {

    "pass validation" when {

      "given a value of zero" in {
        val res = createJson("0.00").validate[TestMonetaryClass]

        res match {
          case JsSuccess(data, _) => data.property mustBe 0d
          case _ => fail("failed validation")
        }
      }

      "given a whole number" in {
        val res = createJson("12").validate[TestMonetaryClass]

        res match {
          case JsSuccess(data, _) => data.property mustBe 12d
          case _ => fail("failed validation")
        }
      }

      "given a 1dp number" in {
        val res = createJson("100.5").validate[TestMonetaryClass]

        res match {
          case JsSuccess(data, _) => data.property mustBe 100.5d
          case _ => fail("failed validation")
        }
      }

      "given a 2dp number" in {
        val res = createJson("2.99").validate[TestMonetaryClass]

        res match {
          case JsSuccess(data, _) => data.property mustBe 2.99d
          case _ => fail("failed validation")
        }
      }

      "given a large 2dp number" in {
        val res = createJson("1000000000.01").validate[TestMonetaryClass]

        res match {
          case JsSuccess(data, _) => data.property mustBe 1000000000.01d
          case _ => fail("failed validation")
        }
      }

    }

    "fail validation" when {

      "given a 3dp number" in {
        val res = createJson("2.000").validate[TestMonetaryClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError(currencyFormatError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a 100dp number" in {
        val res = createJson("1.1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890").validate[TestMonetaryClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError(currencyFormatError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a large 3dp number" in {
        val res = createJson("100000000000000000000000000000000000.001").validate[TestMonetaryClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError(currencyFormatError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a negative value" in {
        val res = createJson("-0.01").validate[TestMonetaryClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError(currencyFormatError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a non-numeric value" in {
        val res = createJson(""""x"""").validate[TestMonetaryClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError("error.expected.jsnumber"))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a numeric string" in {
        val res = createJson(""""5"""").validate[TestMonetaryClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError("error.expected.jsnumber"))))
          }
          case _ => fail("passed validation")
        }
      }

    }

  }

  "Monetary writes" must {

    "write to a whole number" when {

      "given a 2dp value with 2 trailing zeros" in {
        val test = TestMonetaryClass(BigDecimal(0.00))
        val res = Json.toJson[TestMonetaryClass](test).toString()

        res mustBe createJsonString("0")
      }

    }

    "write to 1dp" when {

      "given a 2dp value with 1 trailing zeros" in {
        val test = TestMonetaryClass(BigDecimal(1.50))
        val res = Json.toJson[TestMonetaryClass](test).toString()

        res mustBe createJsonString("1.5")
      }

    }

    "write to 2dp" when {

      "given a 2dp value" in {
        val test = TestMonetaryClass(BigDecimal("2.99"))
        val res = Json.toJson[TestMonetaryClass](test).toString()

        res mustBe createJsonString("2.99")
      }

    }

  }

  "Date reads" must {

    "return an invalid data type response" when {

      "given an invalid data type" in {
        val res = createJson("true").validate[TestDateClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError("error.expected.jsstring"))))
          }
          case _ => fail("passed validation")
        }
      }

    }

    "return an bad formatting error response" when {

      "given a random string" in {
        val res = createJson("\"true\"").validate[TestDateClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError("error.formatting.date"))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a yy-MM-dd string" in {
        val res = createJson("\"90-01-20\"").validate[TestDateClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError("error.formatting.date"))))
          }
          case _ => fail("passed validation")
        }
      }

      "given an invalid date" in {
        val res = createJson("\"2000-02-30\"").validate[TestDateClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ property, Seq(JsonValidationError("error.formatting.date"))))
          }
          case _ => fail("passed validation")
        }
      }

    }

  }

  private def createJson(value:String): JsValue = {
    Json.parse(createJsonString(value))
  }

  private def createJsonString(value:String): String = {
    s"""{"$property":$value}"""
  }

  case class TestMonetaryClass(property: Amount)
  case class TestDateClass(property: DateTime)

  object SUT {}

}
