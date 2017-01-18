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

package component.steps

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import component.FeatureSuite
import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers

trait Env extends ScalaDsl with EN with Matchers {

  val hostPost = 9000
  val host = sys.env.getOrElse("HOST", s"http://localhost:$hostPost")

  val stubPort = sys.env.getOrElse("WIREMOCK_PORT", "11111").toInt
  val stubHost = "localhost"

  val wireMockUrl = s"http://$stubHost:$stubPort"
  final val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))


  Before { scenario =>
    FeatureSuite.ensureSetup
  }

  After { scenario =>
    WireMock.reset()
  }

}

object Env extends Env
