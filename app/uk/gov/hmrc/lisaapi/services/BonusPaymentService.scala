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

import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

trait BonusPaymentService {
  val desConnector: DesConnector

  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier): Future[RequestBonusPaymentResponse] = {
    val response = desConnector.requestBonusPayment(lisaManager, accountId, request)

    response map {
      case successResponse: DesTransactionResponse => {
        Logger.debug("Matched RequestBonusPaymentSuccessResponse and the message is " + successResponse.message)

        successResponse.message match {
          case "Late" => RequestBonusPaymentLateResponse(successResponse.transactionID)
          case _ => RequestBonusPaymentOnTimeResponse(successResponse.transactionID)
        }
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" => RequestBonusPaymentAccountClosed
          case "LIFE_EVENT_NOT_FOUND" => RequestBonusPaymentLifeEventNotFound
          case "BONUS_CLAIM_ERROR" => RequestBonusPaymentBonusClaimError
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => RequestBonusPaymentAccountNotFound
          case "BONUS_CLAIM_ALREADY_EXISTS" => RequestBonusPaymentClaimAlreadyExists
          case _ => RequestBonusPaymentError
        }
      }
    }
  }

  def getBonusPayment(lisaManager: String, accountId: String, transactionId: String)
                     (implicit hc: HeaderCarrier): Future[GetBonusPaymentResponse] = {


    val response: Future[DesResponse] = desConnector.getBonusPayment(lisaManager, accountId, transactionId)

    response map {
      case successResponse: DesGetBonusPaymentResponse => {
        Logger.debug("Matched DesGetBonusPaymentResponse")

        GetBonusPaymentSuccessResponse(successResponse.lifeEventId,
                                       successResponse.periodStartDate,
                                       successResponse.periodEndDate,
                                       successResponse.htbTransfer,
                                       successResponse.inboundPayments,
                                       successResponse.bonuses)
      }

      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "BONUS_PAYMENT_TRANSACTION_NOT_FOUND" => GetBonusPaymentTransactionNotFoundResponse
          case "BAD_REQUEST" => GetBonusPaymentLmrnDoesNotExistResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetBonusPaymentInvestorNotFoundResponse
          case _ => GetBonusPaymentErrorResponse
        }

      }
    }
  }

}

object BonusPaymentService extends BonusPaymentService {
  override val desConnector: DesConnector = DesConnector
}
