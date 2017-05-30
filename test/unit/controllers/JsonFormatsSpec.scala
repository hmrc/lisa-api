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

package unit.controllers

import org.scalatestplus.play.PlaySpec
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.controllers.JsonFormats
import uk.gov.hmrc.lisaapi.models._

class JsonFormatsSpec extends PlaySpec {

  val monetaryField = "monetaryValue"
  val invalidError = "INVALID_DATE"

  implicit val testReads: Reads[TestClass] = (JsPath \ monetaryField).read[Amount](nonNegativeAmountValidator).map(TestClass.apply)
  implicit val testWrites: Writes[TestClass] = (JsPath \ monetaryField).write[Amount].contramap[TestClass](_.monetaryValue)

  "Monetary reads" must {

    "pass validation" when {

      "given a value of zero" in {
        val res = createJson("0.00").validate[TestClass]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 0d
          case _ => fail("failed validation")
        }
      }

      "given a whole number" in {
        val res = createJson("12").validate[TestClass]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 12d
          case _ => fail("failed validation")
        }
      }

      "given a 1dp number" in {
        val res = createJson("100.5").validate[TestClass]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 100.5d
          case _ => fail("failed validation")
        }
      }

      "given a 2dp number" in {
        val res = createJson("2.99").validate[TestClass]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 2.99d
          case _ => fail("failed validation")
        }
      }

      "given a large 2dp number" in {
        val res = createJson("1000000000.01").validate[TestClass]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 1000000000.01d
          case _ => fail("failed validation")
        }
      }

    }

    "fail validation" when {

      "given a 3dp number" in {
        val res = createJson("2.005").validate[TestClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a 100dp number" in {
        val res = createJson("1.1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890").validate[TestClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }

      }

      "given a large 3dp number" in {
        val res = createJson("100000000000000000000000000000000000.001").validate[TestClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a negative value" in {
        val res = createJson("-0.01").validate[TestClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a non-numeric value" in {
        val res = createJson(""""x"""").validate[TestClass]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError("error.expected.numberformatexception"))))
          }
          case _ => fail("passed validation")
        }
      }

    }

  }

  "Monetary writes" must {

    "write to 2dp" when {

      "given a 2dp value" in {
        val test = TestClass(BigDecimal("2.99"))
        val res = Json.toJson[TestClass](test).toString()

        res mustBe createJsonString("2.99")
      }

      // doesn't appear possible in play without diving deep into the underlying json library (jackson)
      // see also: https://github.com/playframework/playframework/issues/5423
      "given a 2dp value with trailing zeros" ignore {
        val test = TestClass(BigDecimal(0.00))
        val res = Json.toJson[TestClass](test).toString()

        res mustBe createJsonString("0.00")
      }

    }

  }

  private def createJson(monetaryValue:String): JsValue = {
    Json.parse(createJsonString(monetaryValue))
  }

  private def createJsonString(monetaryValue:String): String = {
    s"""{"$monetaryField":$monetaryValue}"""
  }

  case class TestClass(monetaryValue: BigDecimal)

  object SUT extends JsonFormats {}

}
