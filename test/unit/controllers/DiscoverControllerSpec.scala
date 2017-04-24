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
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.DiscoverController
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.AuditService

import scala.concurrent.Future


class DiscoverControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  "The Discover available endpoints endpoint" must {

    "return with status 200 ok and appropriate json" in {
      val res = SUT.discover("Z019283").apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

      status(res) mustBe OK
      (contentAsJson(res) \ "_links" \ "life events" \ "href").as[String] mustBe "/lifetime-isa/manager/Z019283/accounts/{accountID}/events"
    }

    "return the lisa manager reference number provided" in {
      val res = SUT.discover("Z111111").apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

      status(res) mustBe OK
      (contentAsJson(res) \ "_links" \ "life events" \ "href").as[String] mustBe "/lifetime-isa/manager/Z111111/accounts/{accountID}/events"
    }

  }

  val SUT = new DiscoverController {}
}
