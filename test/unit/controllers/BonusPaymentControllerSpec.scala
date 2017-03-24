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

import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.mockito.Mockito.reset
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.BonusPaymentController
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse
import uk.gov.hmrc.lisaapi.services.BonusPaymentService

import scala.concurrent.Future
import scala.io.Source

case object TestBonusPaymentResponse extends RequestBonusPaymentResponse

class BonusPaymentControllerSpec  extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"
  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString

  "The Life Event Controller" should {

    "return with status 201 created" when {
      "given a Success Response from the service layer" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374")))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe ("1928374")
        }
      }
    }

    "return with status 403 forbidden" when {
      "given a BONUS_CLAIM_ERROR from the service layer" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentErrorResponse(403, DesFailureResponse("BONUS_CLAIM_ERROR", "xyz"))))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("BONUS_CLAIM_ERROR")
          (contentAsJson(res) \ "message").as[String] mustBe ("xyz")
        }
      }
    }

    "return with status 400 bad request" when {
      "provided an invalid json object" in {
        doRequest(validBonusPaymentJson.replace("1234567891","X")) { res =>
          status(res) mustBe (BAD_REQUEST)
          (contentAsJson(res) \ "code").as[String] mustBe ("BAD_REQUEST")
          (contentAsJson(res) \ "message").as[String] mustBe ("Bad Request")
        }
      }
    }

    "return with status 500 internal server error" when {
      "an exception gets thrown" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validBonusPaymentJson) { res =>
          reset(mockService) // removes the thenThrow

          status(res) mustBe (INTERNAL_SERVER_ERROR)
          (contentAsJson(res) \ "code").as[String] mustBe ("INTERNAL_SERVER_ERROR")
        }
      }

      "an unexpected result comes back from the service" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(TestBonusPaymentResponse))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
          (contentAsJson(res) \ "code").as[String] mustBe ("INTERNAL_SERVER_ERROR")
        }
      }
    }

  }

  def doRequest(jsonString: String)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.requestBonusPayment(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService = mock[BonusPaymentService]
  val SUT = new BonusPaymentController {
    override val service: BonusPaymentService = mockService
  }
}
