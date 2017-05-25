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
import play.api.libs.json._

class JsonFormatsSpec extends PlaySpec {

  val monetaryField = "monetaryValue"
  val invalidError = "error.invalid"

  implicit val testReads: Reads[TestObject] = (JsPath \ monetaryField).read(monetaryReads()).map(TestObject.apply)

  /***
    * Method *should* read the number sent in the Json request and ensure it's *always* to
    * 2 decimal places and is *never* a negative value - otherwise it should return an error
    */
  def monetaryReads(): Reads[Float] = {
    val isTwoDp = (value:Float) => {value.toString.split("\\.")(1).length == 2}
    val isNotNegative = (value:Float) => {value >= 0f}

    Reads.verifying[Float]((f) => isTwoDp(f) && isNotNegative(f))
  }
  /* ****** */

  "Monetary reads" must {

    "pass validation" when {

      "given 0.00" in {
        val res = createJson("0.00").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 0f
          case _ => fail("failed validation")
        }
      }

      "given a 2dp positive number" in {
        val res = createJson("2.99").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 2.99f
          case _ => fail("failed validation")
        }
      }

      "given a 2dp positive number ending in a zero" in {
        val res = createJson("2.50").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 2.5f
          case _ => fail("failed validation")
        }
      }

      "given a 2dp positive number ending in two zeros" in {
        val res = createJson("2.00").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 2f
          case _ => fail("failed validation")
        }
      }

    }

    "fail validation" when {

      "given a positive number ending .0" in {
        val res = createJson("2.0").validate[TestObject]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a positive number ending .000" in {
        val res = createJson("2.000").validate[TestObject]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }
      }

      "given a negative value" in {
        val res = createJson("-5.00").validate[TestObject]

        res match {
          case JsError(errors) => {
            errors mustBe Seq((JsPath \ monetaryField, Seq(ValidationError(invalidError))))
          }
          case _ => fail("passed validation")
        }
      }

    }

  }

  private def createJson(monetaryValue:String): JsValue = {
    Json.parse(s"""{"$monetaryField" : $monetaryValue}""")
  }

  case class TestObject(monetaryValue: Float)

}
