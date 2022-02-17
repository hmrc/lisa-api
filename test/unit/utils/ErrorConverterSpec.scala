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

package unit.utils

import helpers.BaseTestFixture
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.libs.json.{JsError, Json, OFormat}
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.utils.ErrorConverter

case class SimpleClass(str: String, num: Int)

case class MultipleDataTypes(str: String, num: Int, arr: List[SimpleClass], obj: SimpleClass)

class ErrorConverterSpec extends BaseTestFixture with GuiceOneAppPerSuite {

  implicit val simpleFormats: OFormat[SimpleClass] = Json.format[SimpleClass]
  implicit val multipleFormats: OFormat[MultipleDataTypes] = Json.format[MultipleDataTypes]

  val errorConverter: ErrorConverter.type = ErrorConverter

  "Error Converter" must {
    "catch invalid data types" in {
      val validate = Json.parse("""{"str": 1, "num": "text", "arr": {}, "obj": 123}""").validate[MultipleDataTypes]

      assert(validate.isInstanceOf[JsError])
      val e = validate.asInstanceOf[JsError]
      val res = errorConverter.convert(e.errors)

      println(JsError.toFlatForm(e))

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
      val e = validate.asInstanceOf[JsError]
      val res = errorConverter.convert(e.errors)
      res.size mustBe 1
      res must contain(ErrorValidation("INVALID_DATE", "Date is invalid", Some("/dateOfBirth")))


    }
  }
}
