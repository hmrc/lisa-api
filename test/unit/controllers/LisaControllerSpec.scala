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

import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{AccountController, ErrorNotImplemented}
import uk.gov.hmrc.lisaapi.services.AccountService
import uk.gov.hmrc.lisaapi.utils.ErrorConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LisaControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  case class TestType(prop1: String, prop2: String)

  implicit val testTypeReads: Reads[TestType] = (
    (JsPath \ "prop1").read[Int].map[String](i => throw new RuntimeException("Deliberate Test Exception")) and
    (JsPath \ "prop2").read[String]
  ) (TestType.apply _)

  "The withValidJson method" must {

    "return with an Internal Server Error" when {

      "an exception is thrown by one of our Json reads" in {
        when(mockAuthCon.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testJsonValidator().apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "The withValidLMRN method" must {

    "return a Bad Request Error" when {

      "an invalid lmrn is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testLMRNValidator("Z").apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "lisaManagerReferenceNumber in the URL is in the wrong format"
      }

    }

    "pass through to the nested method" when {

      "a valid lmrn is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testLMRNValidator("Z123456").apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe OK
      }

    }

  }

  "The todo endpoint" must {
    "return 501 not implemented" in {
      val result = SUT.todo("Z1234", "ABCD1234", "").apply(FakeRequest(Helpers.POST, "/").
        withHeaders(acceptHeader).withBody(AnyContentAsJson(Json.parse("{}"))))

      status(result) mustBe NOT_IMPLEMENTED
      contentAsJson(result) mustBe Json.toJson(ErrorNotImplemented)
    }
  }

  "acceptHeaderValidationRules" must {
    "allow v1.0" in {
      SUT.acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0+json")) mustBe true
    }
    "allow v2.0" in {
      SUT.acceptHeaderValidationRules(Some("application/vnd.hmrc.2.0+json")) mustBe true
    }
    "disallow v3.0" in {
      SUT.acceptHeaderValidationRules(Some("application/vnd.hmrc.3.0+json")) mustBe false
    }
  }

  val mockService = mock[AccountService]
  val mockErrorConverter = mock[ErrorConverter]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val SUT = new AccountController {
    override val service: AccountService = mockService
    override val authConnector = mockAuthCon

    def testJsonValidator(): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime = System.currentTimeMillis()
      withValidJson[TestType](_ =>
        Future.successful(PreconditionFailed) // we don't ever want this to return
        , lisaManager = ""
      )
    }

    def testLMRNValidator(lmrn: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime = System.currentTimeMillis()
      withValidLMRN(lmrn) { () => Future.successful(Ok) }
    }
  }


}
