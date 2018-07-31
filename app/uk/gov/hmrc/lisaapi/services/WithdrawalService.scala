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
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesTransactionResponse}
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait WithdrawalService {
  val desConnector: DesConnector

  // scalastyle:off cyclomatic.complexity
  def reportWithdrawalCharge(lisaManager: String, accountId: String, request: ReportWithdrawalChargeRequest)
                            (implicit hc: HeaderCarrier): Future[ReportWithdrawalChargeResponse] = {
    val response = desConnector.reportWithdrawalCharge(lisaManager, accountId, request)

    response map {
      case successResponse: DesTransactionResponse => {
        Logger.debug("Matched ReportWithdrawalChargeSuccessResponse and the message is " + successResponse.message)

        successResponse.message match {
          case "Late" => ReportWithdrawalChargeLateResponse(successResponse.transactionID)
          case "Superseded" => ReportWithdrawalChargeSupersededResponse(successResponse.transactionID)
          case _ => ReportWithdrawalChargeOnTimeResponse(successResponse.transactionID)
        }
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => ReportWithdrawalChargeAccountNotFound
          case "INVESTOR_ACCOUNT_ALREADY_VOID" => ReportWithdrawalChargeAccountVoid
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => ReportWithdrawalChargeAccountClosed
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => ReportWithdrawalChargeAccountCancelled
          case "WITHDRAWAL_CHARGE_ALREADY_EXISTS" => ReportWithdrawalChargeAlreadyExists
          case "WITHDRAWAL_REPORTING_ERROR" => ReportWithdrawalChargeReportingError
          case "SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED" => ReportWithdrawalChargeAlreadySuperseded
          case "SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH" => ReportWithdrawalChargeSupersedeAmountMismatch
          case "SUPERSEDING_TRANSACTION_OUTCOME_ERROR" => ReportWithdrawalChargeSupersedeOutcomeError
          case _ => {
            Logger.warn(s"Request bonus payment returned error: ${failureResponse.code}")
            ReportWithdrawalChargeError
          }
        }
      }
    }
  }

}

object WithdrawalService extends WithdrawalService {
  override val desConnector: DesConnector = DesConnector
}