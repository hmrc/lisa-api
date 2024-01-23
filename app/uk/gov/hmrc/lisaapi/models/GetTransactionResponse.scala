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
import play.api.libs.json.{JsPath, Writes}

import java.time.LocalDate

trait GetTransactionResponse

case object GetTransactionErrorResponse extends GetTransactionResponse
case object GetTransactionTransactionNotFoundResponse extends GetTransactionResponse
case object GetTransactionAccountNotFoundResponse extends GetTransactionResponse
case object GetTransactionServiceUnavailableResponse extends GetTransactionResponse

case class GetTransactionSuccessResponse(
  transactionId: TransactionId,
  transactionType: Option[String] = None,
  paymentStatus: String,
  paymentDate: Option[LocalDate] = None,
  paymentDueDate: Option[LocalDate] = None,
  paymentAmount: Option[Amount] = None,
  paymentReference: Option[String] = None,
  supersededBy: Option[TransactionId] = None,
  bonusDueForPeriod: Option[Amount] = None
) extends GetTransactionResponse

object GetTransactionResponse {

  implicit val bonusSuccessWrites: Writes[GetTransactionSuccessResponse] = (
    (JsPath \ "transactionId").write[TransactionId] and
      (JsPath \ "transactionType").writeNullable[String] and
      (JsPath \ "paymentStatus").write[String] and
      (JsPath \ "paymentDate").writeNullable[LocalDate] and
      (JsPath \ "paymentDueDate").writeNullable[LocalDate] and
      (JsPath \ "paymentAmount").writeNullable[Amount] and
      (JsPath \ "paymentReference").writeNullable[String] and
      (JsPath \ "supersededBy").writeNullable[TransactionId] and
      (JsPath \ "bonusDueForPeriod").writeNullable[Amount]
    )(unlift(GetTransactionSuccessResponse.unapply))
}