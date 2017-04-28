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
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.models.{ApiResponse, ApiResponseData, CreateLisaInvestorRequest, _}

trait JsonFormats {

  implicit val ninoRegex = "^((?!(BG|GB|KN|NK|NT|TN|ZZ)|(D|F|I|Q|U|V)[A-Z]|[A-Z](D|F|I|O|Q|U|V))[A-Z]{2})[0-9]{6}[A-D]?$".r
  implicit val nameRegex = "^[a-zA-Z &`\\-\\'^]{1,35}$".r
  implicit val lmrnRegex = "^Z\\d{4,6}$".r
  implicit val investorIDRegex = "^\\d{10}$".r
  implicit val lifeEventIDRegex = "^\\d{10}$".r
  implicit val accountIDRegex = "^[a-zA-Z0-9 :\\-]{1,20}$".r
  implicit val accountClosureRegex = "^(Transferred out|All funds withdrawn|Voided)$".r
  implicit val transactionTypeRegex = "^(Bonus|Penalty)$".r
  implicit val bonusClaimReasonRegex = "^(Life Event|Regular Bonus)$".r
  implicit val lifeEventTypeRegex = "^(LISA Investor Terminal Ill Health|LISA Investor Death|House Purchase)$".r

  implicit val createLisaInvestorRequestReads: Reads[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").read(Reads.pattern(ninoRegex, "error.formatting.nino")) and
    (JsPath \ "firstName").read(Reads.pattern(nameRegex, "error.formatting.firstName")).map[String](_.toUpperCase) and
    (JsPath \ "lastName").read(Reads.pattern(nameRegex, "error.formatting.lastName")).map[String](_.toUpperCase) and
    (JsPath \ "dateOfBirth").read(isoDateReads(allowFutureDates = false)).map(new DateTime(_))
  )(CreateLisaInvestorRequest.apply _)

  implicit val createLisaInvestorRequestWrites: Writes[CreateLisaInvestorRequest] = (
    (JsPath \ "investorNINO").write[String] and
    (JsPath \ "firstName").write[String] and
    (JsPath \ "lastName").write[String] and
    (JsPath \ "dateOfBirth").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(CreateLisaInvestorRequest.unapply))

  implicit val desCreateAccountResponseFormats = Json.format[DesAccountResponse]
  implicit val desCreateAccountResponseFormatsOld = Json.format[DesAccountResponseOld]
  implicit val desCreateInvestorResponseFormats = Json.format[DesCreateInvestorResponse]
  implicit val desLifeEventResponseFormats = Json.format[DesLifeEventResponse]
  implicit val desTransactionResponseFormats = Json.format[DesTransactionResponse]

  implicit val desFailureReads: Reads[DesFailureResponse] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "reason").read[String]
  )(DesFailureResponse.apply _)

  implicit val desFailureWrites: Writes[DesFailureResponse] = (
    (JsPath \ "code").write[String] and
    (JsPath \ "message").write[String]
  )(unlift(DesFailureResponse.unapply))

  implicit val apiResponseDataFormats = Json.format[ApiResponseData]
  implicit val apiResponseFormats = Json.format[ApiResponse]

  implicit val accountTransferReads: Reads[AccountTransfer] = (
    (JsPath \ "transferredFromAccountId").read(Reads.pattern(accountIDRegex, "error.formatting.accountID")) and
    (JsPath \ "transferredFromLMRN").read(Reads.pattern(lmrnRegex, "error.formatting.lmrn")) and
    (JsPath \ "transferInDate").read(isoDateReads()).map(new DateTime(_))
  )(AccountTransfer.apply _)

  implicit val accountTransferWrites: Writes[AccountTransfer] = (
    (JsPath \ "transferredFromAccountID").write[String] and
    (JsPath \ "transferredFromLMRN").write[String] and
    (JsPath \ "transferInDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(AccountTransfer.unapply))

  implicit val createLisaAccountCreationRequestReads: Reads[CreateLisaAccountCreationRequest] = (
    (JsPath \ "investorId").read(Reads.pattern(investorIDRegex, "error.formatting.investorID")) and
    (JsPath \ "accountId").read(Reads.pattern(accountIDRegex, "error.formatting.accountID")) and
    (JsPath \ "firstSubscriptionDate").read(isoDateReads()).map(new DateTime(_))
  )(CreateLisaAccountCreationRequest.apply _)

  implicit val createLisaAccountTransferRequestReads: Reads[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorId").read(Reads.pattern(investorIDRegex, "error.formatting.investorID")) and
    (JsPath \ "accountId").read(Reads.pattern(accountIDRegex, "error.formatting.accountID")) and
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
    (JsPath \ "accountID").write[String] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "creationReason").write[String]
  ){req: CreateLisaAccountCreationRequest => (req.investorId, req.accountId, req.firstSubscriptionDate, "New")}

  implicit val createLisaAccountTransferRequestWrites: Writes[CreateLisaAccountTransferRequest] = (
    (JsPath \ "investorID").write[String] and
    (JsPath \ "accountID").write[String] and
    (JsPath \ "firstSubscriptionDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "transferAccount").write[AccountTransfer] and
    (JsPath \ "creationReason").write[String]
  ){req: CreateLisaAccountTransferRequest => (req.investorId, req.accountId, req.firstSubscriptionDate, req.transferAccount, "Transferred")}

  implicit val reportLifeEventRequestReads: Reads[ReportLifeEventRequest] = (
    (JsPath \ "eventType").read(Reads.pattern(lifeEventTypeRegex, "error.formatting.eventType")) and
    (JsPath \ "eventDate").read(isoDateReads(allowFutureDates = false)).map(new DateTime(_))
    )(ReportLifeEventRequest.apply _)

  implicit val reportLifeEventRequestWrites: Writes[ReportLifeEventRequest] = (
      (JsPath \ "eventType").write[String] and
      (JsPath \ "eventDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(ReportLifeEventRequest.unapply))

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

  implicit val htbFormats = Json.format[HelpToBuyTransfer]
  implicit val ibpFormats = Json.format[InboundPayments]

  implicit val bonusesReads: Reads[Bonuses] = (
    (JsPath \ "bonusDueForPeriod").read[Float] and
    (JsPath \ "totalBonusDueYTD").read[Float] and
    (JsPath \ "bonusPaidYTD").readNullable[Float] and
    (JsPath \ "claimReason").read((Reads.pattern(bonusClaimReasonRegex, "error.formatting.claimReason")))
  )(Bonuses.apply _)

  implicit val bonusesWrites: Writes[Bonuses] = (
    (JsPath \ "bonusDueForPeriod").write[Float] and
    (JsPath \ "totalBonusDueYTD").write[Float] and
    (JsPath \ "bonusPaidYTD").writeNullable[Float] and
    (JsPath \ "claimReason").write[String]
  )(unlift(Bonuses.unapply))

  implicit val requestBonusPaymentReads: Reads[RequestBonusPaymentRequest] = (
    (JsPath \ "lifeEventId").readNullable(Reads.pattern(lifeEventIDRegex, "error.formatting.lifeEventID")) and
    (JsPath \ "periodStartDate").read(isoDateReads()).map(new DateTime(_)) and
    (JsPath \ "periodEndDate").read(isoDateReads()).map(new DateTime(_)) and
    (JsPath \ "transactionType").read(Reads.pattern(transactionTypeRegex, "error.formatting.transactionType")) and
    (JsPath \ "htbTransfer").readNullable[HelpToBuyTransfer] and
    (JsPath \ "inboundPayments").read[InboundPayments] and
    (JsPath \ "bonuses").read[Bonuses]
  )(RequestBonusPaymentRequest.apply _)

  implicit val requestBonusPaymentWrites: Writes[RequestBonusPaymentRequest] = (
    (JsPath \ "lifeEventId").writeNullable[String] and
    (JsPath \ "periodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "periodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "transactionType").write[String] and
    (JsPath \ "htbTransfer").writeNullable[HelpToBuyTransfer] and
    (JsPath \ "inboundPayments").write[InboundPayments] and
    (JsPath \ "bonuses").write[Bonuses]
  )(unlift(RequestBonusPaymentRequest.unapply))

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