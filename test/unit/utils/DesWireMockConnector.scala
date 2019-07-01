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

package unit.utils

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, urlPathEqualTo, _}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.{Configuration, Environment, Play}
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.inject.Injector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source
import com.github.tomakehurst.wiremock.WireMockServer
import uk.gov.hmrc.lisaapi.services.LifeEventService

import scala.concurrent.ExecutionContext


trait DesWireMockConnector extends WireMockHelper {

  lazy val httpClient = app.injector.instanceOf[HttpClient]
  lazy val configuration = Play.current.configuration
  lazy val environment = app.injector.instanceOf[Environment]
  lazy val appContext = app.injector.instanceOf[AppContext]
  lazy val runMode = inject[RunMode]
  lazy val port = 8080

  object DesConnector2 extends DesConnector(httpClient, environment, appContext, configuration, runMode) {
    override lazy val desUrl: String = s"http://localhost:${server.port}"
  }



  def desWireMockConnectorStub(urlToGet: String, responseData: String, status: Int) ={

    server.stubFor(post(urlPathEqualTo(urlToGet))
      .willReturn(aResponse()
        .withStatus(status)
          .withBody(responseData)
        )
      )
  }
}
