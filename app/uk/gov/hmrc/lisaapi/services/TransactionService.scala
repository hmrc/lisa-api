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

package uk.gov.hmrc.lisaapi.services

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.models._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TransactionService {
  val desConnector: DesConnector

  val statusPending = "Pending"
  val statusPaid = "Paid"
  val statusVoid = "Void"
  val statusCancelled = "Cancelled"
  val statusDue = "Due"
  val statusCollected = "Collected"

  def getTransaction(lisaManager: String, accountId: String, transactionId: String)
                     (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    desConnector.getBonusOrWithdrawal(lisaManager, accountId, transactionId) flatMap {
      case bonus: GetBonusResponse => {
        handleBonusResponse(lisaManager, accountId, transactionId, bonus)
      }
      case withdrawal: GetWithdrawalResponse => {
        handleWithdrawalResponse(lisaManager, accountId, transactionId, withdrawal)
      }
      case error: DesFailureResponse => {
        Logger.debug(s"Error from ITMP: ${error.code}")

        Future.successful(
          error.code match {
            case "TRANSACTION_ID_NOT_FOUND" => GetTransactionTransactionNotFoundResponse
            case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetTransactionAccountNotFoundResponse
            case _ => {
              Logger.warn(s"Get transaction returned error: ${error.code} from ITMP")
              GetTransactionErrorResponse
            }
          }
        )
      }
    }
  }

  private def handleBonusResponse(lisaManager: String, accountId: String, transactionId: String, bonus: GetBonusResponse)
                                 (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {
    bonus.paymentStatus match {
      case `statusPending` | `statusVoid` | `statusCancelled` => {
        Logger.debug(s"Matched a ${bonus.paymentStatus} bonus payment in ITMP")

        Future.successful(GetTransactionBonusSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonus.bonuses.bonusDueForPeriod),
          paymentStatus = bonus.paymentStatus
        ))
      }
      case `statusPaid` => {
        Logger.debug(s"Matched a ${bonus.paymentStatus} bonus payment in ITMP")

        handlePaidBonus(lisaManager, accountId, transactionId, bonus)
      }
      case _ => {
        Logger.warn(s"ITMP returned an unexpected status for a bonus claim: ${bonus.paymentStatus}, returning an error")

        Future.successful(GetTransactionErrorResponse)
      }
    }
  }

  private def handleWithdrawalResponse(lisaManager: String, accountId: String, transactionId: String, withdrawal: GetWithdrawalResponse)
                                      (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {
    withdrawal.paymentStatus match {
      case `statusDue` => {
        Logger.debug(s"Matched a ${withdrawal.paymentStatus} withdrawal charge in ITMP")

        Future.successful(GetTransactionWithdrawalSuccessResponse(
          transactionId = transactionId,
          paymentStatus = withdrawal.paymentStatus
        ))
      }
      case `statusCollected` => {
        handleCollectedWithdrawal(lisaManager, accountId, transactionId)
      }
      case _ => {
        Logger.warn(s"ITMP returned an unexpected status for a withdrawal charge: ${withdrawal.paymentStatus}, returning an error")

        Future.successful(GetTransactionErrorResponse)
      }
    }
  }

  private def handleCollectedWithdrawal(lisaManager: String, accountId: String, transactionId: String)
                                       (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    val transaction: Future[DesResponse] = desConnector.getTransaction(lisaManager, accountId, transactionId)

    transaction map {
      case collected: DesGetTransactionCollected => {
        GetTransactionWithdrawalSuccessResponse(
          transactionId = transactionId,
          paymentStatus = statusCollected,
          paymentDate = Some(collected.paymentDate),
          paymentAmount = Some(collected.paymentAmount),
          paymentReference = Some(collected.paymentReference)
        )
      }
      case due: DesGetTransactionDue => {
        GetTransactionWithdrawalSuccessResponse(
          transactionId = transactionId,
          paymentStatus = statusDue,
          paymentDueDate = Some(due.paymentDueDate),
          paymentAmount = None
        )
      }
      case error: DesFailureResponse if error.code == "NOT_FOUND" => {
        GetTransactionWithdrawalSuccessResponse(
          transactionId = transactionId,
          paymentStatus = statusDue
        )
      }
      case _ => {
        GetTransactionErrorResponse
      }
    }
  }

  private def handlePaidBonus(lisaManager: String, accountId: String, transactionId: String, bonusPayment: GetBonusResponse)
                             (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    val transaction: Future[DesResponse] = desConnector.getTransaction(lisaManager, accountId, transactionId)

    transaction map {
      case paid: DesGetTransactionPaid => {
        GetTransactionBonusSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonusPayment.bonuses.bonusDueForPeriod),
          paymentStatus = statusPaid,
          paymentDate = Some(paid.paymentDate),
          paymentAmount = Some(paid.paymentAmount),
          paymentReference = Some(paid.paymentReference)
        )
      }
      case pending: DesGetTransactionPending => {
        GetTransactionBonusSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonusPayment.bonuses.bonusDueForPeriod),
          paymentStatus = statusPending,
          paymentDueDate = Some(pending.paymentDueDate),
          paymentAmount = None
        )
      }
      case error: DesFailureResponse if error.code == "NOT_FOUND" => {
        GetTransactionBonusSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonusPayment.bonuses.bonusDueForPeriod),
          paymentStatus = statusPending
        )
      }
      case error: DesFailureResponse => {
        Logger.warn(s"Get transaction returned error: ${error.code} from ETMP")

        GetTransactionErrorResponse
      }
    }
  }

}

object TransactionService extends TransactionService {
  override val desConnector: DesConnector = DesConnector
}
