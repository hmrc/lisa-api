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

import java.time.LocalDate

case class AccountTransfer(
  transferredFromAccountId: AccountId,
  transferredFromLMRN: LisaManagerReferenceNumber,
  transferInDate: LocalDate
)

object AccountTransfer {
  implicit val accountTransferReads: Reads[AccountTransfer] = (
    (JsPath \ "transferredFromAccountId").read(JsonReads.accountId) and
      (JsPath \ "transferredFromLMRN").read(JsonReads.lmrn) and
      (JsPath \ "transferInDate").read(JsonReads.notFutureDate)
  )(AccountTransfer.apply _)

  implicit val accountTransferWrites: Writes[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").write[String] and
      (JsPath \ "transferredFromLMRN").write[String] and
      (JsPath \ "transferInDate").write[LocalDate]
  )(unlift(AccountTransfer.unapply))
}
