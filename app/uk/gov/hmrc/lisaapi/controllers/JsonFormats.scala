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
import org.joda.time.format.DateTimeFormat
import play.api.data.validation.ValidationError
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models.des.{DesAccountResponse, DesCreateInvestorResponse}
import uk.gov.hmrc.lisaapi.models.{ApiResponse, ApiResponseData, CreateLisaInvestorRequest, _}

trait JsonFormats {

  implicit val ninoRegex = "^[A-Z]{2}\\d{6}[A-D]$".r
  implicit val nameRegex = "^.{1,35}$".r
  implicit val lmrnRegex = "^Z\\d{4,6}$".r
  implicit val investorIDRegex = "^\\d{10}$".r
  implicit val accountClosureRegex = "^(Transferred out|All funds withdrawn|Voided)$".r

  implicit val createLisaInvestorRequestReads: Reads[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").read(Reads.pattern(ninoRegex, "error.formatting.nino")) and
    (JsPath \ "firstName").read(Reads.pattern(nameRegex, "error.formatting.firstName")) and
    (JsPath \ "lastName").read(Reads.pattern(nameRegex, "error.formatting.lastName")) and
    (JsPath \ "DoB").read(isoDateReads(allowFutureDates = false)).map(new DateTime(_))
  )(CreateLisaInvestorRequest.apply _)

  implicit val createLisaInvestorRequestWrites: Writes[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").write[String] and
    (JsPath \ "firstName").write[String] and
    (JsPath \ "lastName").write[String] and
    (JsPath \ "DoB").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(CreateLisaInvestorRequest.unapply))

  implicit val desCreateAccountResponseFormats = Json.format[DesAccountResponse]
  implicit val desCreateInvestorResponseFormats = Json.format[DesCreateInvestorResponse]

  implicit val apiResponseDataFormats = Json.format[ApiResponseData]
  implicit val apiResponseFormats = Json.format[ApiResponse]

  implicit val accountTransferReads: Reads[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").read[String] and
    (JsPath \ "transferredFromLMRN").read(Reads.pattern(lmrnRegex, "error.formatting.lmrn")) and
    (JsPath \ "transferInDate").read(isoDateReads()).map(new DateTime(_))
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
    (JsPath \ "firstSubscriptionDate").read(isoDateReads()).map(new DateTime(_))
  )(CreateLisaAccountCreationRequest.apply _)

  implicit val createLisaAccountTransferRequestReads: Reads[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorID").read(Reads.pattern(investorIDRegex, "error.formatting.investorID")) and
    (JsPath \ "lisaManagerReferenceNumber").read(Reads.pattern(lmrnRegex, "error.formatting.lmrn")) and
    (JsPath \ "accountID").read[String] and
    (JsPath \ "firstSubscriptionDate").read(isoDateReads()).map(new DateTime(_)) and
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

  implicit val closeLisaAccountRequestReads: Reads[CloseLisaAccountRequest] = (
    (JsPath \ "accountClosureReason").read(Reads.pattern(accountClosureRegex, "error.formatting.accountClosureReason")) and
    (JsPath \ "closureDate").read(isoDateReads(allowFutureDates = false)).map(new DateTime(_))
  )(CloseLisaAccountRequest.apply _)

  implicit val closeLisaAccountRequestWrites: Writes[CloseLisaAccountRequest] = (
    (JsPath \ "accountClosureReason").write[String] and
    (JsPath \ "closureDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(CloseLisaAccountRequest.unapply))

  private def isoDateReads(allowFutureDates: Boolean = true): Reads[org.joda.time.DateTime] = new Reads[org.joda.time.DateTime] {

    val dateFormat = "yyyy-MM-dd"
    val dateValidationMessage = "error.formatting.date"

    def reads(json: JsValue): JsResult[DateTime] = json match {
      case JsString(s) => parseDate(s) match {
        case Some(d: DateTime) => {
          if (!allowFutureDates && d.isAfterNow) {
            JsError(Seq(JsPath() -> Seq(ValidationError(dateValidationMessage))))
          }
          else {
            JsSuccess(d)
          }
        }
        case None => JsError(Seq(JsPath() -> Seq(ValidationError(dateValidationMessage))))
      }
      case _ => JsError(Seq(JsPath() -> Seq(ValidationError(dateValidationMessage))))
    }

    private def parseDate(input: String): Option[DateTime] =
      scala.util.control.Exception.allCatch[DateTime] opt (DateTime.parse(input, DateTimeFormat.forPattern(dateFormat)))

  }

}