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

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

sealed trait GetBonusPaymentResponse


case object GetBonusPaymentLmrnDoesNotExistResponse extends GetBonusPaymentResponse
case object GetBonusPaymentTransactionNotFoundResponse extends GetBonusPaymentResponse
case object GetBonusPaymentInvestorNotFoundResponse extends GetBonusPaymentResponse
case object GetBonusPaymentErrorResponse extends GetBonusPaymentResponse


case class GetBonusPaymentSuccessResponse(
                                           lifeEventId: Option[LifeEventId],
                                           periodStartDate: DateTime,
                                           periodEndDate: DateTime,
                                           htbTransfer: Option[HelpToBuyTransfer],
                                           inboundPayments: InboundPayments,
                                           bonuses: Bonuses) extends GetBonusPaymentResponse

object GetBonusPaymentSuccessResponse {
    implicit val getBonusPaymentReads: Reads[GetBonusPaymentSuccessResponse] = (
    (JsPath \ "lifeEventId").readNullable(JsonReads.lifeEventId) and
    (JsPath \ "periodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "periodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "htbTransfer").readNullable[HelpToBuyTransfer] and
    (JsPath \ "inboundPayments").read[InboundPayments] and
    (JsPath \ "bonuses").read[Bonuses]
    )(GetBonusPaymentSuccessResponse.apply _)

  implicit val getBonusPaymentWrites: Writes[GetBonusPaymentSuccessResponse] = (
    (JsPath \ "lifeEventId").writeNullable[String] and
    (JsPath \ "periodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "periodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "htbTransfer").writeNullable[HelpToBuyTransfer] and
    (JsPath \ "inboundPayments").write[InboundPayments] and
    (JsPath \ "bonuses").write[Bonuses]
    ){req: GetBonusPaymentSuccessResponse => (req.lifeEventId, req.periodStartDate, req.periodEndDate, req.htbTransfer, req.inboundPayments, req.bonuses)}
}

