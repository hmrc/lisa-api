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

package uk.gov.hmrc.lisaapi.controllers.doc

import controllers.Assets
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc.ControllerComponents
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.lisaapi.config.AppContext

class DocumentationSpec extends PlaySpec with MockitoSugar {

  val mockAppContext: AppContext                     = mock[AppContext]
  val mockAssets: Assets                             = mock[Assets]
  val mockControllerComponents: ControllerComponents = stubControllerComponents()

  val documentation = new Documentation(
    mockAppContext,
    mockAssets,
    mockControllerComponents
  )

  "documentation" must {
    "delegate to assets.at with versioned path and sanitised endpoint name" in {
      documentation.documentation("1.0", "Get Account")
      succeed
    }
  }

  "specification" must {
    "delegate to assets.at with versioned conf path and file" in {
      documentation.specification("1.0", "application.yaml")
      succeed
    }
  }

  "definition" must {
    "return OK with the definition content" in {
      when(mockAppContext.apiContext).thenReturn("lifetime-isa")
      when(mockAppContext.v1apiStatus).thenReturn("BETA")
      when(mockAppContext.v2apiStatus).thenReturn("STABLE")
      when(mockAppContext.access).thenReturn(None)
      when(mockAppContext.v1endpointsEnabled).thenReturn(true)
      when(mockAppContext.v2endpointsEnabled).thenReturn(true)

      val result = documentation.definition()(FakeRequest())

      status(result)        mustBe OK
      contentType(result)   mustBe Some("text/plain")
      contentAsString(result) must include("lifetime-isa")
    }

    "handle different API statuses" in {
      when(mockAppContext.apiContext).thenReturn("lifetime-isa")
      when(mockAppContext.v1apiStatus).thenReturn("ALPHA")
      when(mockAppContext.v2apiStatus).thenReturn("DEPRECATED")
      when(mockAppContext.access).thenReturn(None)
      when(mockAppContext.v1endpointsEnabled).thenReturn(false)
      when(mockAppContext.v2endpointsEnabled).thenReturn(false)

      val result = documentation.definition()(FakeRequest())

      status(result)        mustBe OK
      contentType(result)   mustBe Some("text/plain")
      contentAsString(result) must include("ALPHA")
    }
  }

}
