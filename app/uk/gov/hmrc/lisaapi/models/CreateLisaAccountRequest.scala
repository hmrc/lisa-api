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

import java.time.LocalDate

sealed trait CreateLisaAccountRequest

case class CreateLisaAccountCreationRequest(
  investorId: InvestorId,
  accountId: AccountId,
  firstSubscriptionDate: LocalDate
) extends CreateLisaAccountRequest

case class CreateLisaAccountTransferRequest(
  creationReason: String,
  investorId: InvestorId,
  accountId: AccountId,
  firstSubscriptionDate: LocalDate,
  transferAccount: AccountTransfer
) extends CreateLisaAccountRequest

//scalastyle:off multiple.string.literals
object CreateLisaAccountRequest {
  implicit val createLisaAccountCreationRequestReads: Reads[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorId").read(JsonReads.investorId) and
      (JsPath \ "accountId").read(JsonReads.accountId) and
      (JsPath \ "firstSubscriptionDate").read(JsonReads.notFutureDate) and
      (JsPath \ "creationReason").read[String](Reads.pattern("New".r, "error.formatting.creationReason"))
  )((investorId, accountId, firstSubscriptionDate, _) =>
    CreateLisaAccountCreationRequest(investorId, accountId, firstSubscriptionDate)
  )

  implicit val createLisaAccountTransferRequestReads: Reads[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorId").read(JsonReads.investorId) and
      (JsPath \ "accountId").read(JsonReads.accountId) and
      (JsPath \ "firstSubscriptionDate").read(JsonReads.notFutureDate) and
      (JsPath \ "transferAccount").read[AccountTransfer] and
      (JsPath \ "creationReason").read[String](
        Reads.pattern(
          "Transferred|Current year funds transferred|Previous year funds transferred".r,
          "error.formatting.creationReason"
        )
      )
  )((investorId, accountId, firstSubscriptionDate, transferAccount, creationReason) =>
    CreateLisaAccountTransferRequest(creationReason, investorId, accountId, firstSubscriptionDate, transferAccount)
  )

  implicit val createLisaAccountRequestReads: Reads[CreateLisaAccountRequest] = Reads[CreateLisaAccountRequest] {
    json =>
      val creationReason = (json \ "creationReason").asOpt[String]

      creationReason match {
        case Some("Transferred") | Some("Current year funds transferred") | Some("Previous year funds transferred") =>
          createLisaAccountTransferRequestReads.reads(json)
        case _                                                                                                      =>
          createLisaAccountCreationRequestReads.reads(json)
      }
  }

  implicit val createLisaAccountCreationRequestWrites: Writes[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorID").write[InvestorId] and
      (JsPath \ "accountID").write[AccountId] and
      (JsPath \ "firstSubscriptionDate").write[LocalDate] and
      (JsPath \ "creationReason").write[String]
  ) { req: CreateLisaAccountCreationRequest => (req.investorId, req.accountId, req.firstSubscriptionDate, "New") }

  implicit val createLisaAccountTransferRequestWrites: Writes[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorID").write[InvestorId] and
      (JsPath \ "accountID").write[AccountId] and
      (JsPath \ "firstSubscriptionDate").write[LocalDate] and
      (JsPath \ "transferAccount").write[AccountTransfer] and
      (JsPath \ "creationReason").write[String]
  ) { req: CreateLisaAccountTransferRequest =>
    (
      req.investorId,
      req.accountId,
      req.firstSubscriptionDate,
      req.transferAccount,
      req.creationReason match {
        case "Current year funds transferred"  => "CurrentYearFundsTransferred"
        case "Previous year funds transferred" => "PreviousYearFundsTransferred"
        case _                                 => "Transferred"
      }
    )
  }

  implicit val createLisaAccountRequestWrites: Writes[CreateLisaAccountRequest] = Writes[CreateLisaAccountRequest] {
    case r: CreateLisaAccountCreationRequest => createLisaAccountCreationRequestWrites.writes(r)
    case r: CreateLisaAccountTransferRequest => createLisaAccountTransferRequestWrites.writes(r)
  }
}
