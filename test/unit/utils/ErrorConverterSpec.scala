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

package unit.utils

import org.joda.time.DateTime
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsSuccess, Json, OFormat}
import uk.gov.hmrc.lisaapi.controllers.{ErrorValidation, JsonFormats}
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.utils.ErrorConverter


case class SimpleClass(str: String, num: Int)
case class MultipleDataTypes(str: String, num: Int, arr: List[SimpleClass], obj: SimpleClass)

class ErrorConverterSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
with JsonFormats {

  implicit val simpleFormats: OFormat[SimpleClass] = Json.format[SimpleClass]
  implicit val multipleFormats: OFormat[MultipleDataTypes] = Json.format[MultipleDataTypes]

  "Error Converter" must {
    "catch invalid data types" in {
      val validate = Json.parse("""{"str": 1, "num": "text", "arr": {}, "obj": 123}""").validate[MultipleDataTypes]

      validate match {
        case e: JsError => {
          val res = SUT.convert(e.errors)

          println(JsError.toFlatForm(e))

          res.size mustBe 5

          res must contain(ErrorValidation("INVALID_DATA_TYPE", "An invalid data type has been used", Some("/str")))
          res must contain(ErrorValidation("INVALID_DATA_TYPE", "An invalid data type has been used", Some("/num")))
          res must contain(ErrorValidation("INVALID_DATA_TYPE", "An invalid data type has been used", Some("/arr")))
          res must contain(ErrorValidation("MISSING_FIELD", "A required field is missing", Some("/obj/str")))
          res must contain(ErrorValidation("MISSING_FIELD", "A required field is missing", Some("/obj/num")))

        }
      }
    }

    "catch invalid date" in {
      val investorJson = """{
                         "investorNINO" : "AB123456D",
                         "firstName" : "Ex first Name",
                         "lastName" : "Ample",
                         "dateOfBirth" : "1973-13-24"
                       }""".stripMargin

      val validate = Json.parse(investorJson).validate[CreateLisaInvestorRequest]


      validate match {
        case e: JsError => {
          val res = SUT.convert(e.errors)
          res.size mustBe 1
          res must contain(ErrorValidation("INVALID_DATE", "A date is invalid", Some("/dateOfBirth")))
        }

      }
    }
  }

  val SUT = ErrorConverter

}