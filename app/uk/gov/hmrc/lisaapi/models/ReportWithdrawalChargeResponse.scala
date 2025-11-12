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

package uk.gov.hmrc.lisaapi.models

trait ReportWithdrawalChargeResponse

trait ReportWithdrawalChargeSuccessResponse extends ReportWithdrawalChargeResponse {
  val transactionId: String
}
case class ReportWithdrawalChargeLateResponse(transactionId: String) extends ReportWithdrawalChargeSuccessResponse
case class ReportWithdrawalChargeOnTimeResponse(transactionId: String) extends ReportWithdrawalChargeSuccessResponse
case class ReportWithdrawalChargeSupersededResponse(transactionId: String) extends ReportWithdrawalChargeSuccessResponse

trait ReportWithdrawalChargeErrorResponse extends ReportWithdrawalChargeResponse
case object ReportWithdrawalChargeAccountClosed extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeAccountVoid extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeAccountCancelled extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeAccountNotFound extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeReportingError extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeSupersedeAmountMismatch extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeSupersedeOutcomeError extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeServiceUnavailable extends ReportWithdrawalChargeErrorResponse
case object ReportWithdrawalChargeError extends ReportWithdrawalChargeErrorResponse

case class ReportWithdrawalChargeAlreadySuperseded(transactionId: String) extends ReportWithdrawalChargeErrorResponse
case class ReportWithdrawalChargeAlreadyExists(transactionId: String) extends ReportWithdrawalChargeErrorResponse
