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
import play.api.libs.json.{JsPath, Json, Reads, Writes}

sealed trait GetLisaAccountResponse

case class GetLisaAccountSuccessResponse(
  accountId: String,
  investorId: String,
  creationReason: String,
  firstSubscriptionDate:String,
  accountStatus:String,
  subscriptionStatus:Option[String],
  accountClosureReason: Option[String],
  closureDate: Option[String],
  transferAccount: Option[GetLisaAccountTransferAccount]
) extends GetLisaAccountResponse

case class GetLisaAccountTransferAccount(
  transferredFromAccountId: String,
  transferredFromLMRN: String,
  transferInDate: DateTime
)

case object GetLisaAccountDoesNotExistResponse extends GetLisaAccountResponse
case object GetLisaAccountErrorResponse extends GetLisaAccountResponse

object GetLisaAccountTransferAccount {
  implicit val reads: Reads[GetLisaAccountTransferAccount] = (
    (JsPath \ "transferredFromAccountId").read(JsonReads.accountId) and
    (JsPath \ "transferredFromLMRN").read(JsonReads.lmrn) and
    (JsPath \ "transferInDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(GetLisaAccountTransferAccount.apply _)

  implicit val writes: Writes[GetLisaAccountTransferAccount] = (
    (JsPath \ "transferredFromAccountId").write[String] and
    (JsPath \ "transferredFromLMRN").write[String] and
    (JsPath \ "transferInDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(GetLisaAccountTransferAccount.unapply))
}

object GetLisaAccountSuccessResponse {
  implicit val format = Json.format[GetLisaAccountSuccessResponse]
}