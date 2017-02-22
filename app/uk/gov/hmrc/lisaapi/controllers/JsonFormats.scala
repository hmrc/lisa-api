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
import uk.gov.hmrc.lisaapi.models.des.DesCreateInvestorResponse
import uk.gov.hmrc.lisaapi.models.{ApiResponse, ApiResponseData, CreateLisaInvestorRequest, _}

trait JsonFormats {
  implicit val ninoRegex = "^[A-Z]{2}\\d{6}[A-D]$".r
  implicit val nameRegex = "^.{1,35}$".r
  implicit val dateRegex = "^\\d{4}-\\d{2}-\\d{2}$".r
  implicit val lmrnRegex = "^Z\\d{4,6}$".r
  implicit val investorIDRegex = "^\\d{10}$".r

  implicit val createLisaInvestorRequestReads: Reads[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").read(Reads.pattern(ninoRegex, "error.formatting.nino")) and
    (JsPath \ "firstName").read(Reads.pattern(nameRegex, "error.formatting.firstName")) and
    (JsPath \ "lastName").read(Reads.pattern(nameRegex, "error.formatting.lastName")) and
    (JsPath \ "DoB").read(Reads.pattern(dateRegex, "error.formatting.date")).map(new DateTime(_))
  )(CreateLisaInvestorRequest.apply _)

  implicit val createLisaInvestorRequestWrites: Writes[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").write[String] and
    (JsPath \ "firstName").write[String] and
    (JsPath \ "lastName").write[String] and
    (JsPath \ "DoB").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(CreateLisaInvestorRequest.unapply))

  implicit val desCreateInvestorResponseFormats = Json.format[DesCreateInvestorResponse]

  implicit val apiResponseDataFormats = Json.format[ApiResponseData]
  implicit val apiResponseFormats = Json.format[ApiResponse]

  implicit val accountTransferReads: Reads[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").read[String] and
    (JsPath \ "transferredFromLMRN").read(Reads.pattern(lmrnRegex, "error.formatting.lmrn")) and
    (JsPath \ "transferInDate").read(Reads.pattern(dateRegex, "error.formatting.date")).map(new DateTime(_))
  )(AccountTransfer.apply _)

  implicit val accountTransferWrites: Writes[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").write[String] and
    (JsPath \ "transferredFromLMRN").write[String] and
    (JsPath \ "transferInDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(AccountTransfer.unapply))

  implicit val createLisaAccountCreationRequestReads: Reads[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorID").read(Reads.pattern(investorIDRegex, "error.formatting.investorID")) and
    (JsPath \ "lisaManagerReferenceNumber").read(Reads.pattern(lmrnRegex, "error.formatting.lmrn")) and
    (JsPath \ "accountID").read[String] and
    (JsPath \ "firstSubscriptionDate").read(Reads.pattern(dateRegex, "error.formatting.date")).map(new DateTime(_))
  )(CreateLisaAccountCreationRequest.apply _)

  implicit val createLisaAccountTransferRequestReads: Reads[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorID").read(Reads.pattern(investorIDRegex, "error.formatting.investorID")) and
    (JsPath \ "lisaManagerReferenceNumber").read(Reads.pattern(lmrnRegex, "error.formatting.lmrn")) and
    (JsPath \ "accountID").read[String] and
    (JsPath \ "firstSubscriptionDate").read(Reads.pattern(dateRegex, "error.formatting.date")).map(new DateTime(_)) and
    (JsPath \ "transferAccount").read[AccountTransfer]
  )(CreateLisaAccountTransferRequest.apply _)

  implicit val createLisaAccountRequestReads = Reads[CreateLisaAccountRequest] { json =>
    (json \ "creationReason").validate[String](Reads.pattern("^(New|Transferred)$".r, "error.formatting.creationReason")).flatMap {
      case "New" => createLisaAccountCreationRequestReads.reads(json)
      case "Transferred" => createLisaAccountTransferRequestReads.reads(json)
    }
  }

  implicit val createLisaAccountCreationRequestWrites: Writes[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorID").write[String] and
    (JsPath \ "lisaManagerReferenceNumber").write[String] and
    (JsPath \ "accountID").write[String] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(CreateLisaAccountCreationRequest.unapply))

  implicit val createLisaAccountTransferRequestWrites: Writes[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorID").write[String] and
    (JsPath \ "lisaManagerReferenceNumber").write[String] and
    (JsPath \ "accountID").write[String] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "transferAccount").write[AccountTransfer]
  )(unlift(CreateLisaAccountTransferRequest.unapply))

  implicit val createLisaAccountRequestWrites = Writes[CreateLisaAccountRequest] {
    case r: CreateLisaAccountCreationRequest => createLisaAccountCreationRequestWrites.writes(r)
    case r: CreateLisaAccountTransferRequest => createLisaAccountTransferRequestWrites.writes(r)
  }

}