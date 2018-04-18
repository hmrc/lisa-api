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

import org.mockito.Matchers.{eq => matchersEquals, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService, ReinstateAccountService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class ReinstateAccountControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val mockAuthCon = mock[LisaAuthConnector]

  override def beforeEach() {
    reset(mockAuditService)
  }

  val reinstateAccountValidJson = s"""{"accountId" :"$accountId"}"""

  "The Reinstate Account endpoint" must {

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))

    "audit an account reinstated event" when {
      "return with status 200 ok" when {
        "submitted a valid reinstate account request" in {
          when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountSuccessResponse("code", "reason")))

          doSyncReinstateAccount (reinstateAccountValidJson){ res =>
            verify(mockAuditService).audit(
              auditType = matchersEquals("accountReinstated"),
              path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
              auditData = matchersEquals(Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountId" -> accountId
               )))(any())
          }
        }
      }
    }

    "audit an accountNotReinstated event" when {
      "the data service returns a ReinstateLisaAccountAlreadyClosedResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountAlreadyClosedResponse))

        doSyncReinstateAccount(reinstateAccountValidJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotReinstated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "reasonNotReinstated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            )))(any())
        }
      }
      "the data service returns a ReinstateLisaAccountAlreadyCancelledResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountAlreadyCancelledResponse))

        doSyncReinstateAccount(reinstateAccountValidJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotReinstated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "reasonNotReinstated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            )))(any())
        }
      }
      "the data service returns a ReinstateLisaAccountAlreadyOpenResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountAlreadyOpenResponse))

        doSyncReinstateAccount(reinstateAccountValidJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotReinstated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "reasonNotReinstated" -> "INVESTOR_ACCOUNT_ALREADY_OPEN"
            )))(any())
        }
      }
      "the data service returns a ReinstateLisaAccountInvestorComplianceCheckFailedResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountInvestorComplianceCheckFailedResponse))

        doSyncReinstateAccount(reinstateAccountValidJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotReinstated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "reasonNotReinstated" -> "INVESTOR_COMPLIANCE_CHECK_FAILED"
            )))(any())
        }
      }
      "the data service returns a ReinstateLisaAccountNotFoundResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountNotFoundResponse))

        doSyncReinstateAccount(reinstateAccountValidJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotReinstated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "reasonNotReinstated" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            )))(any())

        }
      }
      "the data service returns an error" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountErrorResponse))

        doSyncReinstateAccount (reinstateAccountValidJson){ res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotReinstated"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/reinstate"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "reasonNotReinstated" -> "INTERNAL_SERVER_ERROR"
            )))(any())
        }
      }
    }

    "return with status 200 ok" when {
      "submitted a valid reinstate account request" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountSuccessResponse("code", "reason")))

        doReinstateRequest(reinstateAccountValidJson, lisaManager) { res =>
          status(res) mustBe (OK)
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" when {
      "the data service returns a ReinstateLisaAccountAlreadyClosedResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountAlreadyClosedResponse))

        doReinstateRequest (reinstateAccountValidJson, lisaManager) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CANCELLED" when {
      "the data service returns a ReinstateLisaAccountAlreadyCancelledResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountAlreadyCancelledResponse))

        doReinstateRequest (reinstateAccountValidJson, lisaManager) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_OPEN" when {
      "the data service returns a ReinstateLisaAccountAlreadyOpenResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountAlreadyOpenResponse))

        doReinstateRequest(reinstateAccountValidJson, lisaManager) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_OPEN")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_COMPLIANCE_CHECK_FAILED" when {
      "the data service returns a ReinstateLisaAccountInvestorComplianceCheckFailedResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountInvestorComplianceCheckFailedResponse))

        doReinstateRequest(reinstateAccountValidJson, lisaManager) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_COMPLIANCE_CHECK_FAILED")
        }
      }
    }

    "return with status 404 forbidden and a code of INVESTOR_ACCOUNTID_NOT_FOUND" when {
      "the data service returns a ReinstateLisaAccountNotFoundResponse" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountNotFoundResponse))

        doReinstateRequest(reinstateAccountValidJson, lisaManager) { res =>
          status(res) mustBe (NOT_FOUND)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
        }
      }
    }

    "return with status 400 bad request" when {
      "submitted an invalid lmrn" in {
        doReinstateRequest(reinstateAccountValidJson, "Z12345") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "submitted an invalid accountId" in {
        doReinstateRequest(reinstateAccountValidJson.replace(accountId, "1=2!"), lisaManager) { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorGenericBadRequest.errorCode
          (json \ "message").as[String] mustBe ErrorGenericBadRequest.message
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_FORMAT"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/accountId"
        }
      }
    }

    "return with status 500 internal server error" when {
      "the data service returns an error" in {
        when(mockService.reinstateAccountService(any(), any())(any())).thenReturn(Future.successful(ReinstateLisaAccountErrorResponse))

        doReinstateRequest(reinstateAccountValidJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }

  }

  def doReinstateRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) => Unit) {
    val res = SUT.reinstateAccount(lmrn).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncReinstateAccount(jsonString: String)(callback: (Result) => Unit) {
    val res = await(SUT.reinstateAccount(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  val mockService: ReinstateAccountService = mock[ReinstateAccountService]
  val mockAuditService: AuditService = mock[AuditService]
  val SUT = new ReinstateAccountController{
    override val service: ReinstateAccountService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
  }

}