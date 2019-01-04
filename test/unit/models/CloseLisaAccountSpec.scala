/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.lisaapi.models.CloseLisaAccountRequest

class CloseLisaAccountSpec extends PlaySpec {

  val validClosedRequestJson = """{"accountClosureReason":"All funds withdrawn", "closureDate":"2000-01-01"}"""
  val validCancelledRequestJson = """{"accountClosureReason":"Cancellation", "closureDate":"2000-01-01"}"""

  "CloseLisaAccountRequest" must {

    "serialize from closed requestjson" in {
      val res = Json.parse(validClosedRequestJson).validate[CloseLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.accountClosureReason mustBe "All funds withdrawn"
          data.closureDate.getYear mustBe 2000
          data.closureDate.getMonthOfYear mustBe 1
          data.closureDate.getDayOfMonth mustBe 1
        }
      }
    }

    "serialize from cancelled request json" in {
      val res = Json.parse(validCancelledRequestJson).validate[CloseLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.accountClosureReason mustBe "Cancellation"
          data.closureDate.getYear mustBe 2000
          data.closureDate.getMonthOfYear mustBe 1
          data.closureDate.getDayOfMonth mustBe 1
        }
      }
    }

    "deserialize to json" in {
      val request = CloseLisaAccountRequest("All funds withdrawn", new DateTime("2000-01-01"))

      val json = Json.toJson[CloseLisaAccountRequest](request)

      json mustBe Json.parse(validClosedRequestJson)
    }

    "catch errors" when {

      "given an empty object" in {
        val req = "{}"

        validateRequest(req) { errors =>
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              val accountClosureReasonMissing = path.toString() == "/accountClosureReason" && errors.contains(ValidationError("error.path.missing"))
              val closureDateMissing = path.toString() == "/closureDate" && errors.contains(ValidationError("error.path.missing"))

              (accountClosureReasonMissing || closureDateMissing)
            }
          } mustBe 2
        }
      }

      "given an invalid reason for closure" in {
        val req = validClosedRequestJson.replace("All funds withdrawn", "X")

        validateRequest(req) { errors =>
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/accountClosureReason" && errors.contains(ValidationError("error.formatting.accountClosureReason"))
            }
          } mustBe 1
        }
      }

      "given an invalid closure date" in {
        val req = validClosedRequestJson.replace("2000-01-01", "01/01/2000")

        validateRequest(req) { errors =>
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/closureDate" && errors.contains(ValidationError("error.formatting.date"))
            }
          } mustBe 1
        }
      }

      "given a closure date in the future" in {
        val futureDate = DateTime.now().plusDays(1).toString("yyyy-MM-dd")
        val req = validClosedRequestJson.replace("2000-01-01", futureDate)

        validateRequest(req) { errors =>
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/closureDate" && errors.contains(ValidationError("error.formatting.date"))
            }
          } mustBe 1
        }
      }

    }

  }

  private def validateRequest(req: String)(callback:(Seq[(JsPath, Seq[ValidationError])]) => Unit) = {
    val res = Json.parse(req).validate[CloseLisaAccountRequest]

    res match {
      case JsError(errors) => {
        callback(errors)
      }
      case _ => fail()
    }
  }

}
