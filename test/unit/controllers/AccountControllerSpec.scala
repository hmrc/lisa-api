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

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.AccountController
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.AccountService

import scala.concurrent.Future


class AccountControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"

  val createAccountJson = """{
                              |  "investorID" : "9876543210",
                              |  "lisaManagerReferenceNumber" : "Z4321",
                              |  "accountID" :"8765432100",
                              |  "creationReason" : "New",
                              |  "firstSubscriptionDate" : "2011-03-23"
                              |}""".stripMargin

  val transferAccountJson = """{
                            |  "investorID" : "9876543210",
                            |  "lisaManagerReferenceNumber" : "Z4321",
                            |  "accountID" :"8765432100",
                            |  "creationReason" : "Transferred",
                            |  "firstSubscriptionDate" : "2011-03-23",
                            |  "transferAccount": {
                            |    "transferredFromAccountID": "Z543210",
                            |    "transferredFromLMRN": "Z543333",
                            |    "transferInDate": "2015-12-13"
                            |  }
                            |}""".stripMargin

  "The Account endpoint" must {

    "return with status 201 created" when {
      "submitted a valid create account request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse("AB123456")))

        doRequest(createAccountJson) { res =>
          status(res) mustBe (CREATED)
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_NOT_FOUND" when {
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doRequest(createAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_NOT_FOUND")
        }
      }
    }

    "return with status 500 internal server error" when {
      "the data service returns an error" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doRequest(createAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }

    "return with status 501 not implemented" when {
      "submitted a account transfer request" in {
        doRequest(transferAccountJson) { res =>
          status(res) mustBe (NOT_IMPLEMENTED)
        }
      }
    }

  }

  def doRequest(jsonString: String)(callback: (Future[Result]) => Unit) {
    val res = SUT.createOrTransferLisaAccount(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService = mock[AccountService]
  val SUT = new AccountController{
    override val service: AccountService = mockService
  }

}
