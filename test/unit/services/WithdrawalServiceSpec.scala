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
import uk.gov.hmrc.lisaapi.models
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.services.WithdrawalService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class WithdrawalServiceSpec extends ServiceTestFixture {

  val withdrawalService: WithdrawalService = new WithdrawalService(mockDesConnector)

  "POST withdrawal report" must {

    "return a success response" when {

      "given a successful on time response from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionResponse("AB123456", Some("On Time"))))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeOnTimeResponse("AB123456")
        }
      }

      "given a successful late notification response from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionResponse("AB123456", Some("Late"))))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeLateResponse("AB123456")
        }
      }

      "given a successful late notification response from the DES connector with supersededRequest" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesTransactionResponse("AB123456", Some("Late"))))

        doSupersededRequest { response =>
          response mustBe ReportWithdrawalChargeSupersededResponse("AB123456")
        }
      }

    }

    "return a account not found response" when {
      "given the code INVESTOR_ACCOUNTID_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND", "xxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeAccountNotFound
        }
      }
    }

    "return a account void response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_VOID from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID", "xxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeAccountVoid
        }
      }
    }

    "return a account cancelled response" when {
      "given the code INVESTOR_ACCOUNT_ALREADY_CANCELLED from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED", "xxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeAccountCancelled
        }
      }
    }

    "return a charge already exists response" when {
      "given the code WITHDRAWAL_CHARGE_ALREADY_EXISTS from the DES connector" in {
        val originalTransactionId: String = "originalTransactionId"
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesWithdrawalChargeAlreadyExistsResponse("WITHDRAWAL_CHARGE_ALREADY_EXISTS", "xxxx", originalTransactionId)
          )
        )

        doRequest { response =>
          response mustBe ReportWithdrawalChargeAlreadyExists(originalTransactionId)
        }
      }
    }

    "return a reporting error response" when {
      "given the code WITHDRAWAL_REPORTING_ERROR from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("WITHDRAWAL_REPORTING_ERROR", "xxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeReportingError
        }
      }
    }

    "return a already superseded response" when {
      "given the code SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED from the DES connector" in {
        val originalTransactionId: String = "originalTransactionId"
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesWithdrawalChargeAlreadySupersededResponse(
              "SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED",
              "xxxx",
              originalTransactionId
            )
          )
        )

        doRequest { response =>
          response mustBe ReportWithdrawalChargeAlreadySuperseded(originalTransactionId)
        }
      }
    }

    "return a superseded amount mismatch response" when {
      "given the code SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH", "xxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeSupersedeAmountMismatch
        }
      }
    }

    "return a superseded transaction outcome response" when {
      "given the code SUPERSEDING_TRANSACTION_OUTCOME_ERROR from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_TRANSACTION_OUTCOME_ERROR", "xxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeSupersedeOutcomeError
        }
      }
    }

    "return a service unavailable response" when {
      "given a DesUnavailableResponse from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeServiceUnavailable
        }
      }
    }

    "return a generic error response" when {
      "given any other error code from the DES connector" in {
        when(mockDesConnector.reportWithdrawalCharge(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SOMETHING_ELSE", "xxxxx")))

        doRequest { response =>
          response mustBe ReportWithdrawalChargeError
        }
      }
    }

  }

  private def doRequest(callback: ReportWithdrawalChargeResponse => Unit): Unit = {
    val request = models.RegularWithdrawalChargeRequest(
      Some(250.00),
      LocalDate.parse("2017-12-06"),
      LocalDate.parse("2018-01-05"),
      1000.00,
      250.00,
      500.00,
      fundsDeductedDuringWithdrawal = true,
      "Regular Withdrawal"
    )

    val response = Await.result(
      withdrawalService.reportWithdrawalCharge("Z019283", "192837", request)(HeaderCarrier()),
      Duration.Inf
    )

    callback(response)
  }

  private def doSupersededRequest(callback: ReportWithdrawalChargeResponse => Unit): Unit = {
    val request = models.SupersededWithdrawalChargeRequest(
      Some(250.00),
      LocalDate.parse("2017-12-06"),
      LocalDate.parse("2018-01-05"),
      1000.00,
      250.00,
      500.00,
      fundsDeductedDuringWithdrawal = true,
      None,
      "For work"
    )

    val response = Await.result(
      withdrawalService.reportWithdrawalCharge("Z019283", "192837", request)(HeaderCarrier()),
      Duration.Inf
    )

    callback(response)
  }
}
