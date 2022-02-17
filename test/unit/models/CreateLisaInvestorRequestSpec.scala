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
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest

class CreateLisaInvestorRequestSpec extends PlaySpec {

  val validRequestJson = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "dateOfBirth":"2000-02-29"}"""
  val validRequestJsonWithSpaces = """{"investorNINO":"AB123456B", "firstName":"  A      ", "lastName":" C    ", "dateOfBirth":"2000-02-29"}"""

  "CreateLisaInvestorRequest" must {

    "serialize from json" in {
      val res = Json.parse(validRequestJson).validate[CreateLisaInvestorRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.investorNINO mustBe "AB123456A"
          data.firstName mustBe "A"
          data.lastName mustBe "B"
          data.dateOfBirth.getYear mustBe 2000
          data.dateOfBirth.getMonthOfYear mustBe 2
          data.dateOfBirth.getDayOfMonth mustBe 29
        }
      }
    }

    "serialize from json with spaces" in {
      val res = Json.parse(validRequestJsonWithSpaces).validate[CreateLisaInvestorRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.investorNINO mustBe "AB123456B"
          data.firstName mustBe "A"
          data.lastName mustBe "C"
          data.dateOfBirth.getYear mustBe 2000
          data.dateOfBirth.getMonthOfYear mustBe 2
          data.dateOfBirth.getDayOfMonth mustBe 29
        }
      }
    }

    "deserialize to json" in {
      val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-02-29"))

      val json = Json.toJson[CreateLisaInvestorRequest](request)

      json mustBe Json.parse(validRequestJson)
    }

    "catch an invalid NINO" in {
      hasCorrectValidationError(validRequestJson.replace("AB123456A", "123"), "/investorNINO", "error.formatting.nino")
    }

    "catch an invalid firstName" in {
      hasCorrectValidationError(validRequestJson.replace("A", ""), "/firstName", "error.formatting.name")
    }

    "catch an invalid lastName" in {
      hasCorrectValidationError(validRequestJson.replace("B", ""), "/lastName", "error.formatting.name")
    }

    "catch an invalid dateOfBirth" when {

      "the data type is incorrect" in {
        hasCorrectValidationError(validRequestJson.replace("\"2000-02-29\"", "123456789"), "/dateOfBirth", "error.expected.jsstring")
      }

      "the format is incorrect" in {
        hasCorrectValidationError(validRequestJson.replace("2000-02-29", "01/01/2000"), "/dateOfBirth", "error.formatting.date")
      }

      "day and month are in the wrong order" in {
        hasCorrectValidationError(validRequestJson.replace("2000-02-29", "2000-31-01"), "/dateOfBirth", "error.formatting.date")
      }

      "an invalid date is supplied" in {
        hasCorrectValidationError(validRequestJson.replace("2000-02-29", "2000-09-31"), "/dateOfBirth", "error.formatting.date")
      }

      "feb 29th is supplied for a non-leap year" in {
        hasCorrectValidationError(validRequestJson.replace("2000-02-29", "2017-02-29"), "/dateOfBirth", "error.formatting.date")
      }

      "the date is in the future" in {
        val futureDate = DateTime.now().plusDays(1).toString("yyyy-MM-dd")

        hasCorrectValidationError(validRequestJson.replace("2000-02-29", futureDate), "/dateOfBirth", "error.formatting.date")
      }

    }

  }

  private def hasCorrectValidationError(req: String, path: String, errorMessage: String): Unit = {
    val res = Json.parse(req).validate[CreateLisaInvestorRequest]

    res match {
      case JsError(errors) => {
        errors.count {
          case (path: JsPath, errors: Seq[JsonValidationError]) => {
            path.eq(path) && errors.contains(JsonValidationError(errorMessage))
          }
        } mustBe 1
      }
      case _ => fail()
    }
  }

}
