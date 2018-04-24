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

  def getTransaction(lisaManager: String, accountId: String, transactionId: String)
                     (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    desConnector.getBonusPayment(lisaManager, accountId, transactionId) flatMap {
      case bonus: DesGetBonusPaymentResponse => {
        bonus.status match {
          case "Paid" => {
            Logger.debug(s"Matched a ${bonus.status} bonus payment in ITMP")

            handleETMP(lisaManager, accountId, transactionId, bonus)
          }
          case "Pending" | "Void" | "Cancelled" => {
            Logger.debug(s"Matched a ${bonus.status} bonus payment in ITMP")

            Future.successful(GetTransactionSuccessResponse(
              transactionId = transactionId,
              bonusDueForPeriod = Some(bonus.bonuses.bonusDueForPeriod),
              paymentStatus = bonus.status
            ))
          }
          case _ => {
            Logger.warn(s"ITMP returned an unexpected status: ${bonus.status}, returning an error")

            Future.successful(GetTransactionErrorResponse)
          }
        }
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

  private def handleETMP(lisaManager: String, accountId: String, transactionId: String, bonusPayment: DesGetBonusPaymentResponse)
                        (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    val transaction: Future[DesResponse] = desConnector.getTransaction(lisaManager, accountId, transactionId)

    transaction map {
      case paid: DesGetTransactionPaid => {
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonusPayment.bonuses.bonusDueForPeriod),
          paymentStatus = "Paid",
          paymentDate = Some(paid.paymentDate),
          paymentAmount = Some(paid.paymentAmount),
          paymentReference = Some(paid.paymentReference)
        )
      }
      case pending: DesGetTransactionPending => {
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonusPayment.bonuses.bonusDueForPeriod),
          paymentStatus = "Pending",
          paymentDueDate = Some(pending.paymentDueDate),
          paymentAmount = None
        )
      }
      case error: DesFailureResponse if error.code == "NOT_FOUND" => {
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          bonusDueForPeriod = Some(bonusPayment.bonuses.bonusDueForPeriod),
          paymentStatus = "Pending"
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
