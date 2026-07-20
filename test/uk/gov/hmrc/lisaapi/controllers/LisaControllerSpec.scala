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

package uk.gov.hmrc.lisaapi.controllers

import ch.qos.logback.classic.Level
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.Logger
import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments, InsufficientConfidenceLevel}
import uk.gov.hmrc.lisaapi.controllers.AccountController
import uk.gov.hmrc.lisaapi.helpers.ControllerTestFixture
import uk.gov.hmrc.play.bootstrap.tools.LogCapturing

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LisaControllerSpec extends ControllerTestFixture with LogCapturing {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  case class TestType(prop1: String, prop2: String)

  abstract class AccountControllerTestHelper
      extends AccountController(
        mockAuthConnector,
        mockAppContext,
        mockAccountService,
        mockAuditService,
        mockLisaMetrics,
        mockControllerComponents,
        mockParser
      ) {
    def testJsonValidator(lisaMangerRef: String): Action[AnyContent]

    def testLMRNValidator(lmrn: String): Action[AnyContent]

    def testAccountIdValidator(accountId: String): Action[AnyContent]

    def testTransactionIdValidator(transactionId: String): Action[AnyContent]
  }

  val accountController: AccountControllerTestHelper = new AccountControllerTestHelper {

    def testJsonValidator(lisaMangerRef: String): Action[AnyContent] = validateHeader(mockParser).async {
      implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        withValidJson[TestType](
          _ => Future.successful(PreconditionFailed) // we don't ever want this to return
          ,
          lisaManager = lisaMangerRef
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

  val fundReleaseJsonForInvalidAddress =
    """
{
  "propertyDetails": {
    "nameOrNumber": "Flat~1!!",
    "postalCode": "AA11 1AA"
  }
}
"""

  val fundReleaseJsonForNotValidAddress =
    """
{
  "propertyDetails": {
    "nameOrNumber": "Flat A Wiiliams Park Benton Road Newcastle Upon Tyne",
    "postalCode": "AA11 1AA"
  }
}
"""

  val fundReleaseJsonForValidAddress =
    """
{
  "propertyDetails": {
    "nameOrNumber": "Flat A",
    "postalCode": "AA11 1AA"
  }
}
"""

  val fundReleaseJsonForNoAddress =
    """
{
  "eventDate": "2017-05-10"
}
"""

  implicit val testTypeReads: Reads[TestType] = (
    (JsPath \ "prop1").read[Int].map[String](_ => throw new RuntimeException("Deliberate Test Exception")) and
      (JsPath \ "prop2").read[String]
  )(TestType.apply _)

  "The withValidJson method" must {

    "return with an Internal Server Error" when {

      "an exception is thrown by one of our Json reads" in {
        val lisaManagerReferenceNumber = "Z123456"
        mockAuthorize(lisaManagerReferenceNumber)

        val jsonString = """{"prop1": 123, "prop2": "123"}"""
        val res        = accountController
          .testJsonValidator(lisaManagerReferenceNumber)
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

        (json \ "code").as[String]    mustBe "BAD_REQUEST"
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

        (json \ "code").as[String]    mustBe "BAD_REQUEST"
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

  "The withEnrolment method" must {

    val lisaManagerReferenceNumber = "Z123456"
    val jsonString                 = """{"prop1": 1, "prop2": "val"}"""

    def doEnrolmentRequest(lmrn: String = lisaManagerReferenceNumber): Future[Result] =
      accountController
        .testJsonValidator(lmrn)
        .apply(
          FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).withBody(AnyContentAsJson(Json.parse(jsonString)))
        )

    "return 401 with ErrorInvalidLisaManager" when {

      "the authorise call returns an empty enrolment set (no HMRC-LISA-ORG enrolment)" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(Enrolments(Set.empty)))

        val res = doEnrolmentRequest()
        status(res)                              mustBe UNAUTHORIZED
        (contentAsJson(res) \ "code").as[String] mustBe ErrorInvalidLisaManager.errorCode
      }

      "the authorise call returns an enrolment with a ZREF that does not match the lisaManager" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(
            Future.successful(
              Enrolments(Set(Enrolment("HMRC-LISA-ORG", Seq(EnrolmentIdentifier("ZREF", "Z999999")), "Activated")))
            )
          )

        val res = doEnrolmentRequest()
        status(res)                              mustBe UNAUTHORIZED
        (contentAsJson(res) \ "code").as[String] mustBe ErrorInvalidLisaManager.errorCode
      }

      "the authorise call returns an enrolment with no ZREF identifier" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(
            Future.successful(Enrolments(Set(Enrolment("HMRC-LISA-ORG", Seq.empty, "Activated"))))
          )

        val res = doEnrolmentRequest()
        status(res)                              mustBe UNAUTHORIZED
        (contentAsJson(res) \ "code").as[String] mustBe ErrorInvalidLisaManager.errorCode
      }

    }

    "log the insufficient enrolments alert message exactly once, at ERROR, without an attached exception" when {

      def alertMessageMustBeLogged(): Unit =
        withCaptureOfLoggingFrom(Logger(accountController.getClass)) { logs =>
          val res = doEnrolmentRequest()
          status(res) mustBe UNAUTHORIZED

          val alertLogs = logs.filter(_.getFormattedMessage.contains(INSUFFICIENT_ENROLMENTS_ALERT_TAG))
          alertLogs.size                           mustBe 1
          alertLogs.head.getLevel                  mustBe Level.ERROR
          Option(alertLogs.head.getThrowableProxy) mustBe None
        }

      "there is no HMRC-LISA-ORG enrolment" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(Enrolments(Set.empty)))

        alertMessageMustBeLogged()
      }

      "the enrolment ZREF does not match the lisaManager" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(
            Future.successful(
              Enrolments(Set(Enrolment("HMRC-LISA-ORG", Seq(EnrolmentIdentifier("ZREF", "Z999999")), "Activated")))
            )
          )

        alertMessageMustBeLogged()
      }

      "the enrolment has no ZREF identifier" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.successful(Enrolments(Set(Enrolment("HMRC-LISA-ORG", Seq.empty, "Activated")))))

        alertMessageMustBeLogged()
      }

    }

    "not log the insufficient enrolments alert message" when {

      "the authorise call fails with an AuthorisationException" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(
            Future.failed(InsufficientConfidenceLevel("Insufficient confidence level"))
          )

        withCaptureOfLoggingFrom(Logger(accountController.getClass)) { logs =>
          status(doEnrolmentRequest()) mustBe UNAUTHORIZED

          logs.exists(_.getFormattedMessage.contains(INSUFFICIENT_ENROLMENTS_ALERT_TAG)) mustBe false
        }
      }

      "the enrolment matches and the request succeeds" in {
        mockAuthorize(lisaManagerReferenceNumber)

        withCaptureOfLoggingFrom(Logger(accountController.getClass)) { logs =>
          status(doEnrolmentRequest()) must not be UNAUTHORIZED

          logs.exists(_.getFormattedMessage.contains(INSUFFICIENT_ENROLMENTS_ALERT_TAG)) mustBe false
        }
      }

    }

    "use the alert tag expected by the lisa-api log message alert in hmrc/alert-config" in {
      INSUFFICIENT_ENROLMENTS_ALERT_TAG mustBe "INSUFFICIENT_LISA_ENROLMENTS"
    }

    "return 401 with ErrorUnauthorized" when {

      "the authorise call throws an AuthorisationException" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(
            Future.failed(InsufficientConfidenceLevel("Insufficient confidence level"))
          )

        val res = doEnrolmentRequest()
        status(res)                              mustBe UNAUTHORIZED
        (contentAsJson(res) \ "code").as[String] mustBe ErrorUnauthorized.errorCode
      }

    }

    "return 500 with ErrorInternalServerError" when {

      "the authorise call throws an unexpected exception" in {
        when(mockAuthConnector.authorise[Enrolments](any(), any())(any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Unexpected error")))

        val res = doEnrolmentRequest()
        status(res) mustBe INTERNAL_SERVER_ERROR
      }

    }

    "return 400 Bad Request with empty JSON error" when {

      "the request has no JSON body" in {
        mockAuthorize(lisaManagerReferenceNumber)

        val res = accountController
          .testJsonValidator(lisaManagerReferenceNumber)
          .apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader))

        status(res) mustBe BAD_REQUEST
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

        (json \ "code").as[String]    mustBe "BAD_REQUEST"
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
