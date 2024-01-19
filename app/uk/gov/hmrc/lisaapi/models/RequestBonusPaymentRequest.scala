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

package uk.gov.hmrc.lisaapi.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

import java.time.LocalDate

case class RequestBonusPaymentRequest(
  lifeEventId: Option[LifeEventId],
  periodStartDate: LocalDate,
  periodEndDate: LocalDate,
  htbTransfer: Option[HelpToBuyTransfer],
  inboundPayments: InboundPayments,
  bonuses: Bonuses,
  supersede: Option[Supersede] = None
)

object RequestBonusPaymentRequest {
  implicit val requestBonusPaymentReadsV2: Reads[RequestBonusPaymentRequest] = (
    (JsPath \ "lifeEventId").readNullable(JsonReads.lifeEventId) and
      (JsPath \ "periodStartDate").read(JsonReads.isoDate) and
      (JsPath \ "periodEndDate").read(JsonReads.isoDate) and
      (JsPath \ "htbTransfer").readNullable[HelpToBuyTransfer] and
      (JsPath \ "inboundPayments").read[InboundPayments] and
      (JsPath \ "bonuses").read[Bonuses] and
      (JsPath \ "supersede").readNullable[Supersede]
  )(RequestBonusPaymentRequest.apply _)

  val requestBonusPaymentReadsV1: Reads[RequestBonusPaymentRequest] = (
    (JsPath \ "lifeEventId").readNullable(JsonReads.lifeEventId) and
      (JsPath \ "periodStartDate").read(JsonReads.isoDate) and
      (JsPath \ "periodEndDate").read(JsonReads.isoDate) and
      (JsPath \ "htbTransfer").readNullable[HelpToBuyTransfer] and
      (JsPath \ "inboundPayments").read[InboundPayments] and
      (JsPath \ "bonuses").read[Bonuses](Bonuses.bonusesReadsV1)
  )((lifeEventId, periodStartDate, periodEndDate, htbTransfer, inboundPayments, bonuses) =>
    RequestBonusPaymentRequest(
      lifeEventId = lifeEventId,
      periodStartDate = periodStartDate,
      periodEndDate = periodEndDate,
      htbTransfer = htbTransfer,
      inboundPayments = inboundPayments,
      bonuses = bonuses,
      supersede = None
    )
  )

  implicit val requestBonusPaymentWrites: Writes[RequestBonusPaymentRequest] = (
    (JsPath \ "lifeEventId").writeNullable[String] and
      (JsPath \ "periodStartDate").write[LocalDate] and
      (JsPath \ "periodEndDate").write[LocalDate] and
      (JsPath \ "transactionType").write[String] and
      (JsPath \ "htbTransfer").writeNullable[HelpToBuyTransfer] and
      (JsPath \ "inboundPayments").write[InboundPayments] and
      (JsPath \ "bonuses").write[Bonuses] and
      (JsPath \ "supersededDetail").writeNullable[Supersede]
  ) { req: RequestBonusPaymentRequest =>
    (
      req.lifeEventId,
      req.periodStartDate,
      req.periodEndDate,
      "Bonus",
      req.htbTransfer,
      req.inboundPayments,
      req.bonuses,
      req.supersede
    )
  }
}
