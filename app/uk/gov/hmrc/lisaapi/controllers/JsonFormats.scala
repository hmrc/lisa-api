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

package uk.gov.hmrc.lisaapi.controllers

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.data.validation.ValidationError
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest

trait JsonFormats {
  implicit val ninoRegex: String = "^[A-Z]{2}\\d{6}[A-D]$"
  implicit val nameRegex: String = "^.{1,35}$"
  implicit val dateRegex: String = "^\\d{4}-\\d{2}-\\d{2}$"

  implicit val createLisaInvestorRequestReads: Reads[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").read[String].filter(ValidationError("error.formatting.nino"))(input => input.matches(ninoRegex)) and
      (JsPath \ "firstName").read[String].filter(ValidationError("error.formatting.firstName"))(input => input.matches(nameRegex)) and
      (JsPath \ "lastName").read[String].filter(ValidationError("error.formatting.lastName"))(input => input.matches(nameRegex)) and
      (JsPath \ "DoB").read[String].filter(ValidationError("error.formatting.date"))(input => input.matches(dateRegex)).map(new DateTime(_))
    )(CreateLisaInvestorRequest.apply _)

  implicit val createLisaInvestorRequestWrites: Writes[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").write[String] and
      (JsPath \ "firstName").write[String] and
      (JsPath \ "lastName").write[String] and
      (JsPath \ "DoB").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
    )(unlift(CreateLisaInvestorRequest.unapply))
}