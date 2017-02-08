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

package uk.gov.hmrc.lisaapi.controllers

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import org.scalatestplus.play.OneAppPerSuite
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import play.test.WithServer


class SandboxControllerSpec extends WordSpec with MockitoSugar with ShouldMatchers with OneAppPerSuite {


  val mockLisaController = new SandboxController

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val contentTypeHeader: (String, String) = (HeaderNames.CONTENT_TYPE, "application/json")
  val authorizationHeader: (String, String) = (HeaderNames.AUTHORIZATION, "bearer token")

  abstract class ServerWithConfig(conf: Map[String, String] = Map.empty) extends WithServer()

  val lisaManager = "Z019283"

  "The LisaSandbox Controller  " should {
    "return with status 200 " in
     {
        val res = mockLisaController.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT,"/").withHeaders(acceptHeader, authorizationHeader))
        status(res) should be (OK)
     }

    "return with status 401 " in
      {
      val res = mockLisaController.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader))
      status(res) should be(401)
    }

    "return with status 406 " in
     {
        val res = mockLisaController.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT,"/").withHeaders(("accept","application/vnd.hmrc.2.0+json")))
        status(res) should be (406)
     }
  }

}
