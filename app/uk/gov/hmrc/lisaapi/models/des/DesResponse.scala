/*
 * Copyright 2017 HM Revenue & Customs
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
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models.{JsonReads, LifeEventId, LifeEventType}

trait DesResponse

case class DesAccountResponse(accountID: String) extends DesResponse

case class DesGetAccountResponse(
  accountId: String,
  investorId: String,
  creationReason: String,
  firstSubscriptionDate:String,
  accountStatus:String,
  accountClosureReason:Option[String],
  closureDate:Option[String],
  transferAccount: Option[DesGetAccountTransferResponse]
) extends DesResponse

case class DesGetAccountTransferResponse(
  transferredFromAccountId: String,
  transferredFromLMRN: String,
  transferInDate: DateTime
)

case class DesLifeEventResponse(lifeEventID: String) extends DesResponse
case class DesLifeEventRetrievalResponse(lifeEventID: LifeEventId, eventType: LifeEventType, eventDate: DateTime) extends DesResponse
case class DesCreateInvestorResponse(investorID: String) extends DesResponse
case class DesTransactionResponse(transactionID: String, message: String) extends DesResponse
case class DesFailureResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error") extends DesResponse
case class DesLifeEventExistResponse(code: String, reason: String, lifeEventID: String) extends DesResponse
case object DesEmptySuccessResponse extends DesResponse

object DesResponse {
  implicit val desCreateAccountResponseFormats: OFormat[DesAccountResponse] = Json.format[DesAccountResponse]

  implicit val desGetAccountTransferResponseReads: Reads[DesGetAccountTransferResponse] = (
    (JsPath \ "transferredFromAccountId").read(JsonReads.accountId) and
    (JsPath \ "transferredFromLMRN").read(JsonReads.lmrn) and
    (JsPath \ "transferInDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(DesGetAccountTransferResponse.apply _)

  implicit val desGetAccountTransferResponseWrites: Writes[DesGetAccountTransferResponse] = (
    (JsPath \ "transferredFromAccountId").write[String] and
    (JsPath \ "transferredFromLMRN").write[String] and
    (JsPath \ "transferInDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(DesGetAccountTransferResponse.unapply))

  implicit val desGetAccountResponseFormats: OFormat[DesGetAccountResponse] = Json.format[DesGetAccountResponse]
  implicit val desCreateInvestorResponseFormats: OFormat[DesCreateInvestorResponse] = Json.format[DesCreateInvestorResponse]
  implicit val desLifeEventResponseFormats: OFormat[DesLifeEventResponse] = Json.format[DesLifeEventResponse]
  implicit val desTransactionResponseFormats: OFormat[DesTransactionResponse] = Json.format[DesTransactionResponse]

  implicit val desFailureReads: Reads[DesFailureResponse] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "reason").read[String]
  )(DesFailureResponse.apply _)

  implicit val desFailureWrites: Writes[DesFailureResponse] = (
    (JsPath \ "code").write[String] and
    (JsPath \ "message").write[String]
  )(unlift(DesFailureResponse.unapply))

  implicit val requestLifeEventResponseReads: Reads[DesLifeEventRetrievalResponse] = (
    (JsPath \ "lifeEventID").read(JsonReads.lifeEventId) and
    (JsPath \ "eventType").read(JsonReads.lifeEventType) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(DesLifeEventRetrievalResponse.apply _)

  implicit val requestLifeEventAlreadyExistResponseFormats: OFormat[DesLifeEventExistResponse] = Json.format[DesLifeEventExistResponse]
}