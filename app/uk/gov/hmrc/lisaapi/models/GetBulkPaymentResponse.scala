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
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models.des.DesResponse

trait GetBulkPaymentResponse extends DesResponse

case object GetBulkPaymentNotFoundResponse extends GetBulkPaymentResponse
case object GetBulkPaymentErrorResponse extends GetBulkPaymentResponse

trait BulkPayment
case class BulkPaymentPaid(paymentAmount: Amount, paymentDate: DateTime, paymentReference: String) extends BulkPayment
case class BulkPaymentPending(paymentAmount: Amount, dueDate: DateTime) extends BulkPayment
object BulkPayment {
  implicit val bpPaidReads: Reads[BulkPaymentPaid] = (
    (JsPath \ "clearedAmount").read[Amount] and
    (JsPath \ "items" \ 0 \ "clearingDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "sapDocumentNumber").read[String]
  )((amount, date, ref) => BulkPaymentPaid.apply(amount.abs, date, ref))

  implicit val bpPaidWrites: Writes[BulkPaymentPaid] = (
    (JsPath \ "paymentAmount").write[Amount] and
    (JsPath \ "paymentDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "paymentReference").write[String]
  )(unlift(BulkPaymentPaid.unapply))

  implicit val bpPendingReads: Reads[BulkPaymentPending] = (
    (JsPath \ "outstandingAmount").read[Amount] and
    (JsPath \ "items" \ 0 \ "dueDate").read(JsonReads.isoDate).map(new DateTime(_))
  )((amount, date) => BulkPaymentPending.apply(amount.abs, date))

  implicit val bpPendingWrites: Writes[BulkPaymentPending] = (
    (JsPath \ "paymentAmount").write[Amount] and
    (JsPath \ "dueDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(BulkPaymentPending.unapply))

  implicit val bpReads: Reads[BulkPayment] = Reads[BulkPayment] { json =>
    val clearingDate = (json \ "items" \ 0 \ "clearingDate").asOpt[String]

    clearingDate match {
      case Some(_) => bpPaidReads.reads(json)
      case _ => bpPendingReads.reads(json)
    }
  }

  implicit val bpWrites: Writes[BulkPayment] = Writes[BulkPayment] { bp =>
    bp match {
      case paid: BulkPaymentPaid => bpPaidWrites.writes(paid)
      case pending: BulkPaymentPending => bpPendingWrites.writes(pending)
    }
  }
}

case class GetBulkPaymentSuccessResponse(lisaManagerReferenceNumber: LisaManagerReferenceNumber,
                                         payments: List[BulkPayment]
                                        ) extends GetBulkPaymentResponse

object GetBulkPaymentResponse {

  implicit val successReads: Reads[GetBulkPaymentSuccessResponse] = (
    (JsPath \ "idNumber").read(JsonReads.lmrn) and
    (JsPath \ "financialTransactions").read[List[BulkPayment]]
  )(GetBulkPaymentSuccessResponse.apply _)

  implicit val successWrites: Writes[GetBulkPaymentSuccessResponse] = Json.writes[GetBulkPaymentSuccessResponse]

  implicit val gbpReads: Reads[GetBulkPaymentResponse] = Reads[GetBulkPaymentResponse] { json =>
    // processing date is the only required field so if it's present and the other fields aren't then
    // we can assume the request was processed correctly, but no transactions were found
    val processingDate = (json \ "processingDate").asOpt[String]

    // we need both of these to be present for a successful response
    val idNumber = (json \ "idNumber").asOpt[String]
    val financialTransactions = (json \ "financialTransactions").asOpt[JsArray]

    (processingDate, idNumber, financialTransactions) match {
      case (Some(_), Some(_), Some(_)) => successReads.reads(json)
      case (Some(_), _, _) => JsSuccess(GetBulkPaymentNotFoundResponse)
      case _ => JsError()
    }
  }

}