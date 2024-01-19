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
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.AccountController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LisaControllerSpec extends ControllerTestFixture {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  case class TestType(prop1: String, prop2: String)

  abstract class AccountControllerTestHelper extends AccountController(
    mockAuthConnector,
    mockAppContext,
    mockAccountService,
    mockAuditService,
    mockLisaMetrics,
    mockControllerComponents,
    mockParser
  ) {
    def testJsonValidator(): Action[AnyContent]
    def testLMRNValidator(lmrn: String): Action[AnyContent]
    def testAccountIdValidator(accountId: String): Action[AnyContent]
    def testTransactionIdValidator(transactionId: String): Action[AnyContent]
  }

  val accountController: AccountControllerTestHelper = new AccountControllerTestHelper {

    def testJsonValidator(): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidJson[TestType](
        _ => Future.successful(PreconditionFailed) // we don't ever want this to return
        ,
        lisaManager = ""
      )
    }

    def testLMRNValidator(lmrn: String): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withValidLMRN(lmrn)(() => Future.successful(Ok))
    }

    def testAccountIdValidator(accountId: String): Action[AnyContent] = validateHeader(mockParser).async {
      implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidAccountId(accountId)(() => Future.successful(Ok))
    }

    def testTransactionIdValidator(transactionId: String): Action[AnyContent] = validateHeader(mockParser).async {
      implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidTransactionId(transactionId)(() => Future.successful(Ok))
    }
  }

  val fundReleaseJsonForInvalidAddress = """
{
  "propertyDetails": {
    "nameOrNumber": "Flat~1!!",
    "postalCode": "AA11 1AA"
  }
}
"""

  val fundReleaseJsonForNotValidAddress = """
{
  "propertyDetails": {
    "nameOrNumber": "Flat A Wiiliams Park Benton Road Newcastle Upon Tyne",
    "postalCode": "AA11 1AA"
  }
}
"""

  val fundReleaseJsonForValidAddress = """
{
  "propertyDetails": {
    "nameOrNumber": "Flat A",
    "postalCode": "AA11 1AA"
  }
}
"""

  val fundReleaseJsonForNoAddress = """
{
  "eventDate": "2017-05-10"
}
"""

  implicit val testTypeReads: Reads[TestType] = (
    (JsPath \ "prop1").read[Int].map[String](i => throw new RuntimeException("Deliberate Test Exception")) and
      (JsPath \ "prop2").read[String]
  )(TestType.apply _)

  "The withValidJson method" must {

    "return with an Internal Server Error" when {

      "an exception is thrown by one of our Json reads" in {
        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testJsonValidator()
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe INTERNAL_SERVER_ERROR
      }
    }
  }

  "The withValidLMRN method" must {

    "return a Bad Request Error" when {

      "an invalid lmrn is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testLMRNValidator("Z")
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
      }

    }

    "pass through to the nested method" when {

      "a valid lmrn is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testLMRNValidator("Z123456")
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe OK
      }

    }

  }

  "The withValidaccount method" must {

    "return a Bad Request Error" when {

      "an invalid account is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testAccountIdValidator("Z" * 21)
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "Enter accountId in the correct format, like ABC12345"
      }

    }

    "returns 200" when {

      "a valid accountId is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testAccountIdValidator("ABC12345")
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe OK
      }

    }

  }

  "The withValidTransactionId method" must {

    "return a Bad Request Error" when {

      "an invalid transaction Id is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testTransactionIdValidator("123.345")
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "transactionId in the URL is in the wrong format"
      }

    }

    "returns 200" when {

      "a valid accountId is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testTransactionIdValidator("1234567890")
          .apply(
            FakeRequest(Helpers.PUT, "/")
              .withHeaders(acceptHeader)
              .withBody(AnyContentAsJson(Json.parse(jsonString)))
          )

        status(res) mustBe OK
      }

    }

  }
}
