/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.config

import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class AppContextSpec extends PlaySpec with MockitoSugar {

  val mockConfiguration: Configuration = mock[Configuration]
  val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  val appContext: AppContext = new AppContext(mockConfiguration, mockServicesConfig)

  "endpointIsDisabled" must {

    "return false if there are no disabled endpoints in the config" in {
      when(mockConfiguration.getOptional[String](any())(any())).thenReturn(None)

      appContext.endpointIsDisabled("test1") mustBe false
    }

    "return false if the named endpoint isn't in the disabled list" in {
      when(mockConfiguration.getOptional[Seq[String]](any())(any())).thenReturn(Some(Seq[String]("test1")))

      appContext.endpointIsDisabled("test2") mustBe false
    }

    "return true if the named endpoint is in the disabled list" in {
      when(mockConfiguration.getOptional[Seq[String]](any())(any())).thenReturn(Some(Seq[String]("test1", "test2", "test3")))

      appContext.endpointIsDisabled("test3") mustBe true
    }
  }
}

