/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json.{JsPath, Reads, Writes}
import uk.gov.hmrc.lisaapi.models.des.DesResponse

import java.time.LocalDate

sealed trait GetLisaAccountResponse

case class GetLisaAccountSuccessResponse(
  accountId: String,
  investorId: String,
  creationReason: String,
  firstSubscriptionDate: LocalDate,
  accountStatus: String,
  subscriptionStatus: String,
  accountClosureReason: Option[String],
  closureDate: Option[LocalDate],
  transferAccount: Option[GetLisaAccountTransferAccount]
) extends GetLisaAccountResponse
    with DesResponse

case class GetLisaAccountTransferAccount(
  transferredFromAccountId: String,
  transferredFromLMRN: String,
  transferInDate: LocalDate
)

case object GetLisaAccountDoesNotExistResponse extends GetLisaAccountResponse
case object GetLisaAccountErrorResponse extends GetLisaAccountResponse
case object GetLisaAccountServiceUnavailable extends GetLisaAccountResponse

object GetLisaAccountTransferAccount {
  implicit val writes: Writes[GetLisaAccountTransferAccount] = (
    (JsPath \ "transferredFromAccountId").write[String] and
      (JsPath \ "transferredFromLMRN").write[String] and
      (JsPath \ "transferInDate").write[LocalDate]
  )(unlift(GetLisaAccountTransferAccount.unapply))
}

object GetLisaAccountSuccessResponse {
  implicit val getAccountSuccessReads: Reads[GetLisaAccountSuccessResponse] = (
    (JsPath \ "investorId").read(JsonReads.investorId) and
      (JsPath \ "status").read[String] and
      (JsPath \ "creationDate").read(JsonReads.isoDate) and
      (JsPath \ "creationReason").read[String] and
      (JsPath \ "accountClosureReason").readNullable[String] and
      (JsPath \ "lisaManagerClosureDate").readNullable(JsonReads.isoDate) and
      (JsPath \ "subscriptionStatus").readNullable[String] and
      (JsPath \ "firstSubscriptionDate").read(JsonReads.isoDate) and
      (JsPath \ "transferInDate").readNullable(JsonReads.isoDate) and
      (JsPath \ "xferredFromAccountId").readNullable(JsonReads.accountId) and
      (JsPath \ "xferredFromLmrn").readNullable(JsonReads.lmrn)
  )(
    (
      investorId,
      status,
      _,
      creationReason,
      accountClosureReason,
      lisaManagerClosureDate,
      subscriptionStatus,
      firstSubscriptionDate,
      transferInDate,
      xferredFromAccountId,
      xferredFromLmrn
    ) =>
      GetLisaAccountSuccessResponse(
        accountId = "",
        investorId = investorId,
        creationReason = creationReason match {
          case "NEW"                             => "New"
          case "TRANSFERRED"                     => "Transferred"
          case "CURRENT_YEAR_FUNDS_TRANSFERRED"  => "Current year funds transferred"
          case "PREVIOUS_YEAR_FUNDS_TRANSFERRED" => "Previous year funds transferred"
          case "REINSTATED"                      => "Reinstated"
        },
        firstSubscriptionDate = firstSubscriptionDate,
        accountStatus = status,
        subscriptionStatus = if (subscriptionStatus.isEmpty) "AVAILABLE" else subscriptionStatus.get,
        accountClosureReason = accountClosureReason.map {
          case "TRANSFERRED_OUT"     => "Transferred out"
          case "ALL_FUNDS_WITHDRAWN" => "All funds withdrawn"
          case "VOIDED"              => "Voided"
          case "CANCELLED"           => "Cancellation"
        },
        closureDate = lisaManagerClosureDate,
        transferAccount = (xferredFromAccountId, xferredFromLmrn, transferInDate) match {
          case (Some(accountId), Some(lmrn), Some(date)) =>
            Some(
              GetLisaAccountTransferAccount(
                transferredFromAccountId = accountId,
                transferredFromLMRN = lmrn,
                transferInDate = date
              )
            )
          case _                                         => None
        }
      )
  )

  implicit val getAccountSuccessWrites: Writes[GetLisaAccountSuccessResponse] = (
    (JsPath \ "accountId").write[String] and
      (JsPath \ "investorId").write[String] and
      (JsPath \ "creationReason").write[String] and
      (JsPath \ "firstSubscriptionDate").write[LocalDate] and
      (JsPath \ "accountStatus").write[String] and
      (JsPath \ "subscriptionStatus").write[String] and
      (JsPath \ "accountClosureReason").writeNullable[String] and
      (JsPath \ "closureDate").writeNullable[LocalDate] and
      (JsPath \ "transferAccount").writeNullable[GetLisaAccountTransferAccount]
  )(unlift(GetLisaAccountSuccessResponse.unapply))
}
