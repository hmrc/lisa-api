/*
 * Copyright 2021 HM Revenue & Customs
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

case class CreateLisaInvestorRequest(investorNINO: Nino, firstName: Name, lastName: Name, dateOfBirth: DateTime)

object CreateLisaInvestorRequest {
  implicit val createLisaInvestorRequestReads: Reads[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").read[Nino](JsonReads.nino) and
    (JsPath \ "firstName").read(JsonReads.name).map[String](_.toUpperCase.trim) and
    (JsPath \ "lastName").read(JsonReads.name).map[String](_.toUpperCase.trim) and
    (JsPath \ "dateOfBirth").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(CreateLisaInvestorRequest.apply _)

  implicit val createLisaInvestorRequestWrites: Writes[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").write[String] and
    (JsPath \ "firstName").write[String] and
    (JsPath \ "lastName").write[String] and
    (JsPath \ "dateOfBirth").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(CreateLisaInvestorRequest.unapply))
}