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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, urlPathEqualTo}
import org.joda.time.DateTime
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.ReportLifeEventRequest
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesResponse}
import unit.utils.{DesWireMockConnector, WireMockHelper}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
class LifeEventServiceSpec2 extends DesWireMockConnector with Injecting {

  "LiveEventController " must {
    "return ReportLifeEventAccountClosedResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED" in {

        desWireMockConnectorStub("/lifetime-isa/manager/Z019283/accounts/192837/life-event", "INVESTOR_ACCOUNT_ALREADY_CLOSED", 403)
        
        val responseFuture : Future[DesResponse] = DesConnector2.reportLifeEvent("Z019283", "192837", ReportLifeEventRequest("LISA Investor Terminal Ill Health", new DateTime("2017-04-06")))(HeaderCarrier())

        Await.result(responseFuture, Duration.Inf) mustBe DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","")

//        responseFuture mustBe Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED",""))
//

      }

    }

  }

}