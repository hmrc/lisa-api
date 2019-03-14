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

import org.mockito.Matchers.{eq => matchersEquals, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.controllers.{CloseAccountController, ErrorBadRequestAccountId, ErrorBadRequestLmrn}
import uk.gov.hmrc.lisaapi.metrics.LisaMetrics
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CloseAccountControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach with Injecting {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"

  val validDate = "2017-04-06"
  val invalidDate = "2015-04-05"

  val closeAccountJson = s"""{"accountClosureReason" : "All funds withdrawn", "closureDate" : "$validDate"}"""

  val mockAuthCon = mock[AuthConnector]

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockService)
  }

  "The Close Account endpoint" must {

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))

    "audit an accountClosed event" when {
      "return with status 200 ok" when {
        "submitted a valid close account request" in {
          when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse(accountId)))

          doSyncCloseRequest(closeAccountJson) { res =>
            verify(mockAuditService).audit(
              auditType = matchersEquals("accountClosed"),
              path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
              auditData = matchersEquals(Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountClosureReason" -> "All funds withdrawn",
                "closureDate" -> validDate,
                "accountId" -> accountId
              )))(any())
          }
        }
      }
    }

    "audit an accountNotClosed event" when {
      "the json fails date validation" in {
        doSyncCloseRequest(closeAccountJson.replace(validDate, invalidDate)) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> invalidDate,
              "accountId" -> accountId,
              "reasonNotClosed" -> "FORBIDDEN"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountAlreadyVoidResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyVoidResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INVESTOR_ACCOUNT_ALREADY_VOID"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyClosedResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountCancellationPeriodExceeded" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountCancellationPeriodExceeded))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "CANCELLATION_PERIOD_EXCEEDED"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountWithinCancellationPeriod" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountWithinCancellationPeriod))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "ACCOUNT_WITHIN_CANCELLATION_PERIOD"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountNotFoundResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountNotFoundResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            )))(any())

        }
      }
      "the data service returns a CloseLisaAccountErrorResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountErrorResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INTERNAL_SERVER_ERROR"
            )))(any())

        }
      }
    }

    "return with status 200 ok" when {
      "submitted a valid close account request" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse(accountId)))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (OK)
        }
      }
    }

    "return with status 403 forbidden and a code of FORBIDDEN" when {
      "given a closure date prior to 6 April 2017" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse(accountId)))

        val json = closeAccountJson.replace(validDate, "2017-04-05")

        doCloseRequest(json) { res =>
          status(res) mustBe (FORBIDDEN)

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The closureDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/closureDate"
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" when {
      "the data service returns a CloseLisaAccountAlreadyVoidResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyVoidResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_VOID")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyClosedResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }

    "return with status 403 forbidden and a code of CANCELLATION_PERIOD_EXCEEDED" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountCancellationPeriodExceeded))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("CANCELLATION_PERIOD_EXCEEDED")
        }
      }
    }

    "return with status 403 forbidden and a code of ACCOUNT_WITHIN_CANCELLATION_PERIOD" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountWithinCancellationPeriod))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("ACCOUNT_WITHIN_CANCELLATION_PERIOD")
        }
      }
    }

    "return with status 404 forbidden and a code of INVESTOR_ACCOUNTID_NOT_FOUND" when {
      "the data service returns a CloseLisaAccountNotFoundResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountNotFoundResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (NOT_FOUND)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
        }
      }
    }

    "return with status 400 bad request" when {
      "submitted an invalid close account request" in {
        doCloseRequest(closeAccountJson.replace("All funds withdrawn", "X")) { res =>
          status(res) mustBe (BAD_REQUEST)
        }
      }
      "submitted an invalid lmrn" in {
        doCloseRequest(closeAccountJson, "Z12345") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "submitted an invalid accountId" in {
        doCloseRequest(closeAccountJson, accId = "1=2!") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }

    "return with status 500 internal server error" when {
      "the data service returns an error" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountErrorResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
      "an exception is thrown" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenThrow(new RuntimeException("Test"))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }

    "return with status 503 service unavailable" when {
      "the data service returns an error" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountServiceUnavailable))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe SERVICE_UNAVAILABLE
          (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        }
      }
    }

  }

  def doCloseRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId)(callback: (Future[Result]) => Unit) {
    val res = SUT.closeLisaAccount(lmrn, accId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncCloseRequest(jsonString: String)(callback: Result => Unit) {
    val res = await(SUT.closeLisaAccount(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  val mockService: AccountService = mock[AccountService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAppContext: AppContext = mock[AppContext]
  val mockLisaMetrics: LisaMetrics = mock[LisaMetrics]
  val mockControllerComponents = inject[ControllerComponents]

  val SUT = new CloseAccountController(mockAuthCon, mockAppContext, mockAuditService, mockService, mockLisaMetrics, mockControllerComponents) {
    override lazy val v2endpointsEnabled = true
  }

}
