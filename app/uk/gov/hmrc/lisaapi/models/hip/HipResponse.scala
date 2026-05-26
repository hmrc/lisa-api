/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.models.hip

import org.apache.pekko.http.scaladsl.model.HttpResponse
import play.api.libs.functional.syntax.{toAlternativeOps, toFunctionalBuilderOps}
import play.api.libs.json
import play.api.libs.json.{Format, JsError, JsPath, JsSuccess, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.lisaapi.models.{Amount, JsonReads}
import uk.gov.hmrc.lisaapi.models.des.{DesFailure, DesResponse}

import java.time.LocalDate

trait HipResponse extends RoutingResponse {

}

trait HipGetTransactionResponse(paymentStatus: String) extends HipResponse


case class HipGetTransactionPending(
                                     // paymentStatus: String = "PENDING",
                                     paymentDueDate: LocalDate,
                                   ) extends HipGetTransactionResponse("PENDING") {
  def paymentStatus = "PENDING"
}


case class HipGetTransactionPaid(
                                  //  paymentStatus: String = "PAID",
                                  paymentDate: LocalDate,
                                  paymentDueDate: LocalDate,
                                  paymentReference: String,
                                  paymentAmount: Amount) extends HipGetTransactionResponse("PAID") {

  def paymentStatus = "PAID"

}

object HipGetTransactionResponse {


  implicit val paidReads: Reads[HipGetTransactionPaid] = (
    //    (JsPath \ "paymentStatus").read[String] and
    (JsPath \ "paymentDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentDueDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentReference").read[String] and
      (JsPath \ "paymentAmount").read[Amount]
    )((paymentDate, paymentDueDate, paymentReference, paymentAmount) =>  HipGetTransactionPaid(paymentDate, paymentDueDate, paymentReference, paymentAmount))

  
//  Json.Reads[HipGetTransactionPending]
  
  implicit val pendingReads: Reads[HipGetTransactionPending] = Json.reads[HipGetTransactionPending]
//    //   (JsPath \ "paymentStatus").read[String] and
//      (JsPath \ "paymentDueDate").read(JsonReads.isoDate)
//    )((paymentDueDate) => HipGetTransactionPending(paymentDueDate))


  implicit val reads: Reads[HipGetTransactionResponse] = Reads[HipGetTransactionResponse] { json =>
    (json \ "paymentStatus").validate[String] match {
      case JsSuccess(paymentStatus, _) =>
        paymentStatus match {
          case "PENDING" => pendingReads.reads(json)
          case "PAID" => paidReads.reads(json)
          case other => JsError(s"Unknown payment status: $other")
        }
      case JsError(errors) => JsError(s"Unknown type: ${errors.mkString(", ")}")
    }
  }

}

case class HipError(code: String, logID: String, message: String)

case class Hip422Error(code: String, processingDate: String, text: String)


trait HipFailure extends HipResponse


case class HipUnavailableResponse(error: HipError) extends HipFailure

case class HipValidationErrors(errors: Seq[Hip422Error]) extends HipFailure

case class Hip4xxResponse(code: String) extends HipFailure


case class HipFailureResponse(code: String = "INTERNAL_SERVER_ERROR")
  extends HipFailure

//case object HipUnavailableResponse extends HipFailure


object HipFailureResponse {
  implicit val hipErrorReads: Reads[HipError] = (JsPath \ "error").read[HipError]
  implicit val hip422ErrorReads: Reads[Seq[Hip422Error]] = (JsPath \ "errors").read[Seq[Hip422Error]]

  implicit val hipFailureReads: Reads[HipFailureResponse] = hipErrorReads.map(
    error => HipFailureResponse(code = error.code)) orElse hip422ErrorReads.map(errors => HipFailureResponse(code = errors.map(e => e.code.mkString(",")).head))

}