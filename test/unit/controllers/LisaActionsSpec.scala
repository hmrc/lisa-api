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
import play.api.libs.json.Json.toJson
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import play.api.test.FakeRequest
import play.test.Helpers
import uk.gov.hmrc.lisaapi.controllers.{ErrorBadRequestAccountId, ErrorBadRequestLmrn, LMRNRequest, LMRNWithAccountRequest, LisaActions}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.language.postfixOps

class LisaActionsSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "The validateLMRNAction" must {
    "accept a valid 4 digit LMRN" in {
      val action: ActionRefiner[Request, LMRNRequest] = TestController.validateLMRN("Z1234")
      val response = action.invokeBlock[AnyContent](FakeRequest(Helpers.GET, "/"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe Results.Ok
    }

    "accept a valid 6 digit LMRN" in {
      val action: ActionRefiner[Request, LMRNRequest] = TestController.validateLMRN("Z123456")
      val response = action.invokeBlock[AnyContent](FakeRequest(Helpers.GET, "/"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe Results.Ok
    }

    "reject a LMRN without a leading Z" in {
      val action: ActionRefiner[Request, LMRNRequest] = TestController.validateLMRN("123456")
      val response = action.invokeBlock[AnyContent](FakeRequest(Helpers.GET, "/"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe BadRequest(toJson(ErrorBadRequestLmrn))
    }

    "reject a LMRN with 3 digits" in {
      val action: ActionRefiner[Request, LMRNRequest] = TestController.validateLMRN("Z123")
      val response = action.invokeBlock[AnyContent](FakeRequest(Helpers.GET, "/"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe BadRequest(toJson(ErrorBadRequestLmrn))
    }

    "reject a LMRN with 5 digits" in {
      val action: ActionRefiner[Request, LMRNRequest] = TestController.validateLMRN("Z12345")
      val response = action.invokeBlock[AnyContent](FakeRequest(Helpers.GET, "/"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe BadRequest(toJson(ErrorBadRequestLmrn))
    }

    "reject a LMRN with 7 digits" in {
      val action: ActionRefiner[Request, LMRNRequest] = TestController.validateLMRN("Z1234567")
      val response = action.invokeBlock[AnyContent](FakeRequest(Helpers.GET, "/"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe BadRequest(toJson(ErrorBadRequestLmrn))
    }
  }

  "The validateAccountIdAction" must {
    "accept a valid account id with 1 character" in {
      val action: ActionRefiner[LMRNRequest, LMRNWithAccountRequest] = TestController.validateAccountId("1")
      val response = action.invokeBlock[AnyContent](LMRNRequest(FakeRequest(Helpers.GET, "/"), "Z1234"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe Results.Ok
    }

    "accept a valid account id with 20 characters" in {
      val action: ActionRefiner[LMRNRequest, LMRNWithAccountRequest] = TestController.validateAccountId("1234567890abcdefghij")
      val response = action.invokeBlock[AnyContent](LMRNRequest(FakeRequest(Helpers.GET, "/"), "Z1234"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe Results.Ok
    }

    "accept a valid account id with special characters" in {
      val action: ActionRefiner[LMRNRequest, LMRNWithAccountRequest] = TestController.validateAccountId("a b-c/123")
      val response = action.invokeBlock[AnyContent](LMRNRequest(FakeRequest(Helpers.GET, "/"), "Z1234"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe Results.Ok
    }

    "reject an account id with 21 characters" in {
      val action: ActionRefiner[LMRNRequest, LMRNWithAccountRequest] = TestController.validateAccountId("123456789012345678901")
      val response = action.invokeBlock[AnyContent](LMRNRequest(FakeRequest(Helpers.GET, "/"), "Z1234"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe BadRequest(toJson(ErrorBadRequestAccountId))
    }

    "reject an account id with illegal characters" in {
      val action: ActionRefiner[LMRNRequest, LMRNWithAccountRequest] = TestController.validateAccountId("12345%67890")
      val response = action.invokeBlock[AnyContent](LMRNRequest(FakeRequest(Helpers.GET, "/"), "Z1234"), (_) => Future.successful(Results.Ok))
      Await.result(response, 100 millis) mustBe BadRequest(toJson(ErrorBadRequestAccountId))
    }
  }

  object TestController extends LisaActions

}
