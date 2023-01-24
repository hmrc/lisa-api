/*
 * Copyright 2023 HM Revenue & Customs
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

package unit.controllers

import helpers.ControllerTestFixture
import org.mockito.Mockito._
import play.api.http.HeaderNames.ACCEPT
import play.api.mvc._
import play.api.test.FakeRequest
import play.test.Helpers
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.controllers.{APIVersioning, ErrorAcceptHeaderContentInvalid, ErrorAcceptHeaderInvalid, ErrorAcceptHeaderVersionInvalid, ErrorApiNotAvailable}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class APIVersioningSpec extends ControllerTestFixture {

  "The withApiVersion function" must {

    "allow a supported version" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.1.0+json"))
      withApiVersionTest(request) mustBe Results.Ok
    }

    "handle an unimplemented version number" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))
      withApiVersionTest(request) mustBe ErrorAcceptHeaderVersionInvalid.asResult
    }

    "handle a malformed accept header" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.1.+json"))
      withApiVersionTest(request) mustBe ErrorAcceptHeaderInvalid.asResult
    }
  }

  "The validateHeader action" must {

    "allow a supported version" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))
      validateHeaderTest(request) mustBe Results.Ok
    }

    "handle an invalid version number" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.3.0+json"))
      validateHeaderTest(request) mustBe ErrorAcceptHeaderVersionInvalid.asResult
    }

    "handle an invalid content type" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.2.0+xml"))
      validateHeaderTest(request) mustBe ErrorAcceptHeaderContentInvalid.asResult
    }

    "handle a missing accept header" in {
      val request = FakeRequest(Helpers.GET, "/")
      validateHeaderTest(request) mustBe ErrorAcceptHeaderInvalid.asResult
    }

    "handle a malformed accept header" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/hmrc.3.0+json"))
      validateHeaderTest(request) mustBe ErrorAcceptHeaderInvalid.asResult
    }

    "handle a v2 request when v2 endpoints are disabled" in {
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))
      val builder = APIVersioningImplV2Disabled.validateHeader(mockParser)
      val response = builder.invokeBlock[AnyContent](request, _ => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe ErrorAcceptHeaderVersionInvalid.asResult
    }

  }

  "The isEndpointEnabled function" must {
    "allow execution if the appContext returns that the endpoint is not disabled" in {
      when(mockAppContext.endpointIsDisabled("test")).thenReturn(false)
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))
      isEndpointEnabledTest(request) mustBe Results.Ok
    }
    "return a not implemented error if the appContext returns that the endpoint is disabled" in {
      when(mockAppContext.endpointIsDisabled("test")).thenReturn(true)
      val request = FakeRequest(Helpers.GET, "/").withHeaders((ACCEPT, "application/vnd.hmrc.2.0+json"))
      isEndpointEnabledTest(request) mustBe ErrorApiNotAvailable.asResult
    }
  }

  object APIVersioningImpl extends APIVersioning {
    override val validateVersion: String => Boolean = List("1.0", "2.0") contains _
    override val validateContentType: String => Boolean = _ == "json"
    override lazy val v2endpointsEnabled: Boolean = true

    override protected def appContext: AppContext = mockAppContext
  }

  object APIVersioningImplV2Disabled extends APIVersioning {
    override val validateVersion: String => Boolean = List("1.0", "2.0") contains _
    override val validateContentType: String => Boolean = _ == "json"
    override lazy val v2endpointsEnabled: Boolean = false

    override protected def appContext: AppContext = mockAppContext
  }

  def withApiVersionTest[A](request: Request[A]): Result = {
    val response = APIVersioningImpl.withApiVersion {
      case Some("1.0") => Future.successful(Results.Ok)
    }(request)
    Await.result(response, 100 millis)
  }

  def validateHeaderTest[A](request: Request[A]): Result = {
    val builder = APIVersioningImpl.validateHeader(mockParser)
    val response = builder.invokeBlock[A](
      request,
      (_) => Future.successful(Results.Ok))
    Await.result(response, 100 millis)
  }

  def isEndpointEnabledTest[A](request: Request[A]): Result = {
    val builder = APIVersioningImpl.isEndpointEnabled("test", mockParser)
    val response = builder.invokeBlock[A](
      request,
      (_) => Future.successful(Results.Ok))
    Await.result(response, 100 millis)
  }
}
