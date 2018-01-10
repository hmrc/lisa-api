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

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsError, JsPath, JsSuccess, Json}
import uk.gov.hmrc.lisaapi.models.AccountTransfer

class AccountTransferSpec extends PlaySpec {

  val validAccountTransferJson = """{"transferredFromAccountId":"Z543210", "transferredFromLMRN":"Z543333", "transferInDate":"2015-12-13"}"""

  "AccountTransfer" must {

    "serialize from json" in {
      val res = Json.parse(validAccountTransferJson).validate[AccountTransfer]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.transferredFromAccountId mustBe "Z543210"
          data.transferredFromLMRN mustBe "Z543333"
          data.transferInDate.getYear mustBe 2015
          data.transferInDate.getMonthOfYear mustBe 12
          data.transferInDate.getDayOfMonth mustBe 13
        }
      }
    }

    "deserialize to json" in {
      val request = AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))

      val json = Json.toJson[AccountTransfer](request)

      json mustBe Json.parse(validAccountTransferJson.replace("Id", "ID"))
    }

    "catch an invalid transferredFromLMRN" in {
      val req = """{"transferredFromAccountId":"Z543210", "transferredFromLMRN":"A12345", "transferInDate":"2015-12-13"}"""
      val res = Json.parse(req).validate[AccountTransfer]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/transferredFromLMRN" && errors.contains(ValidationError("error.formatting.lmrn"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid transferInDate" in {
      val req = """{"transferredFromAccountId":"Z543210", "transferredFromLMRN":"Z543333", "transferInDate":"12/13/2015"}"""
      val res = Json.parse(req).validate[AccountTransfer]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/transferInDate" && errors.contains(ValidationError("error.formatting.date"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

  }

}
