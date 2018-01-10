/*
 * Copyright 2018 HM Revenue & Customs
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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import uk.gov.hmrc.lisaapi.config.APIAccessConfig
import org.mockito.Matchers._
import org.mockito.Mockito.when

class APIAccessConfigSpec extends PlaySpec with MockitoSugar {

  val apiAccessConfigNone = APIAccessConfig(None)
  val mockConfiguration = mock[Configuration]
  val apiAccessConfigMocked = APIAccessConfig(Some(mockConfiguration))

    "APIAccessConfig created with no Configuration" should {
    "return private for type" in {
      apiAccessConfigNone.accessType must be ("PUBLIC")
    }
    "return an empty sequence for the whitelist ids" in {
      apiAccessConfigNone.whiteListedApplicationIds must be (Some(Seq()))
    }
  }

  "APIAccessConfig created with valid configuration" should {
    "return private for the access type" in {
      when(mockConfiguration.getString(any(),any())).thenReturn(Some("PRIVATE"))
      apiAccessConfigMocked.accessType must be ("PRIVATE")
    }
    "return a sequence of ids" in {
      when(mockConfiguration.getStringSeq(any())).thenReturn(Some(Seq("a","b")))
      apiAccessConfigMocked.whiteListedApplicationIds must be (Some(Seq("a","b")))
    }
  }
}
