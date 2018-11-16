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

package unit.controllers

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.http.HeaderNames.ACCEPT
import play.api.mvc.{Request, Result, Results}
import play.api.test.FakeRequest
import play.test.Helpers
import uk.gov.hmrc.lisaapi.controllers.{APIVersioning, ErrorAcceptHeaderContentInvalid, ErrorAcceptHeaderInvalid, ErrorAcceptHeaderVersionInvalid}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class APIVersioningSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

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

  }

  object APIVersioningImpl extends APIVersioning {
    override val validateVersion: String => Boolean = List("1.0", "2.0") contains _
    override val validateContentType: String => Boolean = _ == "json"
  }

  def withApiVersionTest[A](request: Request[A]): Result = {
    val response = APIVersioningImpl.withApiVersion {
      case Some("1.0") => Future.successful(Results.Ok)
    }(request)
    Await.result(response, 100 millis)
  }

  def validateHeaderTest[A](request: Request[A]): Result = {
    val builder = APIVersioningImpl.validateHeader()
    val response = builder.invokeBlock[A](
      request,
      (_) => Future.successful(Results.Ok))
    Await.result(response, 100 millis)
  }
}