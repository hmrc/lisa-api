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

case class BulkPayment(paymentDate: DateTime,
                       paymentReference: String,
                       paymentAmount: Amount)

case class GetBulkPaymentSuccessResponse(lisaManagerReferenceNumber: LisaManagerReferenceNumber,
                                         payments: List[BulkPayment]
                                        ) extends GetBulkPaymentResponse

object GetBulkPaymentResponse {

  implicit val bpReads: Reads[BulkPayment] = (
    (JsPath \ "clearingDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "paymentReference").read[String] and
    (JsPath \ "paymentAmount").read[Amount]
  )(BulkPayment.apply _)

  implicit val bpWrites: Writes[BulkPayment] = (
    (JsPath \ "paymentDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "paymentReference").write[String] and
    (JsPath \ "paymentAmount").write[Amount]
  )(unlift(BulkPayment.unapply))

  implicit val successReads: Reads[GetBulkPaymentSuccessResponse] = (
    (JsPath \ "idNumber").read(JsonReads.lmrn) and
    (JsPath \ "financialTransactions").read[List[JsValue]]
  )(
    (lmrn, transactions) =>
      GetBulkPaymentSuccessResponse(
        lmrn,
        transactions.
          flatMap(transaction =>
            (transaction \ "items").
              as[List[JsValue]].map(ob =>
                ob.asOpt[BulkPayment]
              ).
              filter(_.isDefined).
              map(_.get)
          )
      )
  )

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