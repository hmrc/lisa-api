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
import play.test.WithServer


class LifetimeIsaSpec extends WordSpec with MockitoSugar with ShouldMatchers with OneAppPerSuite {

  // val mockLisaController = new  LisaController{ implicit val hc: HeaderCarrier = mock[HeaderCarrier}
  val mockLisaController = SandboxController

  abstract class ServerWithConfig(conf: Map[String, String] = Map.empty) extends WithServer()

  val lisainvestor = "Z019283"

  "LisaController " should {
    "return with status 200 " in
      new ServerWithConfig() {
        val fh = FakeHeaders(Seq(("accept","application/vnd.hmrc.1.0+json")))
        val res = mockLisaController.createLisaInvestor(lisainvestor).apply(FakeRequest(Helpers.PUT,"/").withHeaders(("accept","application/vnd.hmrc.1.0+json")))
        status(res) should be (OK)
      }
  }

}
