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
import org.scalatest.MustMatchers
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.services.TransactionService

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class TransactionServiceSpec extends PlaySpec
  with MustMatchers
  with MockitoSugar
  with OneAppPerSuite {

  "Get Transaction" must {

    "return a Pending transaction" when {
      "ITMP returns a Pending status" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Pending")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionSuccessResponse(
          transactionId = "12345",
          creationDate = new DateTime("2000-01-01"),
          bonusDueForPeriod = Some(1.0),
          status = "Pending"
        )
      }
      "ITMP returns a Paid status and ETMP returns a Pending status" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Paid")))

        when(mockDesConnector.getTransaction(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetTransactionPending(
            paymentDueDate = new DateTime("2000-01-01"),
            paymentAmount = 1.0)))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionSuccessResponse(
          transactionId = "12345",
          creationDate = new DateTime("2000-01-01"),
          bonusDueForPeriod = Some(1.0),
          status = "Pending",
          paymentDueDate = Some(new DateTime("2000-01-01")),
          paymentAmount = Some(1.0)
        )
      }
    }

    "return a Cancelled transaction" when {
      "ITMP returns a Cancelled status" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Cancelled")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionSuccessResponse(
          transactionId = "12345",
          creationDate = new DateTime("2000-01-01"),
          bonusDueForPeriod = Some(1.0),
          status = "Cancelled"
        )
      }
      "ITMP returns a Paid status and ETMP returns a Cancelled status" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Paid")))

        when(mockDesConnector.getTransaction(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetTransactionCancelled))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionSuccessResponse(
          transactionId = "12345",
          creationDate = new DateTime("2000-01-01"),
          bonusDueForPeriod = Some(1.0),
          status = "Cancelled"
        )
      }
    }

    "return a Paid transaction" when {
      "ITMP returns a Paid status and ETMP returns a Paid status" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Paid")))

        when(mockDesConnector.getTransaction(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetTransactionPaid(
            paymentDate = new DateTime("2000-01-01"),
            paymentReference = "002630000993",
            paymentAmount = 1.0)))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionSuccessResponse(
          transactionId = "12345",
          creationDate = new DateTime("2000-01-01"),
          bonusDueForPeriod = Some(1.0),
          status = "Paid",
          paymentDate = Some(new DateTime("2000-01-01")),
          paymentReference = Some("002630000993"),
          paymentAmount = Some(1.0)
        )
      }
    }

    "return a Charge transaction" when {
      "ITMP returns a Paid status and ETMP returns a charge" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Paid")))

        when(mockDesConnector.getTransaction(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetTransactionCharge(status = "Due", chargeReference = "XM002610108957")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionSuccessResponse(
          transactionId = "12345",
          creationDate = new DateTime("2000-01-01"),
          status = "Due",
          chargeReference = Some("XM002610108957")
        )
      }
    }

    "return a Transaction Not Found error" when {
      "ITMP returns a Transaction Not Found error" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("TRANSACTION_ID_NOT_FOUND")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionTransactionNotFoundResponse
      }
      "ITMP returns a Paid status and ETMP returns a Transaction Not Found error" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Paid")))

        when(mockDesConnector.getTransaction(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("TRANSACTION_ID_NOT_FOUND")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionTransactionNotFoundResponse
      }
    }

    "return a Account Not Found error" when {
      "ITMP returns a Account Not Found error" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionAccountNotFoundResponse
      }
      "ITMP returns a Paid status and ETMP returns a Account Not Found error" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Paid")))

        when(mockDesConnector.getTransaction(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionAccountNotFoundResponse
      }
    }

    "return a Generic error" when {
      "ITMP returns a status other than Pending, Paid, Void or Cancelled" in {
        when(mockDesConnector.getBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesGetBonusPaymentResponse(
            lifeEventId = None,
            periodStartDate = new DateTime("2001-01-01"),
            periodEndDate = new DateTime("2002-01-01"),
            htbTransfer = None,
            inboundPayments = InboundPayments(None, 1.0, 1.0, 1.0),
            bonuses = Bonuses(1.0, 1.0, None, "X"),
            creationDate = new DateTime("2000-01-01"),
            status = "Unknown")))

        val result = Await.result(SUT.getTransaction("123", "456", "12345")(HeaderCarrier()), Duration.Inf)

        result mustBe GetTransactionErrorResponse
      }
    }

  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends TransactionService {
    override val desConnector: DesConnector = mockDesConnector
  }

}
