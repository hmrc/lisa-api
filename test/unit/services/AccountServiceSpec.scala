/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesAccountResponse, DesEmptySuccessResponse, DesFailureResponse}
import uk.gov.hmrc.lisaapi.services.AccountService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// scalastyle:off multiple.string.literals
class AccountServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "Create Account" must {

    "return a Success Response" when {
      "a success response comes from DES" in {
        val testAccountId = "AB123456"

        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesAccountResponse(accountID = testAccountId)))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountSuccessResponse(accountId = testAccountId)
        }
      }
    }

    "return a Error Response" when {
      "a unexpected error response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "UNEXPECTED_ERROR")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
      "a NOT_FOUND response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "NOT_FOUND")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
      "a INTERNAL_SERVER_ERROR response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INTERNAL_SERVER_ERROR")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
      "a PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
    }

    "return the type-appropriate response" when {
      "a INVESTOR_NOT_FOUND response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_NOT_FOUND")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountInvestorNotFoundResponse
        }
      }
      "a INVESTOR_ELIGIBILITY_CHECK_FAILED response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ELIGIBILITY_CHECK_FAILED")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountInvestorNotEligibleResponse
        }
      }
      "a INVESTOR_COMPLIANCE_CHECK_FAILED response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_COMPLIANCE_CHECK_FAILED")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountInvestorComplianceCheckFailedResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_EXISTS response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_EXISTS")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountAlreadyExistsResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_CLOSED response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_CLOSED")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse
        }
      }
    }

  }

  "Transfer Account" must {

    "return a Success Response" when {
      "a success response comes from DES" in {
        val testAccountId = "AB123456"

        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesAccountResponse(accountID = testAccountId)))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountSuccessResponse(accountId = testAccountId)
        }
      }
    }

    "return a Error Response" when {
      "a unexpected error response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "UNEXPECTED_ERROR")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
      "a NOT_FOUND response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "NOT_FOUND")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
      "a INTERNAL_SERVER_ERROR response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INTERNAL_SERVER_ERROR")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
      "a INVESTOR_ELIGIBILITY_CHECK_FAILED response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ELIGIBILITY_CHECK_FAILED")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
    }

    "return the type-appropriate response" when {
      "a INVESTOR_NOT_FOUND response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_NOT_FOUND")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountInvestorNotFoundResponse
        }
      }
      "a INVESTOR_COMPLIANCE_CHECK_FAILED response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_COMPLIANCE_CHECK_FAILED")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountInvestorComplianceCheckFailedResponse
        }
      }
      "a PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_EXISTS response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_EXISTS")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountAlreadyExistsResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_CLOSED response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_CLOSED")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse
        }
      }
    }

  }

  "Close Account" must {

    "return a Success Response" when {

      "given no rds code and an account id" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful((
             DesEmptySuccessResponse
            ))
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountSuccessResponse("A123456")
        }
      }

    }


    "return the type-appropriate response" when {

      "given failureResponse for a Account Already Closed Response" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED")
            )
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountAlreadyClosedResponse
        }
      }

      "given failureResponse for a Account Cancellation Period Exceeded" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("CANCELLATION_PERIOD_EXCEEDED")
            )
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountCancellationPeriodExceeded
        }
      }

      "given failureResponse for a Account Within Cancellation Period" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("ACCOUNT_WITHIN_CANCELLATION_PERIOD")
            )
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountWithinCancellationPeriod
        }
      }

      "given failureResponse for a Account Bonus Payment Required" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("BONUS_REPAYMENT_REQUIRED")
            )
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountBonusPaymentRequired
        }
      }


      "given failureResponse for a Account Not Found Response" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND"))
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountNotFoundResponse
        }
      }
    }

  }

  private def doCreateRequest(callback: (CreateLisaAccountResponse) => Unit) = {
    val request = CreateLisaAccountCreationRequest("1234567890",  "9876543210", testDate)
    val response = Await.result(SUT.createAccount(testLMRN, request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  private def doTransferRequest(callback: (CreateLisaAccountResponse) => Unit) = {
    val accountTransfer = AccountTransfer("123456", "123456", testDate)
    val request = CreateLisaAccountTransferRequest("1234567890", "9876543210", testDate, accountTransfer)
    val response = Await.result(SUT.transferAccount(testLMRN, request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  private def doCloseRequest(callback: (CloseLisaAccountResponse) => Unit) = {
    val request = CloseLisaAccountRequest("All funds withdrawn", testDate)
    val response = Await.result(SUT.closeAccount(testLMRN, "A123456", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val testDate = new DateTime("2000-01-01")
  val testLMRN = "Z019283"

  val mockDesConnector: DesConnector = mock[DesConnector]

  object SUT extends AccountService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
