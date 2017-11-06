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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.json.Json

sealed trait GetBonusPaymentResponse


case object GetBonusPaymentLmrnDoesNotExistResponse extends GetBonusPaymentResponse
case object GetBonusPaymentTransactionNotFoundResponse extends GetBonusPaymentResponse
case object GetBonusPaymentInvestorNotFoundResponse extends GetBonusPaymentResponse
case object GetBonusPaymentErrorResponse extends GetBonusPaymentResponse


case class GetBonusPaymentSuccessResponse(
                                           lifeEventId: Option[LifeEventId],
                                           periodStartDate: String,
                                           periodEndDate: String,
                                           htbTransfer: Option[HelpToBuyTransfer],
                                           inboundPayments: InboundPayments,
                                           bonuses: Bonuses) extends GetBonusPaymentResponse

object GetBonusPaymentSuccessResponse {
  implicit val format = Json.format[GetBonusPaymentSuccessResponse]
}