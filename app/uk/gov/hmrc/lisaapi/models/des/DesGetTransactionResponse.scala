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

package uk.gov.hmrc.lisaapi.models.des

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsError, JsPath, JsSuccess, Reads}
import uk.gov.hmrc.lisaapi.models.{Amount, JsonReads}

import java.time.LocalDate

trait DesGetTransactionResponse extends DesResponse

case class DesGetTransactionPending(
  paymentDueDate: LocalDate,
  paymentReference: Option[String] = None,
  paymentAmount: Option[Amount] = None
) extends DesGetTransactionResponse
case class DesGetTransactionPaid(paymentDate: LocalDate, paymentReference: String, paymentAmount: Amount)
    extends DesGetTransactionResponse

object DesGetTransactionResponse {
  implicit val pendingReads: Reads[DesGetTransactionPending] = (
    (JsPath \ "paymentDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentReference").readNullable[String] and
      (JsPath \ "paymentAmount").readNullable[Amount]
  )(DesGetTransactionPending.apply _)

  implicit val paidReads: Reads[DesGetTransactionPaid] = (
    (JsPath \ "paymentDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentReference").read[String] and
      (JsPath \ "paymentAmount").read[Amount]
  )(DesGetTransactionPaid.apply _)

  implicit val reads: Reads[DesGetTransactionResponse] = Reads[DesGetTransactionResponse] { json =>
    (json \ "paymentStatus").validate[String] match {
      case JsSuccess(paymentStatus, _) => paymentStatus match {
        case "PENDING" => pendingReads.reads(json)
        case "PAID" => paidReads.reads(json)
      }
      case JsError(errors) => JsError(s"Unknown type: ${errors.mkString(", ")}")
    }
  }
}
