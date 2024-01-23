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
import play.api.libs.json.{JsPath, Reads, Writes}

import java.time.LocalDate

case class CloseLisaAccountRequest(accountClosureReason: AccountClosureReason, closureDate: LocalDate)

object CloseLisaAccountRequest {
  implicit val closeLisaAccountRequestReads: Reads[CloseLisaAccountRequest] = (
    (JsPath \ "accountClosureReason").read(JsonReads.accountClosureReason) and
      (JsPath \ "closureDate").read(JsonReads.notFutureDate)
  )(CloseLisaAccountRequest.apply _)

  implicit val closeLisaAccountRequestWrites: Writes[CloseLisaAccountRequest] = (
    (JsPath \ "accountClosureReason").write[String] and
      (JsPath \ "closureDate").write[LocalDate]
  )(unlift(CloseLisaAccountRequest.unapply))
}
