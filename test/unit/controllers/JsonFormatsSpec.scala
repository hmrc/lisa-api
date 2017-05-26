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
import uk.gov.hmrc.lisaapi.controllers.JsonFormats

class JsonFormatsSpec extends PlaySpec {

  val monetaryField = "monetaryValue"
  val invalidError = "error.invalid"

  implicit val testReads: Reads[TestObject] = (JsPath \ monetaryField).read(SUT.monetaryReads()).map(TestObject.apply)

  "Monetary reads" must {

    "pass validation" when {

      "given a value of zero" in {
        val res = createJson("0.00").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 0f
          case _ => fail("failed validation")
        }
      }

      "given a 1dp number" in {
        val res = createJson("1.5").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 1.5f
          case _ => fail("failed validation")
        }
      }

      "given a 2dp number" in {
        val res = createJson("2.99").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 2.99f
          case _ => fail("failed validation")
        }
      }

      "given a Xdp number - if it has no more than 2 significant figures" in {
        val res = createJson("2.99000").validate[TestObject]

        res match {
          case JsSuccess(data, _) => data.monetaryValue mustBe 2.99f
          case _ => fail("failed validation")
        }
      }

    }

    "fail validation" when {

      "given a number with 3 significant figures" in {
        val res = createJson("2.005").validate[TestObject]

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

  object SUT extends JsonFormats {}

}
