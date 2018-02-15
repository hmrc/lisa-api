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

package unit.controllers

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{DiscoverController, ErrorBadRequestLmrn}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DiscoverControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfter {

  before {
    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  "The Discover available endpoints endpoint" must {

    "return with status 200 ok and appropriate json" in {
      val res = SUT.discover("Z019283").apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

      status(res) mustBe OK
      (contentAsJson(res) \ "_links" \ "close account" \ "href").as[String] mustBe "/lifetime-isa/manager/Z019283/accounts/{accountId}/close-account"
    }

    "return the lisa manager reference number provided" in {
      val res = SUT.discover("Z111111").apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

      status(res) mustBe OK
      (contentAsJson(res) \ "_links" \ "close account" \ "href").as[String] mustBe "/lifetime-isa/manager/Z111111/accounts/{accountId}/close-account"
    }

    "return with status 400 bad request" when {

      "given an invalid lmrn in the url" in {
        val res = SUT.discover("Z0192831").apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
      }

    }

  }

  val mockAuthConnector:LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new DiscoverController {
    override val authConnector: LisaAuthConnector = mockAuthConnector
  }
}
