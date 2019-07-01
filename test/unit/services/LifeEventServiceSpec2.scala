/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.services

import akka.http.scaladsl.model.StatusCodes
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Environment, Play}
import play.api.test.{FakeHeaders, FakeRequest, Injecting}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.{ReportLifeEventAccountClosedResponse, ReportLifeEventRequest}
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesResponse}
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import unit.utils.DesWireMockConnector
import com.github.tomakehurst.wiremock.WireMockServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import com.github.tomakehurst.wiremock.WireMockServer
import play.api.libs.json.JsValue
import play.api.mvc.Result
class LifeEventServiceSpec2 extends DesWireMockConnector with Injecting {

  "LiveEventController " must {
    "return ReportLifeEventAccountClosedResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED" in {

        desWireMockConnectorStub("/lifetime-isa/manager/Z019283/accounts/192837/life-event", "INVESTOR_ACCOUNT_ALREADY_CLOSED", 403)

        val responseFuture : Future[DesResponse] = DesConnector2.reportLifeEvent("Z019283", "192837", ReportLifeEventRequest("LISA Investor Terminal Ill Health", new DateTime("2017-04-06")))(HeaderCarrier())


        responseFuture.onComplete(
          resp => println("FFFFFFFFFFF"+resp)
        )

//        responseFuture mustBe Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED",""))
//

      }

    }

  }

}