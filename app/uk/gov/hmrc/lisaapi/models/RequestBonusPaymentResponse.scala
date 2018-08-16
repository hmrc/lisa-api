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

package uk.gov.hmrc.lisaapi.models

import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse

trait RequestBonusPaymentResponse

trait RequestBonusPaymentSuccessResponse extends RequestBonusPaymentResponse {
  val transactionId: String
}
case class RequestBonusPaymentLateResponse(transactionId: String) extends RequestBonusPaymentSuccessResponse
case class RequestBonusPaymentOnTimeResponse(transactionId: String) extends RequestBonusPaymentSuccessResponse
case class RequestBonusPaymentSupersededResponse(transactionId: String) extends RequestBonusPaymentSuccessResponse

trait RequestBonusPaymentErrorResponse extends RequestBonusPaymentResponse
case object RequestBonusPaymentAccountCancelled extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentAccountClosed extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentLifeEventNotFound extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentBonusClaimError extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentAccountNotFound extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentSupersededAmountMismatch extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentSupersededOutcomeError extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentNoSubscriptions extends RequestBonusPaymentErrorResponse
case object RequestBonusPaymentError extends RequestBonusPaymentErrorResponse

case class RequestBonusPaymentClaimAlreadyExists(transactionId: TransactionId) extends RequestBonusPaymentErrorResponse
case class RequestBonusPaymentAlreadySuperseded(transactionId: TransactionId) extends RequestBonusPaymentErrorResponse