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
import uk.gov.hmrc.lisaapi.models.{AccountTransfer, CreateLisaAccountRequest}

class CreateLisaAccountRequestSpec extends PlaySpec with JsonFormats {

  val validAccountTransferJson = """{"transferredFromAccountID":"Z543210", "transferredFromLMRN":"Z543333", "transferInDate":"2015-12-13"}"""

  val validAccountTransferRequest =
    s"""{
       |"investorID": "9876543210",
       |"lisaManagerReferenceNumber": "Z4321",
       |"accountID": "8765432100",
       |"creationReason": "Transferred",
       |"firstSubscriptionDate": "2011-03-23",
       |"transferAccount": $validAccountTransferJson
       |}""".stripMargin

  val validAccountCreationRequest =
    s"""{
       |"investorID": "9876543210",
       |"lisaManagerReferenceNumber": "Z4321",
       |"accountID": "8765432100",
       |"creationReason": "New",
       |"firstSubscriptionDate": "2011-03-23"
       |}""".stripMargin

  "CreateLisaAccountRequest" must {

    "serialize transfer request from json" in {
      val res = Json.parse(validAccountTransferRequest).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.investorID mustBe "9876543210"
          data.lisaManagerReferenceNumber mustBe "Z4321"
          data.accountID mustBe "8765432100"
          data.creationReason mustBe "Transferred"
          data.firstSubscriptionDate.getYear mustBe 2011
          data.firstSubscriptionDate.getMonthOfYear mustBe 3
          data.firstSubscriptionDate.getDayOfMonth mustBe 23
          data.transferAccount mustBe Some(AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13")))
        }
      }
    }

    "serialize creation request from json" in {
      val res = Json.parse(validAccountCreationRequest).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data.investorID mustBe "9876543210"
          data.lisaManagerReferenceNumber mustBe "Z4321"
          data.accountID mustBe "8765432100"
          data.creationReason mustBe "New"
          data.firstSubscriptionDate.getYear mustBe 2011
          data.firstSubscriptionDate.getMonthOfYear mustBe 3
          data.firstSubscriptionDate.getDayOfMonth mustBe 23
          data.transferAccount mustBe None
        }
      }
    }

    "deserialize transfer request to json" in {
      val request = CreateLisaAccountRequest(
        investorID = "9876543210",
        lisaManagerReferenceNumber = "Z4321",
        accountID = "8765432100",
        creationReason = "Transferred",
        firstSubscriptionDate = new DateTime("2011-03-23"),
        transferAccount = Some(AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13")))
      )

      val json = Json.toJson[CreateLisaAccountRequest](request)

      json mustBe Json.parse(validAccountTransferRequest)
    }

    "deserialize creation request to json" in {
      val request = CreateLisaAccountRequest(
        investorID = "9876543210",
        lisaManagerReferenceNumber = "Z4321",
        accountID = "8765432100",
        creationReason = "New",
        firstSubscriptionDate = new DateTime("2011-03-23")
      )

      val json = Json.toJson[CreateLisaAccountRequest](request)

      json mustBe Json.parse(validAccountCreationRequest)
    }

    "catch an invalid lisaManagerReferenceNumber" in {
      val req = validAccountTransferRequest.replace("Z4321", "A123")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/lisaManagerReferenceNumber" && errors.contains(ValidationError("error.formatting.lmrn"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid firstSubscriptionDate" in {
      val req = validAccountTransferRequest.replace("2011-03-23", "2011")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/firstSubscriptionDate" && errors.contains(ValidationError("error.formatting.date"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid investorID" in {
      val req = validAccountTransferRequest.replace("9876543210", "2011")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/investorID" && errors.contains(ValidationError("error.formatting.investorID"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid creationReason" in {
      val req = validAccountTransferRequest.replace("Transferred", "transferred")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/creationReason" && errors.contains(ValidationError("error.formatting.creationReason"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch a transfer request without the transfer data" in {
      val req = validAccountCreationRequest.replace("New", "Transferred")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/transferAccount" && errors.contains(ValidationError("error.path.missing"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

  }

}
