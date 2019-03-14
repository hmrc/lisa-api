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

package unit.config

import collection.JavaConverters._
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.play.bootstrap.config.RunMode

class AppContextSpec extends PlaySpec with MockitoSugar {

  val mockConfiguration = mock[Configuration]
  val mockRunMode = mock[RunMode]
  val SUT = new AppContext(mockConfiguration, mockRunMode)

  "endpointIsDisabled" must {

    "return false if there are no disabled endpoints in the config" in {
      when(mockConfiguration.getStringList(any())).thenReturn(None)

      SUT.endpointIsDisabled("test1") mustBe false
    }

    "return false if the named endpoint isn't in the disabled list" in {
      when(mockConfiguration.getStringList(any())).thenReturn(Some(List[String]("test1").asJava))

      SUT.endpointIsDisabled("test2") mustBe false
    }

    "return true if the named endpoint is in the disabled list" in {
      when(mockConfiguration.getStringList(any())).thenReturn(Some(List[String]("test1", "test2", "test3").asJava))

      SUT.endpointIsDisabled("test3") mustBe true
    }

  }

}