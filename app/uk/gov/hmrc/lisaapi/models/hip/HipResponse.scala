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
import uk.gov.hmrc.lisaapi.models.hip.{HipFailureResponse,  HipFailures, HipGetTransactionResponse, HipNotFound, HipResponse, HipServiceUnavailable}

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

//case class HipError(code: String, logID: String, message: String)

case class HipFailureResponse(`type`: String, reason: String)

//case class HipFailures(failures: Seq[HipFailureResponse])

//case class Hip422Error(code: String, processingDate: String, text: String)


trait HipFailure extends HipResponse

case class HodErrorBody(code: String, message: String, logId: String)
case class HodError(error: HodErrorBody)

case class HodErrorResponse(response: HodError) extends HipFailure

case class HipBadRequest(response: HipFailures) extends HipFailure



case class Hip422Error(processingDate: String, code: String, text: String)

case class HipValidationError(errors: Hip422Error ) extends HipFailure


case class HipServerError(response: HipFailures) extends HipFailure

case class HipServiceUnavailable(response: HipFailures) extends HipFailure
case class HipFailures(failures: Seq[HipError])
case class HipError(`type`: String, reason: String)



case object HipNotFound extends HipFailure

case object HipUnauthorized extends HipFailure
case object HipForbidden extends HipFailure

case object HipOtherErrorResponse extends HipFailure
case object HipOriginUnknown extends HipFailure


object HipError {
  implicit val hipErrorReads: Reads[HipError] = Json.reads[HipError]
}

object HipFailures {
  implicit val reads: Reads[HipFailures] = Json.reads[HipFailures]
}

object HipServiceUnavailable {
  implicit val reads: Reads[HipServiceUnavailable] = Json.reads[HipServiceUnavailable]
}


object HipServerError {
  implicit val reads: Reads[HipServerError] = Json.reads[HipServerError]
}

object Hip422Error {
  implicit val reads: Reads[Hip422Error] = Json.reads[Hip422Error]
}

object HipBadRequest {
  implicit val reads: Reads[HipBadRequest] = Json.reads[HipBadRequest]

}


object HipValidationError {
  implicit val reads: Reads[HipValidationError] = Json.reads[HipValidationError]
}

object HodErrorBody {
  implicit val reads: Reads[HodErrorBody] = Json.reads[HodErrorBody]

}

object HodError {
  implicit val reads: Reads[HodError] = Json.reads[HodError]

}

object HodErrorResponse {
  implicit val reads: Reads[HodErrorResponse] = Json.reads[HodErrorResponse]

}

