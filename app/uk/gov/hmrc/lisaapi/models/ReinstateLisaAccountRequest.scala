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

import play.api.libs.json._

case class ReinstateLisaAccountRequest(accountId: AccountId)

object ReinstateLisaAccountRequest {

  implicit val reinstateLisaAccountRequestReads: Reads[ReinstateLisaAccountRequest] =
    (__ \ "accountId").read(JsonReads.accountId).map(accountId => ReinstateLisaAccountRequest(accountId))

  implicit val reinstateLisaAccountRequestWrites: Writes[ReinstateLisaAccountRequest] =
    (__ \ "accountId").write[String].contramap { (accountId: ReinstateLisaAccountRequest) =>
      accountId.accountId
    }

}
