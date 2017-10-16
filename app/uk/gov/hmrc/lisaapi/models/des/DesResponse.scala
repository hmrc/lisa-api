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

import play.api.libs.functional.syntax._
import play.api.libs.json._

trait DesResponse

case class DesAccountResponse(accountID: String) extends DesResponse
case class DesLifeEventResponse(lifeEventID: String) extends DesResponse
case class DesLifeEventRetrievalResponse(lifeEventId: String, eventType: String, eventDate: String) extends DesResponse
case class DesCreateInvestorResponse(investorID: String) extends DesResponse
case class DesTransactionResponse(transactionID: String, message: String) extends DesResponse
case class DesFailureResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error") extends DesResponse
case object DesEmptySuccessResponse extends DesResponse

object DesResponse {
  implicit val desCreateAccountResponseFormats: OFormat[DesAccountResponse] = Json.format[DesAccountResponse]
  implicit val desCreateInvestorResponseFormats: OFormat[DesCreateInvestorResponse] = Json.format[DesCreateInvestorResponse]
  implicit val desLifeEventResponseFormats: OFormat[DesLifeEventResponse] = Json.format[DesLifeEventResponse]
  implicit val desLifeEventRetrievalResponseFormats: OFormat[DesLifeEventRetrievalResponse] = Json.format[DesLifeEventRetrievalResponse]
  implicit val desTransactionResponseFormats: OFormat[DesTransactionResponse] = Json.format[DesTransactionResponse]

  implicit val desFailureReads: Reads[DesFailureResponse] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "reason").read[String]
  )(DesFailureResponse.apply _)

  implicit val desFailureWrites: Writes[DesFailureResponse] = (
    (JsPath \ "code").write[String] and
    (JsPath \ "message").write[String]
  )(unlift(DesFailureResponse.unapply))
}