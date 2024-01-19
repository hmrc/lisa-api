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

package unit.services

import helpers.ServiceTestFixture
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesTransactionExistResponse, DesTransactionResponse, DesUnavailableResponse}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.BonusPaymentService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class BonusPaymentServiceSpec extends ServiceTestFixture {

  val bonusPaymentService: BonusPaymentService = new BonusPaymentService(mockDesConnector)

  "POST bonus payment" must {

    "return a success response" when {

      "given a successful on time response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionResponse("AB123456", Some("On Time"))))

        doRequest { response =>
          response mustBe RequestBonusPaymentOnTimeResponse("AB123456")
        }
      }

      "given a successful late notification response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionResponse("AB123456", Some("Late"))))

        doRequest { response =>
          response mustBe RequestBonusPaymentLateResponse("AB123456")
        }
      }

      "given a successful superseded response from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionResponse("AB123456", None)))

        doRequest(
          response => response mustBe RequestBonusPaymentSupersededResponse("AB123456"),
          Some(
            RequestBonusPaymentRequest(
              lifeEventId = Some("1234567891"),
              periodStartDate = LocalDate.parse("2017-04-06"),
              periodEndDate = LocalDate.parse("2017-05-05"),
              htbTransfer = Some(HelpToBuyTransfer(0f, 0f)),
              inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
              bonuses = Bonuses(1000f, 1000f, None, "Superseded Bonus")
            )
          )
        )
      }

    }

    "return a account closed or void response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountClosedOrVoid
        }
      }
    }

    "return a account closed response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_CLOSED from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountClosed
        }
      }
    }

    "return a account cancelled response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_CANCELLED from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountCancelled
        }
      }
    }

    "return a account void response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_VOID from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID", "x")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountVoid
        }
      }
    }

    "return a life event not found response" when {
      "given the code LIFE_EVENT_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_NOT_FOUND", "xx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentLifeEventNotFound
        }
      }
    }

    "return a bonus claim error response" when {
      "given the code BONUS_CLAIM_ERROR from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("BONUS_CLAIM_ERROR", "xxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentBonusClaimError
        }
      }
    }

    "return a account not found response" when {
      "given the code INVESTOR_ACCOUNTID_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND", "xxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentAccountNotFound
        }
      }
    }

    "return a bonus claim already exists response" when {
      "given the code BONUS_CLAIM_ALREADY_EXISTS from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionExistResponse("BONUS_CLAIM_ALREADY_EXISTS", "xxxxx", "987654")))

        doRequest { response =>
          response mustBe RequestBonusPaymentClaimAlreadyExists("987654")
        }
      }
    }

    "return a superseded bonus request amount mismatch response" when {
      "given the code SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentSupersededAmountMismatch
        }
      }
    }

    "return a superseded bonus request outcome error response" when {
      "given the code SUPERSEDING_TRANSACTION_OUTCOME_ERROR from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_TRANSACTION_OUTCOME_ERROR", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentSupersededOutcomeError
        }
      }
    }

    "return a already superseded response" when {
      "given the code SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesTransactionExistResponse("SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED", "xxxxx", "12345")
          )
        )

        doRequest { response =>
          response mustBe RequestBonusPaymentAlreadySuperseded("12345")
        }
      }
    }

    "return a no subscriptions response" when {
      "given the code ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentNoSubscriptions
        }
      }
    }

    "return a generic error response" when {
      "given any other error code from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SOMETHING_ELSE", "xxxxx")))

        doRequest { response =>
          response mustBe RequestBonusPaymentError
        }
      }
    }

    "return a service unavailable response" when {
      "given a DesUnavailableResponse from the DES connector" in {
        when(mockDesConnector.requestBonusPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doRequest { response =>
          response mustBe RequestBonusPaymentServiceUnavailable
        }
      }
    }

  }

  private def doRequest(
    callback: RequestBonusPaymentResponse => Unit,
    data: Option[RequestBonusPaymentRequest] = None
  ): Unit = {
    val request = data match {
      case Some(req) => req
      case None      =>
        RequestBonusPaymentRequest(
          lifeEventId = Some("1234567891"),
          periodStartDate = LocalDate.parse("2017-04-06"),
          periodEndDate = LocalDate.parse("2017-05-05"),
          htbTransfer = Some(HelpToBuyTransfer(0f, 0f)),
          inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
          bonuses = Bonuses(1000f, 1000f, None, "Life Event")
        )
    }

    val response =
      Await.result(bonusPaymentService.requestBonusPayment("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }
}
