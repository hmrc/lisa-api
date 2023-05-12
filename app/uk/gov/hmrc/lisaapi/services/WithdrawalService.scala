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
        logger.debug("Matched ReportWithdrawalChargeSuccessResponse and the message is " + successResponse.message)
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
        logger.debug("Matched DesUnavailableResponse")
        ReportWithdrawalChargeServiceUnavailable
      case alreadyExistsResponse: DesWithdrawalChargeAlreadyExistsResponse         =>
        logger.debug("Matched DesWithdrawalChargeAlreadyExistsResponse and the code is " + alreadyExistsResponse.code)
        ReportWithdrawalChargeAlreadyExists(alreadyExistsResponse.investorTransactionID)
      case alreadySupersededResponse: DesWithdrawalChargeAlreadySupersededResponse =>
        logger.debug(
          "Matched DesWithdrawalChargeAlreadySupersededResponse and the code is " + alreadySupersededResponse.code
        )
        ReportWithdrawalChargeAlreadySuperseded(alreadySupersededResponse.supersededTransactionByID)
      case failureResponse: DesFailureResponse                                     =>
        logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        desFailures.getOrElse(
          failureResponse.code, {
            logger.warn(s"Request bonus payment returned error: ${failureResponse.code}")
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
