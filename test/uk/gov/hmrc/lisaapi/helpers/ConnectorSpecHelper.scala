/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.helpers

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture
import uk.gov.hmrc.lisaapi.utils.WireMockHelper

trait ConnectorSpecHelper extends BaseTestFixture with GuiceOneAppPerSuite with WireMockHelper {

  def applicationBuilder(): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.des.protocol" -> "http",
        "microservice.services.des.host"     -> "localhost",
        "microservice.services.des.port"     -> server.port(),
        "metrics.enabled"                    -> false,
        "auditing.enabled"                   -> false
      )

  override def fakeApplication(): Application = applicationBuilder().build()

  lazy val injector: Injector = app.injector

  def buildResponse(returnStatus: Int, responseBody: String = ""): ResponseDefinitionBuilder = {
    val response: ResponseDefinitionBuilder =
      aResponse()
        .withStatus(returnStatus)
        .withBody(responseBody)

    if (responseBody.nonEmpty) {
      response.withHeader("Content-Type", "application/json")
    } else {
      response
    }
  }

  def stubForGet(url: String, returnStatus: Int, responseBody: String = ""): StubMapping = {
    val response = buildResponse(returnStatus, responseBody)
    server.stubFor(
      get(urlEqualTo(url)).willReturn(response)
    )
  }

  def stubForPost(url: String, returnStatus: Int, responseBody: String = ""): StubMapping = {
    val response = buildResponse(returnStatus, responseBody)
    server.stubFor(
      post(urlEqualTo(url)).willReturn(response)
    )
  }

  def stubForPut(url: String, returnStatus: Int, responseBody: String = ""): StubMapping = {
    val response = buildResponse(returnStatus, responseBody)
    server.stubFor(
      put(urlEqualTo(url)).willReturn(response)
    )
  }

}
