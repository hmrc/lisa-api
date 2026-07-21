/*
 * Copyright 2026 HM Revenue & Customs
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
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.RoutingConnector
import uk.gov.hmrc.lisaapi.models.*
import uk.gov.hmrc.lisaapi.models.des.*
import uk.gov.hmrc.lisaapi.models.hip.*

import scala.concurrent.{ExecutionContext, Future}

class TransactionService @Inject() (connector: RoutingConnector)(implicit ec: ExecutionContext) extends Logging {

  def getTransaction(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetTransactionResponse] =
    connector.getBonusOrWithdrawal(lisaManager, accountId, transactionId) flatMap {
      case success: GetBonusOrWithdrawalSuccessResponse =>
        handleITMPResponse(lisaManager, accountId, transactionId, success)
      case DesUnavailableResponse                       =>
        logger.warn(
          s"[TransactionService][getTransaction] ITMP returned with 503 for getTransaction, lisaManager is $lisaManager"
        )
        Future.successful(GetTransactionServiceUnavailableResponse)
      case error: DesFailureResponse                    =>
        logger.error(
          s"[TransactionService][getTransaction] Error from ITMP: ${error.code} for getTransaction, lisaManager is $lisaManager"
        )
        Future.successful(
          error.code match {
            case "TRANSACTION_ID_NOT_FOUND"     => GetTransactionTransactionNotFoundResponse
            case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetTransactionAccountNotFoundResponse
            case _                              =>
              logger.error(
                s"[TransactionService][getTransaction] Get transaction returned error: ${error.code} from ITMP for lisaManager $lisaManager"
              )
              GetTransactionErrorResponse
          }
        )
    }

  private def handleITMPResponse(
    lisaManager: String,
    accountId: String,
    transactionId: String,
    itmpResponse: GetBonusOrWithdrawalSuccessResponse
  )(implicit hc: HeaderCarrier): Future[GetTransactionResponse] = {
    logger.info(
      s"[TransactionService][handleITMPResponse] Matched a ${itmpResponse.paymentStatus} transaction from ITMP"
    )
    itmpResponse.paymentStatus match {
      case TransactionPaymentStatus.PENDING | TransactionPaymentStatus.DUE | TransactionPaymentStatus.VOID |
          TransactionPaymentStatus.CANCELLED =>
        logger.info(
          s"[TransactionService][handleITMPResponse] Received transaction payment status as cancelled from ITMP for lisaManager : $lisaManager"
        )
        Future.successful(
          GetTransactionSuccessResponse(
            transactionId = transactionId,
            paymentStatus = itmpResponse.paymentStatus,
            bonusDueForPeriod = itmpResponse.getBonusDueForPeriod
          )
        )
      case TransactionPaymentStatus.SUPERSEDED =>
        logger.info(
          s"[TransactionService][handleITMPResponse] Received transaction payment status as superseded from ITMP for lisaManager : $lisaManager"
        )
        Future.successful(
          GetTransactionSuccessResponse(
            transactionId = transactionId,
            paymentStatus = itmpResponse.paymentStatus,
            supersededBy = itmpResponse.supersededBy
          )
        )
      case TransactionPaymentStatus.PAID       =>
        logger.info(
          s"[TransactionService][handleITMPResponse] Received transaction payment status as paid from ITMP for lisaManager : $lisaManager"
        )
        handlePaidTransaction(lisaManager, accountId, transactionId, itmpResponse.getBonusDueForPeriod)
      case TransactionPaymentStatus.COLLECTED  =>
        logger.info(
          s"[TransactionService][handleITMPResponse] Received transaction payment status as collected from ITMP for lisaManager : $lisaManager"
        )
        handleCollectedTransaction(lisaManager, accountId, transactionId)
      case _                                   =>
        logger.error(
          s"[TransactionService][handleITMPResponse] Unexpected status: ${itmpResponse.paymentStatus}, returning an error from ITMP for lisaManager : $lisaManager"
        )
        Future.successful(GetTransactionErrorResponse)
    }
  }

  private def handleCollectedTransaction(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetTransactionResponse] =
    connector.getTransaction(lisaManager, accountId, transactionId) map {
      /* -----------------------------------------------------------------------------------------------------------
                DES RESPONSES
       -------------------------------------------------------------------------------------------------------------*/
      case DesUnavailableResponse           =>
        logger.warn(
          s"[TransactionService][handleCollectedTransaction] Matched DesUnavailableResponse for lisaManager : $lisaManager"
        )
        GetTransactionServiceUnavailableResponse
      case collected: DesGetTransactionPaid =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.COLLECTED,
          paymentDate = Some(collected.paymentDate),
          paymentAmount = Some(collected.paymentAmount),
          paymentReference = Some(collected.paymentReference),
          transactionType = Some(TransactionPaymentType.DEBT)
        )
      case due: DesGetTransactionPending    =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.DUE,
          paymentDueDate = Some(due.paymentDueDate),
          transactionType = Some(TransactionPaymentType.DEBT),
          paymentAmount = due.paymentAmount,
          paymentReference = due.paymentReference
        )
      case error: DesFailureResponse        =>
        error.code match {
          case "NOT_FOUND" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.DUE
            )
          case _           =>
            logger.error(
              s"[TransactionService][handleCollectedTransaction] Get collected transaction returned error: ${error.code} from ETMP for lisaManager : $lisaManager"
            )
            GetTransactionErrorResponse
        }
      /* -----------------------------------------------------------------------------------------------------------
              HIP RESPONSES
        -----------------------------------------------------------------------------------------------------------*/
      case _: HipServiceUnavailable         =>
        logger.warn(
          s"[TransactionService][handleCollectedTransaction] Matched DesUnavailableResponse for lisaManager : $lisaManager"
        )
        GetTransactionServiceUnavailableResponse
      case collected: HipGetTransactionPaid =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.COLLECTED,
          paymentDate = Some(collected.paymentDate),
          paymentAmount = Some(collected.paymentAmount),
          paymentReference = Some(collected.paymentReference),
          transactionType = Some(TransactionPaymentType.DEBT)
        )
      case due: HipGetTransactionPending    =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.DUE,
          paymentDueDate = Some(due.paymentDueDate),
          transactionType = Some(TransactionPaymentType.DEBT),
          paymentAmount = due.paymentAmount,
          paymentReference = due.paymentReference
        )
      case error: HipFailureResponse        =>
        error.`type` match {
          case "NOT_FOUND" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.DUE
            )
          case error       =>
            logger.error(
              s"[TransactionService][handleCollectedTransaction] Get collected transaction returned error: ${error.getClass.getTypeName} from ETMP for lisaManager : $lisaManager"
            )
            GetTransactionErrorResponse
        }

      case error =>
        logger.error(
          s"[TransactionService][handleCollectedTransaction] Get collected transaction returned error: ${error.getClass.getTypeName} from ETMP for lisaManager : $lisaManager"
        )
        GetTransactionErrorResponse
    }

  private def handlePaidTransaction(
    lisaManager: String,
    accountId: String,
    transactionId: String,
    bonusDueForPeriod: Option[Amount]
  )(implicit hc: HeaderCarrier): Future[GetTransactionResponse] =
    connector.getTransaction(lisaManager, accountId, transactionId) map {
      /* -----------------------------------------------------------------------------------------------------------
                    DES RESPONSES
         -----------------------------------------------------------------------------------------------------------*/
      case DesUnavailableResponse            =>
        logger.warn(
          s"[TransactionService][handlePaidTransaction] Matched DesUnavailableResponse for lisaManager : $lisaManager"
        )
        GetTransactionServiceUnavailableResponse
      case paid: DesGetTransactionPaid       =>
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
      case error: DesFailureResponse         =>
        error.code match {
          case "COULD_NOT_PROCESS" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.REFUND_CANCELLED,
              transactionType = Some(TransactionPaymentType.PAYMENT)
            )
          case "NOT_FOUND"         =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.PENDING,
              bonusDueForPeriod = bonusDueForPeriod
            )
          case _                   =>
            logger.error(
              s"[TransactionService][handlePaidTransaction] Get paid transaction returned error: ${error.code} from ETMP for lisaManager : $lisaManager"
            )
            GetTransactionErrorResponse
        }
      /* -----------------------------------------------------------------------------------------------------------
              HIP RESPONSES
        -----------------------------------------------------------------------------------------------------------*/
      case _: HipServiceUnavailable          =>
        logger.warn(
          s"[TransactionService][handlePaidTransaction] Matched HipServiceUnavailable for lisaManager : $lisaManager"
        )
        GetTransactionServiceUnavailableResponse
      case HipForbidden                      =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.REFUND_CANCELLED,
          transactionType = Some(TransactionPaymentType.PAYMENT)
        )
      case paid: HipGetTransactionPaid       =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.PAID,
          paymentDate = Some(paid.paymentDate),
          paymentAmount = Some(paid.paymentAmount),
          paymentReference = Some(paid.paymentReference),
          transactionType = Some(TransactionPaymentType.PAYMENT),
          bonusDueForPeriod = bonusDueForPeriod
        )
      case pending: HipGetTransactionPending =>
        GetTransactionSuccessResponse(
          transactionId = transactionId,
          paymentStatus = TransactionPaymentStatus.PENDING,
          paymentDueDate = Some(pending.paymentDueDate),
          paymentAmount = None,
          transactionType = Some(TransactionPaymentType.PAYMENT),
          bonusDueForPeriod = bonusDueForPeriod
        )
      case error: HipFailureResponse         =>
        error.`type` match {
          case "COULD_NOT_PROCESS" =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.REFUND_CANCELLED,
              transactionType = Some(TransactionPaymentType.PAYMENT)
            )
          case "NOT_FOUND"         =>
            GetTransactionSuccessResponse(
              transactionId = transactionId,
              paymentStatus = TransactionPaymentStatus.PENDING,
              bonusDueForPeriod = bonusDueForPeriod
            )
          case error               =>
            logger.error(
              s"[TransactionService][handlePaidTransaction] Get paid transaction returned HIP error: ${error.getClass.getTypeName} from ETMP for lisaManager : $lisaManager"
            )
            GetTransactionErrorResponse
        }
      case error                             =>
        logger.error(
          s"[TransactionService][handlePaidTransaction] Get paid transaction returned error: ${error.getClass.getTypeName} from ETMP for lisaManager : $lisaManager"
        )
        GetTransactionErrorResponse
    }

}
