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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

sealed trait CreateLisaAccountRequest

case class CreateLisaAccountCreationRequest (
                                              investorId: InvestorId,
                                              accountId: AccountId,
                                              firstSubscriptionDate: DateTime
) extends CreateLisaAccountRequest

case class CreateLisaAccountTransferRequest (
                                              investorId: InvestorId,
                                              accountId: AccountId,
                                              firstSubscriptionDate: DateTime,
                                              transferAccount: AccountTransfer
) extends CreateLisaAccountRequest

//scalastyle:off multiple.string.literals
object CreateLisaAccountRequest {
  implicit val createLisaAccountCreationRequestReads: Reads[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorId").read(JsonReads.investorId) and
    (JsPath \ "accountId").read(JsonReads.accountId) and
    (JsPath \ "firstSubscriptionDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(CreateLisaAccountCreationRequest.apply _)

  implicit val createLisaAccountTransferRequestReads: Reads[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorId").read(JsonReads.investorId) and
    (JsPath \ "accountId").read(JsonReads.accountId) and
    (JsPath \ "firstSubscriptionDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "transferAccount").read[AccountTransfer]
  )(CreateLisaAccountTransferRequest.apply _)

  implicit val createLisaAccountRequestReads: Reads[CreateLisaAccountRequest] = Reads[CreateLisaAccountRequest] { json =>
    (json \ "creationReason").validate[String](Reads.pattern("^(New|Transferred)$".r, "error.formatting.creationReason")).flatMap {
      case "New" => createLisaAccountCreationRequestReads.reads(json)
      case "Transferred" => createLisaAccountTransferRequestReads.reads(json)
    }
  }

  implicit val createLisaAccountCreationRequestWrites: Writes[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorID").write[InvestorId] and
    (JsPath \ "accountID").write[AccountId] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "creationReason").write[String]
  ){req: CreateLisaAccountCreationRequest => (req.investorId, req.accountId, req.firstSubscriptionDate, "New")}

  implicit val createLisaAccountTransferRequestWrites: Writes[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorID").write[InvestorId] and
    (JsPath \ "accountID").write[AccountId] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "transferAccount").write[AccountTransfer] and
    (JsPath \ "creationReason").write[String]
  ){req: CreateLisaAccountTransferRequest => (req.investorId, req.accountId, req.firstSubscriptionDate, req.transferAccount, "Transferred")}

  implicit val createLisaAccountRequestWrites: Writes[CreateLisaAccountRequest] = Writes[CreateLisaAccountRequest] {
    case r: CreateLisaAccountCreationRequest => createLisaAccountCreationRequestWrites.writes(r)
    case r: CreateLisaAccountTransferRequest => createLisaAccountTransferRequestWrites.writes(r)
  }
}