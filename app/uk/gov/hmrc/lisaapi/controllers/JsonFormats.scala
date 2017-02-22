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
import uk.gov.hmrc.lisaapi.models.{AccountTransfer, CreateLisaAccountRequest, CreateLisaInvestorRequest}

trait JsonFormats {
  implicit val ninoRegex: String = "^[A-Z]{2}\\d{6}[A-D]$"
  implicit val nameRegex: String = "^.{1,35}$"
  implicit val dateRegex: String = "^\\d{4}-\\d{2}-\\d{2}$"
  implicit val lmrnRegex: String = "^Z\\d{4,6}$"
  implicit val investorIDRegex: String = "^\\d{10}$"
  implicit val creationReasonRegex: String = "^(New|Transferred)$"

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

  implicit val accountTransferReads: Reads[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").read[String] and
    (JsPath \ "transferredFromLMRN").read[String].filter(ValidationError("error.formatting.lmrn"))(input => input.matches(lmrnRegex)) and
    (JsPath \ "transferInDate").read[String].filter(ValidationError("error.formatting.date"))(input => input.matches(dateRegex)).map(new DateTime(_))
  )(AccountTransfer.apply _)

  implicit val accountTransferWrites: Writes[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").write[String] and
    (JsPath \ "transferredFromLMRN").write[String] and
    (JsPath \ "transferInDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(AccountTransfer.unapply))

  implicit val createLisaAccountRequestReads: Reads[CreateLisaAccountRequest] = (
    (JsPath \ "investorID").read[String].filter(ValidationError("error.formatting.investorID"))(input => input.matches(investorIDRegex)) and
    (JsPath \ "lisaManagerReferenceNumber").read[String].filter(ValidationError("error.formatting.lmrn"))(input => input.matches(lmrnRegex)) and
    (JsPath \ "accountID").read[String] and
    (JsPath \ "creationReason").read[String].filter(ValidationError("error.formatting.creationReason"))(input => input.matches(creationReasonRegex)) and
    (JsPath \ "firstSubscriptionDate").read[String].filter(ValidationError("error.formatting.date"))(input => input.matches(dateRegex)).map(new DateTime(_)) and
    (JsPath \ "transferAccount").readNullable[AccountTransfer]
  )(CreateLisaAccountRequest.apply _)

  implicit val createLisaAccountRequestWrites: Writes[CreateLisaAccountRequest] = (
    (JsPath \ "investorID").write[String] and
    (JsPath \ "lisaManagerReferenceNumber").write[String] and
    (JsPath \ "accountID").write[String] and
    (JsPath \ "creationReason").write[String] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "transferAccount").writeNullable[AccountTransfer]
  )(unlift(CreateLisaAccountRequest.unapply))
}