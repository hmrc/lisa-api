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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.{AppContext, LisaAuthConnector}
import uk.gov.hmrc.lisaapi.controllers.{ErrorNotImplemented, GetLifeEventController}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetLifeEventControllerSpec extends PlaySpec with MockitoSugar with BeforeAndAfter {

  val acceptHeaderV1: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val acceptHeaderV2: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val eventId = "1234567890"

  before {
    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  "Get Life Event" should {

    "not be available for api version 1" in {
      val req = FakeRequest(Helpers.GET, "/")
      val res = SUT.getLifeEvent(lisaManager, accountId, eventId).apply(req.withHeaders(acceptHeaderV1))

      status(res) mustBe NOT_ACCEPTABLE
    }

    "return not implemented for api version 2" in {
      val req = FakeRequest(Helpers.GET, "/")
      val res = SUT.getLifeEvent(lisaManager, accountId, eventId).apply(req.withHeaders(acceptHeaderV2))

      status(res) mustBe NOT_IMPLEMENTED
      contentAsJson(res) mustBe Json.toJson(ErrorNotImplemented)
    }

  }

  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new GetLifeEventController(mockAuthCon, AppContext) {
    override lazy val v2endpointsEnabled = true
  }

}
