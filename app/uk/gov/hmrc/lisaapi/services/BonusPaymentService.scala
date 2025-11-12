/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.{ExecutionContext, Future}
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

class BonusPaymentService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)(implicit
    hc: HeaderCarrier
  ): Future[RequestBonusPaymentResponse] =
    desConnector.requestBonusPayment(lisaManager, accountId, request) map {
      case successResponse: DesTransactionResponse       =>
        logger.info("[BonusPaymentService][requestBonusPayment] Matched RequestBonusPaymentSuccessResponse and the message is " + successResponse.message)
        (request.bonuses.claimReason, successResponse.message) match {
          case ("Superseded Bonus", _) => RequestBonusPaymentSupersededResponse(successResponse.transactionID)
          case (_, Some("Late"))       => RequestBonusPaymentLateResponse(successResponse.transactionID)
          case (_, _)                  => RequestBonusPaymentOnTimeResponse(successResponse.transactionID)
        }
      case conflictResponse: DesTransactionExistResponse =>
        logger.info(s"[BonusPaymentService][requestBonusPayment] Matched DesTransactionExistResponse and the code is : ${conflictResponse.code} for lisaManager : $lisaManager")
        conflictResponse.code match {
          case "BONUS_CLAIM_ALREADY_EXISTS"                   => RequestBonusPaymentClaimAlreadyExists(conflictResponse.transactionID)
          case "SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED" =>
            RequestBonusPaymentAlreadySuperseded(conflictResponse.transactionID)
        }
      case DesUnavailableResponse                        =>
        logger.warn(s"[BonusPaymentService][requestBonusPayment] Matched DesUnavailableResponse for lisaManager : $lisaManager")
        RequestBonusPaymentServiceUnavailable
      case failureResponse: DesFailureResponse           =>
        logger.warn(s"[BonusPaymentService][requestBonusPayment] Matched DesFailureResponse for lisaManager : $lisaManager and the code is : ${failureResponse.code}")
        desFailures.getOrElse(
          failureResponse.code, {
            logger.error(s"[BonusPaymentService][requestBonusPayment] Request bonus payment returned error: ${failureResponse.code} for lisaManager : $lisaManager")
            RequestBonusPaymentError
          }
        )
     }

  private val desFailures = Map[String, RequestBonusPaymentErrorResponse](
    "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID"      -> RequestBonusPaymentAccountClosedOrVoid,
    "INVESTOR_ACCOUNT_ALREADY_CLOSED"              -> RequestBonusPaymentAccountClosed,
    "INVESTOR_ACCOUNT_ALREADY_CANCELLED"           -> RequestBonusPaymentAccountCancelled,
    "INVESTOR_ACCOUNT_ALREADY_VOID"                -> RequestBonusPaymentAccountVoid,
    "LIFE_EVENT_NOT_FOUND"                         -> RequestBonusPaymentLifeEventNotFound,
    "BONUS_CLAIM_ERROR"                            -> RequestBonusPaymentBonusClaimError,
    "INVESTOR_ACCOUNTID_NOT_FOUND"                 -> RequestBonusPaymentAccountNotFound,
    "SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH"   -> RequestBonusPaymentSupersededAmountMismatch,
    "SUPERSEDING_TRANSACTION_OUTCOME_ERROR"        -> RequestBonusPaymentSupersededOutcomeError,
    "ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR" -> RequestBonusPaymentNoSubscriptions
  )

}
