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

package uk.gov.hmrc.lisaapi.models.des

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, JsSuccess, Reads}
import uk.gov.hmrc.lisaapi.models.{Amount, JsonReads}

trait DesGetTransactionResponse extends DesResponse

case class DesGetTransactionPending(paymentDueDate: DateTime) extends DesGetTransactionResponse
case class DesGetTransactionPaid(paymentDate: DateTime, paymentReference: String, paymentAmount: Amount) extends DesGetTransactionResponse

case class DesGetTransactionDue(paymentDueDate: DateTime) extends DesGetTransactionResponse
case class DesGetTransactionCollected(paymentDate: DateTime, paymentReference: String, paymentAmount: Amount) extends DesGetTransactionResponse

object DesGetTransactionResponse {
  implicit val pendingReads: Reads[DesGetTransactionPending] = (JsPath \ "paymentDueDate").
    read(JsonReads.isoDate).map {dateString => DesGetTransactionPending(new DateTime(dateString))}

  implicit val paidReads: Reads[DesGetTransactionPaid] = (
    (JsPath \ "paymentDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "paymentReference").read[String] and
    (JsPath \ "paymentAmount").read[Amount]
  )(DesGetTransactionPaid.apply _)

  implicit val dueReads: Reads[DesGetTransactionDue] = (JsPath \ "paymentDueDate").
    read(JsonReads.isoDate).map {dateString => DesGetTransactionDue(new DateTime(dateString))}

  implicit val collectedReads: Reads[DesGetTransactionCollected] = (
    (JsPath \ "paymentDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "paymentReference").read[String] and
    (JsPath \ "paymentAmount").read[Amount]
  )(DesGetTransactionCollected.apply _)

  implicit val reads: Reads[DesGetTransactionResponse] = Reads[DesGetTransactionResponse] { json =>
    val status: String = (json \ "paymentStatus").as[String]

    status match {
      case "PENDING" => pendingReads.reads(json)
      case "PAID" => paidReads.reads(json)
      case "DUE" => dueReads.reads(json)
      case "COLLECTED" => collectedReads.reads(json)
    }
  }
}