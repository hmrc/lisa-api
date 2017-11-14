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

package uk.gov.hmrc.lisaapi.services

import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.models.{GetTransactionResponse, GetTransactionSuccessResponse}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait TransactionService {
  val desConnector: DesConnector

  def getTransaction(lisaManager: String, accountId: String, transactionId: String)
                     (implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {

    val bonus: Future[DesResponse] = desConnector.getBonusPayment(lisaManager, accountId, transactionId)

    bonus flatMap {
      case bp: DesGetBonusPaymentResponse => {

        bp.status match {
          case "Paid" => {
            val transaction: Future[DesResponse] = desConnector.getTransaction(lisaManager, accountId, transactionId)

            transaction map {
              case paid: DesGetTransactionPaid => {
                GetTransactionSuccessResponse(
                  transactionId = transactionId,
                  creationDate = bp.creationDate,
                  bonusDueForPeriod = Some(bp.bonuses.bonusDueForPeriod),
                  status = "Paid",
                  paymentDate = Some(paid.paymentDate),
                  paymentAmount = Some(paid.paymentAmount),
                  paymentReference = Some(paid.paymentReference)
                )
              }
              case pending: DesGetTransactionPending => {
                GetTransactionSuccessResponse(
                  transactionId = transactionId,
                  creationDate = bp.creationDate,
                  bonusDueForPeriod = Some(bp.bonuses.bonusDueForPeriod),
                  status = "Pending",
                  paymentDueDate = Some(pending.paymentDueDate),
                  paymentAmount = Some(pending.paymentAmount)
                )
              }
              case charge: DesGetTransactionCharge => {
                GetTransactionSuccessResponse(
                  transactionId = transactionId,
                  creationDate = bp.creationDate,
                  status = charge.status,
                  chargeReference = Some(charge.chargeReference)
                )
              }
              case DesGetTransactionCancelled => {
                GetTransactionSuccessResponse(
                  transactionId = transactionId,
                  creationDate = bp.creationDate,
                  bonusDueForPeriod = Some(bp.bonuses.bonusDueForPeriod),
                  status = "Cancelled"
                )
              }
            }

          }
          case "Pending" | "Cancelled" | "Superceded" => {
            Future.successful(GetTransactionSuccessResponse(
              transactionId = transactionId,
              creationDate = bp.creationDate,
              bonusDueForPeriod = Some(bp.bonuses.bonusDueForPeriod),
              status = bp.status
            ))
          }
        }
      }
    }
  }

}

object TransactionService extends TransactionService {
  override val desConnector: DesConnector = DesConnector
}
