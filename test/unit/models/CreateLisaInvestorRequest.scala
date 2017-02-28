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

package unit.models

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.controllers.JsonFormats
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest

class CreateLisaInvestorRequestSpec extends PlaySpec with JsonFormats {

  val validRequestJson = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB":"2000-02-29"}"""

  "CreateLisaInvestorRequest" must {

    "serialize from json" in {
      val res = Json.parse(validRequestJson).validate[CreateLisaInvestorRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.investorNINO mustBe "AB123456A"
          data.firstName mustBe "A"
          data.lastName mustBe "B"
          data.DoB.getYear mustBe 2000
          data.DoB.getMonthOfYear mustBe 2
          data.DoB.getDayOfMonth mustBe 29
        }
      }
    }

    "deserialize to json" in {
      val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-02-29"))

      val json = Json.toJson[CreateLisaInvestorRequest](request)

      json mustBe Json.parse(validRequestJson)
    }

    "catch an invalid NINO" in {
      val req = """{"investorNINO":"123", "firstName":"A", "lastName":"B", "DoB":"2000-01-01"}"""
      val res = Json.parse(req).validate[CreateLisaInvestorRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/investorNINO" && errors.contains(ValidationError("error.formatting.nino"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid firstName" in {
      val req = """{"investorNINO":"AB123456A", "firstName":"", "lastName":"B", "DoB":"2000-01-01"}"""
      val res = Json.parse(req).validate[CreateLisaInvestorRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/firstName" && errors.contains(ValidationError("error.formatting.firstName"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid lastName" in {
      val req = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"", "DoB":"2000-01-01"}"""
      val res = Json.parse(req).validate[CreateLisaInvestorRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/lastName" && errors.contains(ValidationError("error.formatting.lastName"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid DoB" when {

      "the date is in the future" in {
        val futureDate = new DateTime().plusDays(1).toString("yyyy-MM-dd")
        val req = s"""{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB": "${futureDate}"}"""
        val res = Json.parse(req).validate[CreateLisaInvestorRequest]

        res match {
          case JsError(errors) => {
            errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString() == "/DoB" && errors.contains(ValidationError("error.formatting.date"))
              }
            } mustBe 1
          }
          case _ => fail()
        }
      }

      "the data type is incorrect" in {
        val req = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB": 123456789}"""
        val res = Json.parse(req).validate[CreateLisaInvestorRequest]

        res match {
          case JsError(errors) => {
            errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString() == "/DoB" && errors.contains(ValidationError("error.formatting.date"))
              }
            } mustBe 1
          }
          case _ => fail()
        }
      }

      "the format is incorrect" in {
        val req = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB":"01/01/2000"}"""
        val res = Json.parse(req).validate[CreateLisaInvestorRequest]

        res match {
          case JsError(errors) => {
            errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString() == "/DoB" && errors.contains(ValidationError("error.formatting.date"))
              }
            } mustBe 1
          }
          case _ => fail()
        }
      }

      "day and month are in the wrong order" in {
        val req = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB":"2000-31-01"}"""
        val res = Json.parse(req).validate[CreateLisaInvestorRequest]

        res match {
          case JsError(errors) => {
            errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString() == "/DoB" && errors.contains(ValidationError("error.formatting.date"))
              }
            } mustBe 1
          }
          case _ => fail()
        }
      }

      "an invalid date is supplied" in {
        val req = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB":"2000-09-31"}"""
        val res = Json.parse(req).validate[CreateLisaInvestorRequest]

        res match {
          case JsError(errors) => {
            errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString() == "/DoB" && errors.contains(ValidationError("error.formatting.date"))
              }
            } mustBe 1
          }
          case _ => fail()
        }
      }

      "feb 29th is supplied for a non-leap year" in {
        val req = """{"investorNINO":"AB123456A", "firstName":"A", "lastName":"B", "DoB":"2017-02-29"}"""
        val res = Json.parse(req).validate[CreateLisaInvestorRequest]

        res match {
          case JsError(errors) => {
            errors.count {
              case (path: JsPath, errors: Seq[ValidationError]) => {
                path.toString() == "/DoB" && errors.contains(ValidationError("error.formatting.date"))
              }
            } mustBe 1
          }
          case _ => fail()
        }
      }

    }

  }

}
