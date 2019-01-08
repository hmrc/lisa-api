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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesAccountResponse, DesEmptySuccessResponse, DesFailureResponse, DesUnavailableResponse}
import uk.gov.hmrc.lisaapi.services.AccountService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier

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
      "a DesUnavailable response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountServiceUnavailableResponse
        }
      }
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
          response mustBe CreateLisaAccountInvestorAccountAlreadyClosedResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_CANCELLED response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_CANCELLED")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountInvestorAccountAlreadyClosedResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_VOID response comes from DES" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_VOID")))

        doCreateRequest { response =>
          response mustBe CreateLisaAccountInvestorAccountAlreadyVoidResponse
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
      "a DesUnavailable response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountServiceUnavailableResponse
        }
      }
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
          response mustBe CreateLisaAccountInvestorAccountAlreadyClosedResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_CANCELLED response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_CANCELLED")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountInvestorAccountAlreadyClosedResponse
        }
      }
      "a INVESTOR_ACCOUNT_ALREADY_VOID response comes from DES" in {
        when(mockDesConnector.transferAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNT_ALREADY_VOID")))

        doTransferRequest { response =>
          response mustBe CreateLisaAccountInvestorAccountAlreadyVoidResponse
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

      "given a DesUnavailableResponse" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doCloseRequest { response =>
          response mustBe CloseLisaAccountServiceUnavailable
        }
      }

      "given failureResponse for a Account Already Void Response" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID")
            )
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountAlreadyVoidResponse
        }
      }

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

      "given failureResponse for a Account Already Cancelled Response" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED")
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

    "return a generic error response" when {

      "given an unexpected error code" in {
        when(mockDesConnector.closeAccount(any(), any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("X")
            )
          )

        doCloseRequest { response =>
          response mustBe CloseLisaAccountErrorResponse
        }
      }

    }

  }

  "Get Account" must {

    "return a Success Response" when {

      "given a success response" in {
        val successResponse = GetLisaAccountSuccessResponse("123", "456", "All funds withdrawn", new DateTime("201-04-06"), "OPEN", "AVAILABLE", None, None, None)

        when(mockDesConnector.getAccountInformation(any(), any())(any()))
          .thenReturn(Future.successful(successResponse))

        val res = Await.result(SUT.getAccount(testLMRN, "A123456")(HeaderCarrier()), Duration.Inf)

        res mustBe successResponse
      }

    }

    "return the type-appropriate response" when {

      "given a DesUnavailableResponse" in {
        when(mockDesConnector.getAccountInformation(any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        val res = Await.result(SUT.getAccount(testLMRN, "A123456")(HeaderCarrier()), Duration.Inf)

        res mustBe GetLisaAccountServiceUnavailable
      }

      "given failureResponse for a INVESTOR_ACCOUNTID_NOT_FOUND" in {
        when(mockDesConnector.getAccountInformation(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND")))

        val res = Await.result(SUT.getAccount(testLMRN, "A123456")(HeaderCarrier()), Duration.Inf)

        res mustBe GetLisaAccountDoesNotExistResponse
      }

      "given any other failureResponse" in {
        when(mockDesConnector.getAccountInformation(any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("X")))

        val res = Await.result(SUT.getAccount(testLMRN, "A123456")(HeaderCarrier()), Duration.Inf)

        res mustBe GetLisaAccountErrorResponse
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
    val request = CreateLisaAccountTransferRequest("Transferred", "1234567890", "9876543210", testDate, accountTransfer)
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
