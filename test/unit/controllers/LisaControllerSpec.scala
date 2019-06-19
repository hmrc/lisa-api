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

package unit.controllers

import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsValue, Json, Reads}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc._
import play.api.test.Helpers._
import play.api.mvc.{AnyContentAsJson, ControllerComponents, PlayBodyParsers, Result}
import play.api.test._
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.controllers.AccountController
import uk.gov.hmrc.lisaapi.metrics.LisaMetrics
import uk.gov.hmrc.lisaapi.models.ReportLifeEventSuccessResponse
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.lisaapi.utils.ErrorConverter

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LisaControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with Injecting {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  case class TestType(prop1: String, prop2: String)

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
    ) (TestType.apply _)

  "The withValidAddress method" must {

    "return with a Bad Request" when {

      "when address contains invalid charcters" in {
        val res = SUT.testAddressValidator().apply(FakeRequest(Helpers.POST, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(fundReleaseJsonForInvalidAddress))))
        status(res) mustBe BAD_REQUEST
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "nameOrNumber must be 35 characters or less"
      }
    }

    "return with a Bad Request" when {

      "when address contains more than 35 charcters" in {
        val res = SUT.testAddressValidator().apply(FakeRequest(Helpers.POST, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(fundReleaseJsonForNotValidAddress))))
        status(res) mustBe BAD_REQUEST
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "nameOrNumber must be 35 characters or less"
      }
    }

    "return with a success" when {

      "when address is in valid format" in {
        val res = SUT.testAddressValidator().apply(FakeRequest(Helpers.POST, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(fundReleaseJsonForValidAddress))))
        status(res) mustBe OK
      }
    }

    "return with a success" when {

      "when address does not exist" in {
        val res = SUT.testAddressValidator().apply(FakeRequest(Helpers.POST, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(fundReleaseJsonForNoAddress))))
        status(res) mustBe OK
      }
    }
  }

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
        (json \ "message").as[String] mustBe "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
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

  "The withValidaccount method" must {

    "return a Bad Request Error" when {

      "an invalid account is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testAccountIdValidator("Z" *21).apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "Enter accountId in the correct format, like ABC12345"
      }

    }

    "returns 200" when {

      "a valid accountId is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testAccountIdValidator("ABC12345").apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe OK
      }

    }

  }

  "The withValidTransactionId method" must {

    "return a Bad Request Error" when {

      "an invalid transaction Id is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testTransactionIdValidator("123.345" ).apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe BAD_REQUEST

        val json = contentAsJson(res)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "transactionId in the URL is in the wrong format"
      }

    }

    "returns 200" when {

      "a valid accountId is passed in" in {
        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res = SUT.testTransactionIdValidator("1234567890").apply(FakeRequest(Helpers.PUT, "/")
          .withHeaders(acceptHeader)
          .withBody(AnyContentAsJson(Json.parse(jsonString))))

        status(res) mustBe OK
      }

    }

  }

    val mockService = mock[AccountService]
    val mockErrorConverter = mock[ErrorConverter]
    val mockAuthCon: AuthConnector = mock[AuthConnector]
    val mockAuditService: AuditService = mock[AuditService]
    val mockAppContext: AppContext = mock[AppContext]
    val mockLisaMetrics: LisaMetrics = mock[LisaMetrics]
    val mockControllerComponents = inject[ControllerComponents]
    val mockParser = inject[PlayBodyParsers]
    val SUT = new AccountController(mockAuthCon, mockAppContext, mockService, mockAuditService, mockLisaMetrics, mockControllerComponents, mockParser) {

      def testJsonValidator(): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidJson[TestType](_ =>
          Future.successful(PreconditionFailed) // we don't ever want this to return
          , lisaManager = ""
        )
      }

      def testLMRNValidator(lmrn: String): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidLMRN(lmrn) { () => Future.successful(Ok) }
      }

      def testAccountIdValidator(accountId: String): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidAccountId(accountId) { () => Future.successful(Ok) }
      }


      def testAddressValidator(): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
         withValidAddress(request.body.asJson){
           () => Future.successful(Ok) }
      }


      def testTransactionIdValidator(transactionId: String): Action[AnyContent] = validateHeader(mockParser).async { implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidTransactionId(transactionId) { () => Future.successful(Ok) }
      }

    }


  }
