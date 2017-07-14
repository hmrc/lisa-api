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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers._

import scala.concurrent.Future


class HelperControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  "The Not Implemented endpoint" must {

    "return 501 not implemented" in {
      val result = SUT.notImplemented("Z1234", "1234567890").
                          apply(FakeRequest(Helpers.POST, "/").
                          withHeaders(acceptHeader).
                          withBody(AnyContentAsJson(Json.parse("{}"))))

      status(result) mustBe NOT_IMPLEMENTED
      contentAsJson(result) mustBe Json.toJson(ErrorNotImplemented)
    }

  }

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val SUT = new HelperController {
    override val authConnector = mockAuthCon
  }

}