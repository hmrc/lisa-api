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

package uk.gov.hmrc.lisaapi.services

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.{ExecutionContext, Future}

class TransactionService @Inject()(desConnector: DesConnector)(implicit ec: ExecutionContext) {

  def getTransaction(lisaManager: String, accountId: String, transactionId: String)
                     (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    desConnector.getBonusOrWithdrawal(lisaManager, accountId, transactionId) flatMap {
      case success: GetBonusOrWithdrawalSuccessResponse =>
        handleITMPResponse(lisaManager, accountId, transactionId, success)
      case DesUnavailableResponse =>
        Logger.debug("503 from ITMP")
        Future.successful(GetTransactionServiceUnavailableResponse)
      case error: DesFailureResponse =>
        Logger.debug(s"Error from ITMP: ${error.code}")
        Future.successful(
          error.code match {
            case "TRANSACTION_ID_NOT_FOUND" => GetTransactionTransactionNotFoundResponse
            case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetTransactionAccountNotFoundResponse
            case _ =>
              Logger.warn(s"Get transaction returned error: ${error.code} from ITMP")
              GetTransactionErrorResponse
          }
        )
    }
  }

  private def handleITMPResponse(lisaManager: String, accountId: String, transactionId: String, itmpResponse: GetBonusOrWithdrawalSuccessResponse)
                                (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {
    Logger.debug(s"Matched a ${itmpResponse.paymentStatus} transaction from ITMP")
    itmpResponse.paymentStatus match {
      case TransactionPaymentStatus.PENDING |
           TransactionPaymentStatus.DUE |
           TransactionPaymentStatus.VOID |
           TransactionPaymentStatus.CANCELLED =>
        Future.successful(GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = itmpResponse.paymentStatus,
          bonusDueForPeriod = itmpResponse.getBonusDueForPeriod
        ))
      case TransactionPaymentStatus.SUPERSEDED =>
        Future.successful(GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = itmpResponse.paymentStatus,
          supersededBy = itmpResponse.supersededBy
        ))
      case TransactionPaymentStatus.PAID =>
        handlePaidTransaction(lisaManager, accountId, transactionId, itmpResponse.getBonusDueForPeriod)
      case TransactionPaymentStatus.COLLECTED =>
        handleCollectedTransaction(lisaManager, accountId, transactionId)
      case _ =>
        Logger.warn(s"Unexpected status: ${itmpResponse.paymentStatus}, returning an error")
        Future.successful(GetTransactionErrorResponse)
    }
  }

  private def handleCollectedTransaction(lisaManager: String, accountId: String, transactionId: String)
                                        (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {
    desConnector.getTransaction(lisaManager, accountId, transactionId) map {
      case DesUnavailableResponse => GetTransactionServiceUnavailableResponse
      case collected: DesGetTransactionPaid =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.COLLECTED,
          paymentDate = Some(collected.paymentDate),
          paymentAmount = Some(collected.paymentAmount),
          paymentReference = Some(collected.paymentReference),
          transactionType = Some(TransactionPaymentType.DEBT)
        )
      case due: DesGetTransactionPending =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.DUE,
          paymentDueDate = Some(due.paymentDueDate),
          transactionType = Some(TransactionPaymentType.DEBT),
          paymentAmount = due.paymentAmount,
          paymentReference = due.paymentReference
        )
      case error: DesFailureResponse =>
        error.code match {
          case "NOT_FOUND" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.DUE
            )
          case _ =>
            Logger.warn(s"Get collected transaction returned error: ${error.code} from ETMP")
            GetTransactionErrorResponse
        }
    }
  }

  private def handlePaidTransaction(lisaManager: String, accountId: String, transactionId: String, bonusDueForPeriod: Option[Amount])
                                   (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    desConnector.getTransaction(lisaManager, accountId, transactionId) map {
      case DesUnavailableResponse => GetTransactionServiceUnavailableResponse
      case paid: DesGetTransactionPaid =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.PAID,
          paymentDate = Some(paid.paymentDate),
          paymentAmount = Some(paid.paymentAmount),
          paymentReference = Some(paid.paymentReference),
          transactionType = Some(TransactionPaymentType.PAYMENT),
          bonusDueForPeriod = bonusDueForPeriod
        )
      case pending: DesGetTransactionPending =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.PENDING,
          paymentDueDate = Some(pending.paymentDueDate),
          paymentAmount = None,
          transactionType = Some(TransactionPaymentType.PAYMENT),
          bonusDueForPeriod = bonusDueForPeriod
        )
      case error: DesFailureResponse =>
        error.code match {
          case "COULD_NOT_PROCESS" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.REFUND_CANCELLED,
              transactionType = Some(TransactionPaymentType.PAYMENT)
            )
          case "NOT_FOUND" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.PENDING,
              bonusDueForPeriod = bonusDueForPeriod
            )
          case _ =>
            Logger.warn(s"Get paid transaction returned error: ${error.code} from ETMP")
            GetTransactionErrorResponse
        }
    }
  }
}