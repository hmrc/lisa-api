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

import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json
import play.api.libs.json.*
import uk.gov.hmrc.lisaapi.models.{Amount, JsonReads}

import java.time.LocalDate

trait RoutingResponse

trait HipResponse extends RoutingResponse {}

trait HipGetTransactionResponse extends HipResponse {
  def paymentStatus: String
}

case class HipGetTransactionPending(
  paymentDueDate: LocalDate,
  paymentAmount: Option[Amount],
  paymentReference: Option[String]
) extends HipGetTransactionResponse {
  val paymentStatus = "PENDING"
}

case class HipGetTransactionPaid(
  paymentDate: LocalDate,
  paymentDueDate: LocalDate,
  paymentReference: String,
  paymentAmount: Amount
) extends HipGetTransactionResponse {
  val paymentStatus = "PAID"
}

object HipGetTransactionResponse {

  implicit val paidReads: Reads[HipGetTransactionPaid] = (
    (JsPath \ "paymentDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentDueDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentReference").read[String] and
      (JsPath \ "paymentAmount").read[Amount]
  )((paymentDate, paymentDueDate, paymentReference, paymentAmount) =>
    HipGetTransactionPaid(paymentDate, paymentDueDate, paymentReference, paymentAmount)
  )

  implicit val pendingReads: Reads[HipGetTransactionPending] = (
    (JsPath \ "paymentDueDate").read(JsonReads.isoDate) and
      (JsPath \ "paymentAmount").readNullable[Amount] and
      (JsPath \ "paymentReference").readNullable[String]

    )((paymentDueDate, paymentAmount, paymentReference) => HipGetTransactionPending(paymentDueDate, paymentAmount, paymentReference))


  implicit val hipResponseReads: Reads[HipGetTransactionResponse] = Reads[HipGetTransactionResponse] { json =>
    (json \ "success" \ "paymentStatus").validate[String] match {
      case JsSuccess(paymentStatus, _) =>
        val jsVal = (json \ "success").get
        paymentStatus match {
          case "PENDING" => pendingReads.reads(jsVal)
          case "PAID"    => paidReads.reads(jsVal)
          case other     => JsError(s"Unknown payment status: $other")
        }
      case JsError(errors)             => JsError(s"Unknown type: ${errors.mkString(", ")}")
    }
  }

}

trait HipFailure extends HipResponse

case class HipFailureResponse(`type`: String, reason: String) extends HipFailure

case class HodErrorBody(code: String, message: String, logId: String)
case class HodError(error: HodErrorBody)

case class HodErrorResponse(response: HodError) extends HipFailure

case class HipBadRequest(response: HipFailures) extends HipFailure

case class Hip422Error(processingDate: String, code: String, text: String)

case class HipValidationError(errors: Hip422Error) extends HipFailure

case class HipServerError(response: HipFailures) extends HipFailure

case class HipServiceUnavailable(response: HipFailures) extends HipFailure
case class HipFailures(failures: Seq[HipError])
case class HipError(`type`: String, reason: String)

case object HipNotFound extends HipFailure
case object HipUnauthorized extends HipFailure
case object HipForbidden extends HipFailure

case object HipOtherErrorResponse extends HipFailure
case object HipOriginUnknown extends HipFailure

object HipFailure {
  implicit val hipErrorReads: Reads[HipError]                          = Json.reads[HipError]
  implicit val hipFailureReads: Reads[HipFailures]                     = Json.reads[HipFailures]
  implicit val hpServiceUnavailableReads: Reads[HipServiceUnavailable] = Json.reads[HipServiceUnavailable]
  implicit val hipServerErrorReads: Reads[HipServerError]              = Json.reads[HipServerError]
  implicit val hip422ErrorReads: Reads[Hip422Error]                    = Json.reads[Hip422Error]
  implicit val hipBadRequestReads: Reads[HipBadRequest]                = Json.reads[HipBadRequest]
  implicit val hipValidationErrorReads: Reads[HipValidationError]      = Json.reads[HipValidationError]
  implicit val hodErrorBodyReads: Reads[HodErrorBody]                  = Json.reads[HodErrorBody]
  implicit val hodErrorReads: Reads[HodError]                          = Json.reads[HodError]
  implicit val hodErrorResponseReads: Reads[HodErrorResponse]          = Json.reads[HodErrorResponse]
}
