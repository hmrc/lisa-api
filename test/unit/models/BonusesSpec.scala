/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.models.Bonuses

class BonusesSpec extends PlaySpec {

  val validBonusJson = """{"bonusDueForPeriod": 1000.50, "totalBonusDueYTD": 1000.50, "bonusPaidYTD": 500.50, "claimReason": "Life Event"}"""

  "Bonuses" must {

    "serialize from json" when {

      "sent a valid request with all fields" in {
        val res = Json.parse(validBonusJson).validate[Bonuses]

        res match {
          case JsError(errors) => fail()
          case JsSuccess(data, path) => {
            data.bonusDueForPeriod mustBe 1000.5f
            data.totalBonusDueYTD mustBe 1000.5f
            data.bonusPaidYTD mustBe Some(500.5f)
            data.claimReason mustBe "Life Event"
          }
        }
      }

      "sent a valid request without the optional field" in {
        val res = Json.parse(validBonusJson.replace("\"bonusPaidYTD\": 500.50,","")).validate[Bonuses]

        res match {
          case JsError(errors) => fail()
          case JsSuccess(data, path) => {
            data.bonusDueForPeriod mustBe 1000.5f
            data.totalBonusDueYTD mustBe 1000.5f
            data.bonusPaidYTD mustBe None
            data.claimReason mustBe "Life Event"
          }
        }
      }

    }

    "deserialize to json" in {
      val request = Bonuses(1000.5f, 1000.5f, Some(500.5f), "Life Event")

      val json = Json.toJson[Bonuses](request)

      json mustBe Json.parse(validBonusJson)
    }

    "catch an invalid claim reason" in {
      val req = validBonusJson.replace("Life Event", "X")
      val res = Json.parse(req).validate[Bonuses]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/claimReason" && errors.contains(ValidationError("error.formatting.claimReason"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

  }

}
