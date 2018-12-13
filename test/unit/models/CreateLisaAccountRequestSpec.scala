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
import uk.gov.hmrc.lisaapi.models.{AccountTransfer, CreateLisaAccountCreationRequest, CreateLisaAccountRequest, CreateLisaAccountTransferRequest}

class CreateLisaAccountRequestSpec extends PlaySpec {

  val validAccountTransferJson = """{"transferredFromAccountId":"Z543210", "transferredFromLMRN":"Z543333", "transferInDate":"2015-12-13"}"""

  val validAccountTransferRequest =
    s"""{
       |"investorId": "9876543210",
       |"accountId": "8765432100",
       |"creationReason": "Transferred",
       |"firstSubscriptionDate": "2011-03-23",
       |"transferAccount": $validAccountTransferJson
       |}""".stripMargin

  val validAccountCreationRequest =
    s"""{
       |"investorId": "9876543210",
       |"accountId": "8765432100",
       |"creationReason": "New",
       |"firstSubscriptionDate": "2011-03-23"
       |}""".stripMargin

  "CreateLisaAccountRequest" must {

    "serialize transfer request from json" in {
      val res = Json.parse(validAccountTransferRequest).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data match {
            case req: CreateLisaAccountTransferRequest => {
              req.creationReason mustBe "Transferred"
              req.investorId mustBe "9876543210"
              req.accountId mustBe "8765432100"
              req.firstSubscriptionDate.getYear mustBe 2011
              req.firstSubscriptionDate.getMonthOfYear mustBe 3
              req.firstSubscriptionDate.getDayOfMonth mustBe 23
              req.transferAccount mustBe AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))
            }
            case _ => fail()
          }
        }
      }
    }

    "serialize transfer current year funds request from json" in {
      val res = Json.
                  parse(validAccountTransferRequest.replace("Transferred", "Current year funds transferred")).
                  validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data match {
            case req: CreateLisaAccountTransferRequest => {
              req.creationReason mustBe "Current year funds transferred"
              req.investorId mustBe "9876543210"
              req.accountId mustBe "8765432100"
              req.firstSubscriptionDate.getYear mustBe 2011
              req.firstSubscriptionDate.getMonthOfYear mustBe 3
              req.firstSubscriptionDate.getDayOfMonth mustBe 23
              req.transferAccount mustBe AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))
            }
            case _ => fail()
          }
        }
      }
    }

    "serialize transfer previous year funds request from json" in {
      val res = Json.
        parse(validAccountTransferRequest.replace("Transferred", "Previous year funds transferred")).
        validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data match {
            case req: CreateLisaAccountTransferRequest => {
              req.creationReason mustBe "Previous year funds transferred"
              req.investorId mustBe "9876543210"
              req.accountId mustBe "8765432100"
              req.firstSubscriptionDate.getYear mustBe 2011
              req.firstSubscriptionDate.getMonthOfYear mustBe 3
              req.firstSubscriptionDate.getDayOfMonth mustBe 23
              req.transferAccount mustBe AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))
            }
            case _ => fail()
          }
        }
      }
    }

    "serialize creation request from json" in {
      val res = Json.parse(validAccountCreationRequest).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => fail()
        case JsSuccess(data, path) => {
          data match {
            case req: CreateLisaAccountCreationRequest => {
              req.investorId mustBe "9876543210"
              req.accountId mustBe "8765432100"
              req.firstSubscriptionDate.getYear mustBe 2011
              req.firstSubscriptionDate.getMonthOfYear mustBe 3
              req.firstSubscriptionDate.getDayOfMonth mustBe 23
            }
            case _ => fail()
          }
        }
      }
    }

    "deserialize transfer request to json" in {
      val request = CreateLisaAccountTransferRequest(
        creationReason = "Transferred",
        investorId = "9876543210",
        accountId = "8765432100",
        firstSubscriptionDate = new DateTime("2011-03-23"),
        transferAccount = AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))
      )

      val json = Json.toJson[CreateLisaAccountRequest](request)

      json mustBe Json.parse(validAccountTransferRequest.replace("Id", "ID"))
    }

    "deserialize transfer current year funds request to json" in {
      val request = CreateLisaAccountTransferRequest(
        creationReason = "Current year funds transferred",
        investorId = "9876543210",
        accountId = "8765432100",
        firstSubscriptionDate = new DateTime("2011-03-23"),
        transferAccount = AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))
      )

      val json = Json.toJson[CreateLisaAccountRequest](request)

      json mustBe Json.parse(
        validAccountTransferRequest.
          replace("Id", "ID").
          replace("Transferred", "CurrentYearFundsTransferred")
      )
    }

    "deserialize transfer previous year funds request to json" in {
      val request = CreateLisaAccountTransferRequest(
        creationReason = "Previous year funds transferred",
        investorId = "9876543210",
        accountId = "8765432100",
        firstSubscriptionDate = new DateTime("2011-03-23"),
        transferAccount = AccountTransfer("Z543210", "Z543333", new DateTime("2015-12-13"))
      )

      val json = Json.toJson[CreateLisaAccountRequest](request)

      json mustBe Json.parse(
        validAccountTransferRequest.
          replace("Id", "ID").
          replace("Transferred", "PreviousYearFundsTransferred")
      )
    }

    "deserialize creation request to json" in {
      val request = CreateLisaAccountCreationRequest(
        investorId = "9876543210",
        accountId = "8765432100",
        firstSubscriptionDate = new DateTime("2011-03-23")
      )

      val json = Json.toJson[CreateLisaAccountRequest](request)

      json mustBe Json.parse(validAccountCreationRequest.replace("Id", "ID"))
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

    "catch an invalid investorId" in {
      val req = validAccountTransferRequest.replace("9876543210", "2011")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString() == "/investorId" && errors.contains(ValidationError("error.formatting.investorId"))
            }
          } mustBe 1
        }
        case _ => fail()
      }
    }

    "catch an invalid creationReason value" in {
      val req = validAccountTransferRequest.replace("Transferred", "transferred")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "creationReason", Seq(ValidationError("error.formatting.creationReason"))))
        }
        case _ => fail()
      }
    }

    "catch an invalid creationReason data type" in {
      val req = validAccountTransferRequest.replace("\"Transferred\"", "1")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "creationReason", Seq(ValidationError("error.expected.jsstring"))))
        }
        case _ => fail()
      }
    }

    "catch a missing creationReason value" in {
      val req = validAccountCreationRequest.replace("\"creationReason\": \"New\",", "")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "creationReason", Seq(ValidationError("error.path.missing"))))
        }
        case _ => fail()
      }
    }

    "catch a transfer request without the transfer data" in {
      val req = validAccountCreationRequest.replace("New", "Transferred")
      val res = Json.parse(req).validate[CreateLisaAccountRequest]

      res match {
        case JsError(errors) => {
          errors mustBe Seq((JsPath \ "transferAccount", Seq(ValidationError("error.path.missing"))))
        }
        case _ => fail()
      }
    }

    "fail if request has invalid date value " in {
      val req = validAccountCreationRequest.replace("2011-03-23","2011-14-23")
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


  }

}
