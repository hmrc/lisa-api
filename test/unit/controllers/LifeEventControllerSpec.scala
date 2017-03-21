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

import com.sun.glass.ui.MenuItem.Callback
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.LifeEventController
import scala.concurrent.Future
import uk.gov.hmrc.lisaapi.services.LifeEventService

/**
  * Created by mark on 20/03/17.
  */
class LifeEventControllerSpec  extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"

  val reportLifeEventJson =
    """
      |{
      |  "accountId" : "1234567890",
      |  "lisaManagerReferenceNumber" : "Z543210",
      |  "eventType" : "LISA Investor Terminal Ill Health",
      |  "eventDate" : "2017-04-06"
      |}
    """.stripMargin

  "The Report A Life Event Endpoint" must {

    "return with status 201 created and Life Even ID" when {
      "submitted with a valid report life event request" in {

      }
    }
  }

  def doReportLifeEventRequest(jsonString: String)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.reportLisaLifeEvent(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))
  }

  val mockService = mock[LifeEventService]
  val SUT = new LifeEventController {
    override val service: LifeEventService = mockService
  }
}
