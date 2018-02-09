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

trait GetBulkPaymentResponse

case object GetBulkPaymentNothingFoundResponse extends GetBulkPaymentResponse

case class BulkPayment(paymentDate: DateTime,
                       paymentReference: String,
                       paymentAmount: Amount)

case class GetBulkPaymentSuccessResponse(lisaManagerReferenceNumber: LisaManagerReferenceNumber,
                                         payments: List[BulkPayment]
                                        ) extends GetBulkPaymentResponse with DesResponse

object GetBulkPaymentResponse {
  implicit val bpReads: Reads[BulkPayment] = (
    (JsPath \ "clearingDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "paymentReference").read[String] and
    (JsPath \ "paymentAmount").read[Amount]
  )(BulkPayment.apply _)

  implicit val bpWrites = Json.writes[BulkPayment]

  implicit val successReads: Reads[GetBulkPaymentSuccessResponse] = (
    (JsPath \ "idNumber").read(JsonReads.lmrn) and
    (JsPath \ "financialTransactions").read[List[JsValue]]
  )(
    (lmrn, transactions) =>
      GetBulkPaymentSuccessResponse(
        lmrn,
        transactions.
          flatMap(transaction =>
            (transaction \ "items").as[List[BulkPayment]]
          )
      )
  )

  implicit val successWrites: Writes[GetBulkPaymentSuccessResponse] = Json.writes[GetBulkPaymentSuccessResponse]
}