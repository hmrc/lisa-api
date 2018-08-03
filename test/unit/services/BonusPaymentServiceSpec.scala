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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.{RequestBonusPaymentResponse, _}
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesGetBonusPaymentResponse, DesTransactionResponse}
import uk.gov.hmrc.lisaapi.services.BonusPaymentService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier

class BonusPaymentServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "POST bonus payment" must {

    "return a success response" when {

      "given a successful on time response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesTransactionResponse("AB123456", "On Time")))

        doRequest { response =>
          response mustBe RequestBonusPaymentOnTimeResponse("AB123456")
        }
      }

      "given a successful late notification response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesTransactionResponse("AB123456", "Late")))

        doRequest { response =>
          response mustBe RequestBonusPaymentLateResponse("AB123456")
        }
      }

    }

    "return a account closed response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_CLOSED from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountClosed
        }
      }
      "given the code INVESTOR_ACCOUNT_ALREADY_CANCELLED from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountClosed
        }
      }
    }

    "return a account void response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_VOID from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountVoid
        }
      }
    }

    "return a life event not found response" when {
      "given the code LIFE_EVENT_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_NOT_FOUND", "xx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentLifeEventNotFound
        }
      }
    }

    "return a bonus claim error response" when {
      "given the code BONUS_CLAIM_ERROR from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("BONUS_CLAIM_ERROR", "xxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentBonusClaimError
        }
      }
    }

    "return a account not found response" when {
      "given the code INVESTOR_ACCOUNTID_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND", "xxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountNotFound
        }
      }
    }

    "return a bonus claim already exists response" when {
      "given the code BONUS_CLAIM_ALREADY_EXISTS from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("BONUS_CLAIM_ALREADY_EXISTS", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentClaimAlreadyExists
        }
      }
    }

    "return a superseded bonus request amount mismatch response" when {
      "given the code SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentSupersededAmountMismatch
        }
      }
    }

    "return a superseded bonus request outcome error response" when {
      "given the code SUPERSEDING_TRANSACTION_OUTCOME_ERROR from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_TRANSACTION_OUTCOME_ERROR", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentSupersededOutcomeError
        }
      }
    }

    "return a already superseded response" when {
      "given the code SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAlreadySuperseded
        }
      }
    }

    "return a no subscriptions response" when {
      "given the code ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentNoSubscriptions
        }
      }
    }

    "return a generic error response" when {
      "given any other error code from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("SOMETHING_ELSE", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentError
        }
      }
    }

  }

  "GET bonus payment" must {

    "return success" when {
      "a valid response comes from DES" in {
        val successResponse = DesGetBonusPaymentResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0f, 10f)),
          InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
          Bonuses(1000f, 1000f, Some(1000f), "Life Event"),
          new DateTime("2017-05-05"),
          "Paid")

        when(mockDesConnector.getBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(successResponse))

        dogetBonusPaymentRequest { response =>
          response mustBe GetBonusPaymentSuccessResponse(successResponse.lifeEventId,
            successResponse.periodStartDate,
            successResponse.periodEndDate,
            successResponse.htbTransfer,
            successResponse.inboundPayments,
            successResponse.bonuses)
        }
      }
    }

    "return appropriate failure" when {

      "an invalid lisa account (investor id not found) (404) response comes from DES" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INVESTOR_ACCOUNTID_NOT_FOUND")))

        dogetBonusPaymentRequest { response =>
          response mustBe GetBonusPaymentInvestorNotFoundResponse
        }
      }

      "an invalid payment transaction (404) response comes from DES" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "TRANSACTION_ID_NOT_FOUND")))

        dogetBonusPaymentRequest { response =>
          response mustBe GetBonusPaymentTransactionNotFoundResponse
        }
      }

      "a lisaManagerReferenceNumber does not exist (400) response comes from DES" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "BAD_REQUEST")))

        dogetBonusPaymentRequest { response =>
          response mustBe GetBonusPaymentLmrnDoesNotExistResponse
        }
      }

      "an unknown error response comes from DES" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse(code = "INTERNAL_SERVER_ERROR")))

        dogetBonusPaymentRequest { response =>
          response mustBe GetBonusPaymentErrorResponse
        }
      }
    }
  }

  private def doRequest(callback: (RequestBonusPaymentResponse) => Unit) = {
    val request = RequestBonusPaymentRequest(
      lifeEventId = Some("1234567891"),
      periodStartDate = new DateTime("2017-04-06"),
      periodEndDate = new DateTime("2017-05-05"),
      htbTransfer = Some(HelpToBuyTransfer(0f, 0f)),
      inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
      bonuses = Bonuses(1000f, 1000f, None, "Life Event")
    )

    val response = Await.result(SUT.requestBonusPayment("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  private def dogetBonusPaymentRequest(callback: (GetBonusPaymentResponse) => Unit) = {
    val response = Await.result(SUT.getBonusPayment("1234567890", "9876543210", "1234")(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends BonusPaymentService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
