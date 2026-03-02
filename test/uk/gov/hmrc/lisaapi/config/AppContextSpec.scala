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

package uk.gov.hmrc.lisaapi.config

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppContextSpec extends PlaySpec with MockitoSugar {

  val mockConfiguration: Configuration   = mock[Configuration]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]

  "AppContext" must {

    "return correct configuration values" in {
      when(mockServicesConfig.getString("appName")).thenReturn("lisa-api")
      when(mockServicesConfig.getString("api.context")).thenReturn("lifetime-isa")
      when(mockServicesConfig.getString("baseUrl")).thenReturn("http://localhost:9667")
      when(mockServicesConfig.getString("api.status")).thenReturn("BETA")
      when(mockServicesConfig.getString("api.statusv2")).thenReturn("STABLE")
      when(mockServicesConfig.getString("desauthtoken")).thenReturn("test-token")
      when(mockServicesConfig.getString("environment")).thenReturn("test")
      when(mockServicesConfig.getBoolean("api.endpointsEnabled")).thenReturn(true)
      when(mockServicesConfig.getBoolean("api.endpointsEnabledv2")).thenReturn(true)
      when(mockServicesConfig.baseUrl("des")).thenReturn("http://localhost:8080")
      when(mockConfiguration.getOptional[Configuration](any())(any())).thenReturn(None)

      val appContext: AppContext = new AppContext(mockConfiguration, mockServicesConfig)

      appContext.appName            mustBe "lisa-api"
      appContext.apiContext         mustBe "lifetime-isa"
      appContext.baseUrl            mustBe "http://localhost:9667"
      appContext.v1apiStatus        mustBe "BETA"
      appContext.v2apiStatus        mustBe "STABLE"
      appContext.desAuthToken       mustBe "test-token"
      appContext.desUrlHeaderEnv    mustBe "test"
      appContext.v1endpointsEnabled mustBe true
      appContext.v2endpointsEnabled mustBe true
      appContext.desUrl             mustBe "http://localhost:8080"
      appContext.access             mustBe None
    }

    "return access configuration when present" in {
      val accessConfig = mock[Configuration]
      when(mockConfiguration.getOptional[Configuration](any())(any())).thenReturn(Some(accessConfig))
      when(mockServicesConfig.getString(any())).thenReturn("")
      when(mockServicesConfig.getBoolean(any())).thenReturn(true)
      when(mockServicesConfig.baseUrl(any())).thenReturn("")

      val appContext: AppContext = new AppContext(mockConfiguration, mockServicesConfig)

      appContext.access mustBe Some(accessConfig)
    }
  }

  "endpointIsDisabled" must {

    "return false if there are no disabled endpoints in the config" in {
      when(mockConfiguration.getOptional[String](any())(any())).thenReturn(None)
      when(mockServicesConfig.getString(any())).thenReturn("")
      when(mockServicesConfig.getBoolean(any())).thenReturn(true)
      when(mockServicesConfig.baseUrl(any())).thenReturn("")

      val appContext: AppContext = new AppContext(mockConfiguration, mockServicesConfig)
      appContext.endpointIsDisabled("test1") mustBe false
    }

    "return false if the named endpoint isn't in the disabled list" in {
      when(mockConfiguration.getOptional[Seq[String]](any())(any())).thenReturn(Some(Seq[String]("test1")))
      when(mockServicesConfig.getString(any())).thenReturn("")
      when(mockServicesConfig.getBoolean(any())).thenReturn(true)
      when(mockServicesConfig.baseUrl(any())).thenReturn("")

      val appContext: AppContext = new AppContext(mockConfiguration, mockServicesConfig)
      appContext.endpointIsDisabled("test2") mustBe false
    }

    "return true if the named endpoint is in the disabled list" in {
      when(mockConfiguration.getOptional[Seq[String]](any())(any()))
        .thenReturn(Some(Seq[String]("test1", "test2", "test3")))
      when(mockServicesConfig.getString(any())).thenReturn("")
      when(mockServicesConfig.getBoolean(any())).thenReturn(true)
      when(mockServicesConfig.baseUrl(any())).thenReturn("")

      val appContext: AppContext = new AppContext(mockConfiguration, mockServicesConfig)
      appContext.endpointIsDisabled("test3") mustBe true
    }
  }

}
