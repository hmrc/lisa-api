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
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models.des.DesResponse

import java.time.LocalDate

trait GetBulkPaymentResponse extends DesResponse

case object GetBulkPaymentNotFoundResponse extends GetBulkPaymentResponse
case object GetBulkPaymentErrorResponse extends GetBulkPaymentResponse
case object GetBulkPaymentServiceUnavailableResponse extends GetBulkPaymentResponse

trait BulkPayment
case class BulkPaymentPaid(
  paymentAmount: Amount,
  paymentDate: Option[LocalDate] = None,
  paymentReference: Option[String] = None
) extends BulkPayment
case class BulkPaymentPending(paymentAmount: Amount, dueDate: Option[LocalDate] = None) extends BulkPayment
case class BulkPaymentCollected(
  paymentAmount: Amount,
  paymentDate: Option[LocalDate] = None,
  paymentReference: Option[String] = None
) extends BulkPayment
case class BulkPaymentDue(paymentAmount: Amount, dueDate: Option[LocalDate] = None) extends BulkPayment

object BulkPayment {

  implicit val bpPaidReads: Reads[BulkPaymentPaid] = (
    (JsPath \ "clearedAmount").read[Amount].map(_.abs) and
      (JsPath \ "items" \ 0 \ "clearingDate").readNullable(JsonReads.isoDate) and
      (JsPath \ "items" \ 0 \ "clearingSAPDocument").readNullable[String]
    )(BulkPaymentPaid.apply _)

  implicit val bpPaidWrites: Writes[BulkPaymentPaid] = (
    (JsPath \ "transactionType").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "paymentAmount").write[Amount] and
      (JsPath \ "paymentDate").writeNullable[LocalDate] and
      (JsPath \ "paymentReference").writeNullable[String]
  )(bp =>
    (
      "Payment",
      "Paid",
      bp.paymentAmount,
      bp.paymentDate,
      bp.paymentReference
    )
  )

  implicit val bpCollectedReads: Reads[BulkPaymentCollected] = (
    (JsPath \ "clearedAmount").read[Amount].map(_.abs) and
      (JsPath \ "items" \ 0 \ "clearingDate").readNullable(JsonReads.isoDate) and
      (JsPath \ "items" \ 0 \ "clearingSAPDocument").readNullable[String]
    )(BulkPaymentCollected.apply _)

  implicit val bpCollectedWrites: Writes[BulkPaymentCollected] = (
    (JsPath \ "transactionType").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "paymentAmount").write[Amount] and
      (JsPath \ "paymentDate").writeNullable[LocalDate] and
      (JsPath \ "paymentReference").writeNullable[String]
  )(bp =>
    (
      "Debt",
      "Collected",
      bp.paymentAmount,
      bp.paymentDate,
      bp.paymentReference
    )
  )

  implicit val bpPendingReads: Reads[BulkPaymentPending] = (
    (JsPath \ "outstandingAmount").read[Amount].map(_.abs) and
      (JsPath \ "items" \ 0 \ "dueDate").readNullable(JsonReads.isoDate)
    )(BulkPaymentPending.apply _)

  implicit val bpPendingWrites: Writes[BulkPaymentPending] = (
    (JsPath \ "transactionType").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "paymentAmount").write[Amount] and
      (JsPath \ "dueDate").writeNullable[LocalDate]
  )(bp =>
    (
      "Payment",
      "Pending",
      bp.paymentAmount,
      bp.dueDate
    )
  )

  implicit val bpDueReads: Reads[BulkPaymentDue] = (
    (JsPath \ "outstandingAmount").read[Amount].map(_.abs) and
      (JsPath \ "items" \ 0 \ "dueDate").readNullable(JsonReads.isoDate)
  )(BulkPaymentDue.apply _)

  implicit val bpDueWrites: Writes[BulkPaymentDue] = (
    (JsPath \ "transactionType").write[String] and
      (JsPath \ "status").write[String] and
      (JsPath \ "paymentAmount").write[Amount] and
      (JsPath \ "dueDate").writeNullable[LocalDate]
  )(bp =>
    (
      "Debt",
      "Due",
      bp.paymentAmount,
      bp.dueDate
    )
  )

  implicit val bpReads: Reads[BulkPayment] = Reads[BulkPayment] { json =>
    val clearingDate      = (json \ "items" \ 0 \ "clearingDate").asOpt[String]
    val outstandingAmount = (json \ "outstandingAmount").asOpt[Amount]
    val clearedAmount     = (json \ "clearedAmount").asOpt[Amount]

    (clearingDate, clearedAmount, outstandingAmount) match {
      case (Some(_), Some(clearedAmount), _) if clearedAmount < 0      => bpPaidReads.reads(json)
      case (Some(_), Some(clearedAmount), _) if clearedAmount > 0      => bpCollectedReads.reads(json)
      case (None, _, Some(outstandingAmount)) if outstandingAmount < 0 => bpPendingReads.reads(json)
      case (None, _, Some(outstandingAmount)) if outstandingAmount > 0 => bpDueReads.reads(json)
    }
  }

  implicit val bpWrites: Writes[BulkPayment] = Writes[BulkPayment] {
    case paid: BulkPaymentPaid           => bpPaidWrites.writes(paid)
    case pending: BulkPaymentPending     => bpPendingWrites.writes(pending)
    case collected: BulkPaymentCollected => bpCollectedWrites.writes(collected)
    case due: BulkPaymentDue             => bpDueWrites.writes(due)
  }
}

case class GetBulkPaymentSuccessResponse(
  lisaManagerReferenceNumber: LisaManagerReferenceNumber,
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
    val idNumber              = (json \ "idNumber").asOpt[String]
    val financialTransactions = (json \ "financialTransactions").asOpt[JsArray]

    (processingDate, idNumber, financialTransactions) match {
      case (Some(_), Some(_), Some(_)) => successReads.reads(json)
      case (Some(_), _, _)             => JsSuccess(GetBulkPaymentNotFoundResponse)
      case _                           => JsError()
    }
  }

}
