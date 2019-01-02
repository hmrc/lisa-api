/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{Reads, Writes, __}
import uk.gov.hmrc.lisaapi.models.des.DesResponse

trait CreateLisaInvestorResponse extends DesResponse

case class CreateLisaInvestorSuccessResponse(investorId: String) extends CreateLisaInvestorResponse
case class CreateLisaInvestorAlreadyExistsResponse(investorId: String) extends CreateLisaInvestorResponse
case object CreateLisaInvestorInvestorNotFoundResponse extends CreateLisaInvestorResponse
case object CreateLisaInvestorErrorResponse extends CreateLisaInvestorResponse

object CreateLisaInvestorResponse {
  implicit val successReads: Reads[CreateLisaInvestorSuccessResponse] =
    (__ \ "investorID").read(JsonReads.investorId).map {investorId => CreateLisaInvestorSuccessResponse(investorId) }

  implicit val successWrites: Writes[CreateLisaInvestorSuccessResponse] =
    (__ \ "investorId").write[String].contramap {(resp: CreateLisaInvestorSuccessResponse) => resp.investorId }

  implicit val existsReads: Reads[CreateLisaInvestorAlreadyExistsResponse] =
    (__ \ "investorID").read(JsonReads.investorId).map {investorId => CreateLisaInvestorAlreadyExistsResponse(investorId) }

  implicit val existsWrites: Writes[CreateLisaInvestorAlreadyExistsResponse] =
    (__ \ "investorId").write[String].contramap {(resp: CreateLisaInvestorAlreadyExistsResponse) => resp.investorId }
}