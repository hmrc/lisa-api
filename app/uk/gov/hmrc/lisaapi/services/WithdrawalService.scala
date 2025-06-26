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

package uk.gov.hmrc.lisaapi.services

import com.google.inject.Inject
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.{ExecutionContext, Future}

class WithdrawalService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def reportWithdrawalCharge(lisaManager: String, accountId: String, request: ReportWithdrawalChargeRequest)(implicit
    hc: HeaderCarrier
  ): Future[ReportWithdrawalChargeResponse] =
    desConnector.reportWithdrawalCharge(lisaManager, accountId, request) map {
      case successResponse: DesTransactionResponse                                 =>
        logger.info(s"[WithdrawalService][reportWithdrawalCharge] Matched ReportWithdrawalChargeSuccessResponse and the message is : ${successResponse.message} for lisaManager: $lisaManager")
        request match {
          case _: RegularWithdrawalChargeRequest    =>
            if (successResponse.message.contains("Late")) {
              ReportWithdrawalChargeLateResponse(successResponse.transactionID)
            } else {
              ReportWithdrawalChargeOnTimeResponse(successResponse.transactionID)
            }
          case _: SupersededWithdrawalChargeRequest =>
            ReportWithdrawalChargeSupersededResponse(successResponse.transactionID)
        }
      case DesUnavailableResponse                                                  =>
        logger.warn(s"[WithdrawalService][reportWithdrawalCharge] Matched DesUnavailableResponse for lisaManager: $lisaManager")
        ReportWithdrawalChargeServiceUnavailable
      case alreadyExistsResponse: DesWithdrawalChargeAlreadyExistsResponse         =>
        logger.warn(s"[WithdrawalService][reportWithdrawalCharge] Matched DesWithdrawalChargeAlreadyExistsResponse and the code is : ${alreadyExistsResponse.code} for lisaManager: $lisaManager")
        ReportWithdrawalChargeAlreadyExists(alreadyExistsResponse.investorTransactionID)
      case alreadySupersededResponse: DesWithdrawalChargeAlreadySupersededResponse =>
        logger.warn(s"[WithdrawalService][reportWithdrawalCharge] Matched DesWithdrawalChargeAlreadySupersededResponse and the code is: ${alreadySupersededResponse.code} for lisaManager: $lisaManager")
        ReportWithdrawalChargeAlreadySuperseded(alreadySupersededResponse.supersededTransactionByID)
      case failureResponse: DesFailureResponse                                     =>
        logger.warn(s"[WithdrawalService][reportWithdrawalCharge] Matched DesFailureResponse and the code is :${failureResponse.code} for lisaManager: $lisaManager")
        desFailures.getOrElse(
          failureResponse.code, {
            logger.error(s"[WithdrawalService][reportWithdrawalCharge] Request bonus payment returned error: ${failureResponse.code} for lisaManager: $lisaManager")
            ReportWithdrawalChargeError
          }
        )
    }

  private val desFailures = Map[String, ReportWithdrawalChargeErrorResponse](
    "INVESTOR_ACCOUNTID_NOT_FOUND"               -> ReportWithdrawalChargeAccountNotFound,
    "INVESTOR_ACCOUNT_ALREADY_VOID"              -> ReportWithdrawalChargeAccountVoid,
    "INVESTOR_ACCOUNT_ALREADY_CANCELLED"         -> ReportWithdrawalChargeAccountCancelled,
    "WITHDRAWAL_REPORTING_ERROR"                 -> ReportWithdrawalChargeReportingError,
    "SUPERSEDING_TRANSACTION_ID_AMOUNT_MISMATCH" -> ReportWithdrawalChargeSupersedeAmountMismatch,
    "SUPERSEDING_TRANSACTION_OUTCOME_ERROR"      -> ReportWithdrawalChargeSupersedeOutcomeError
  )
}
