/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.models.des

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models._

trait DesResponse

case class DesAccountResponse(accountID: String) extends DesResponse

case class DesGetAccountResponse(
  accountId: String,
  investorId: String,
  creationReason: String,
  firstSubscriptionDate:String,
  accountStatus:String,
  subscriptionStatus:Option[String],
  accountClosureReason:Option[String],
  closureDate:Option[String],
  transferAccount: Option[DesGetAccountTransferResponse]
) extends DesResponse

case class DesGetAccountTransferResponse(
  transferredFromAccountId: String,
  transferredFromLMRN: String,
  transferInDate: DateTime
)

case class DesLifeEventResponse(lifeEventID: String) extends DesResponse
case class DesLifeEventRetrievalResponse(lifeEventID: LifeEventId, eventType: LifeEventType, eventDate: DateTime) extends DesResponse
case class DesCreateInvestorResponse(investorID: String) extends DesResponse
case class DesTransactionResponse(transactionID: String, message: String) extends DesResponse
case class DesFailureResponse(code: String = "INTERNAL_SERVER_ERROR", reason: String = "Internal Server Error") extends DesResponse
case class DesLifeEventExistResponse(code: String, reason: String, lifeEventID: String) extends DesResponse
case object DesEmptySuccessResponse extends DesResponse
case class DesUpdateSubscriptionSuccessResponse (code: String, reason: String)extends DesResponse
case class DesReinstateAccountSuccessResponse (code: String, reason: String)extends DesResponse
case class DesGetBonusPaymentResponse(lifeEventId: Option[LifeEventId],
                                      periodStartDate: DateTime,
                                      periodEndDate: DateTime,
                                      htbTransfer: Option[HelpToBuyTransfer],
                                      inboundPayments: InboundPayments,
                                      bonuses: Bonuses,
                                      creationDate: DateTime,
                                      status: String) extends DesResponse

object DesResponse {
  implicit val desCreateAccountResponseFormats: OFormat[DesAccountResponse] = Json.format[DesAccountResponse]

  implicit val desGetAccountTransferResponseReads: Reads[DesGetAccountTransferResponse] = (
    (JsPath \ "transferredFromAccountId").read(JsonReads.accountId) and
    (JsPath \ "transferredFromLMRN").read(JsonReads.lmrn) and
    (JsPath \ "transferInDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(DesGetAccountTransferResponse.apply _)

  implicit val desGetAccountTransferResponseWrites: Writes[DesGetAccountTransferResponse] = (
    (JsPath \ "transferredFromAccountId").write[String] and
    (JsPath \ "transferredFromLMRN").write[String] and
    (JsPath \ "transferInDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  )(unlift(DesGetAccountTransferResponse.unapply))

  implicit val desCreateInvestorResponseFormats: OFormat[DesCreateInvestorResponse] = Json.format[DesCreateInvestorResponse]
  implicit val desLifeEventResponseFormats: OFormat[DesLifeEventResponse] = Json.format[DesLifeEventResponse]
  implicit val desTransactionResponseFormats: OFormat[DesTransactionResponse] = Json.format[DesTransactionResponse]
  implicit val desUpdateSubscriptionResponseFormats: OFormat[DesUpdateSubscriptionSuccessResponse] = Json.format[DesUpdateSubscriptionSuccessResponse]
  implicit val desReinstateAccountResponseFormats: OFormat[DesReinstateAccountSuccessResponse] = Json.format[DesReinstateAccountSuccessResponse]

  implicit val desFailureReads: Reads[DesFailureResponse] = (
    (JsPath \ "code").read[String] and
    (JsPath \ "reason").read[String]
  )(DesFailureResponse.apply _)

  implicit val desFailureWrites: Writes[DesFailureResponse] = (
    (JsPath \ "code").write[String] and
    (JsPath \ "message").write[String]
  )(unlift(DesFailureResponse.unapply))

  implicit val requestLifeEventResponseReads: Reads[DesLifeEventRetrievalResponse] = (
    (JsPath \ "lifeEventID").read(JsonReads.lifeEventId) and
    (JsPath \ "eventType").read(JsonReads.lifeEventType) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(DesLifeEventRetrievalResponse.apply _)

  implicit val requestLifeEventAlreadyExistResponseFormats: OFormat[DesLifeEventExistResponse] = Json.format[DesLifeEventExistResponse]

  implicit val desGetBonusPaymentResponse: Reads[DesGetBonusPaymentResponse] = (
    (JsPath \ "lifeEventId").readNullable(JsonReads.lifeEventId) and
    (JsPath \ "claimPeriodStart").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "claimPeriodEnd").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "htbInAmountForPeriod").readNullable[Amount] and
    (JsPath \ "htbInAmountYtd").readNullable[Amount] and
    (JsPath \ "newSubsInPeriod").readNullable[Amount] and
    (JsPath \ "newSubsYtd").read[Amount] and
    (JsPath \ "totalSubsInPeriod").read[Amount] and
    (JsPath \ "totalSubsYtd").read[Amount] and
    (JsPath \ "bonusDueForPeriod").read[Amount] and
    (JsPath \ "bonusDueYtd").read[Amount] and
    (JsPath \ "bonusPaidYtd").readNullable[Amount] and
    (JsPath \ "claimReason").read[String] and
    (JsPath \ "creationDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "paymentStatus").read[String]
  )(
    (lifeEventId,
     periodStartDate,
     periodEndDate,
     htbInAmountForPeriod,
     htbInAmountYtd,
     newSubsInPeriod,
     newSubsYtd,
     totalSubsInPeriod,
     totalSubsYtd,
     bonusDueForPeriod,
     bonusDueYtd,
     bonusPaidYtd,
     claimReason,
     creationDate,
     status) =>
        DesGetBonusPaymentResponse(
          lifeEventId,
          periodStartDate,
          periodEndDate,
          (htbInAmountForPeriod, htbInAmountYtd) match {
            case (Some(amountForPeriod), Some(amountYtd)) => {
              Some(HelpToBuyTransfer(amountForPeriod, amountYtd))
            }
            case _ => None
          },
          InboundPayments(newSubsInPeriod,
            newSubsYtd,
            totalSubsInPeriod,
            totalSubsYtd
          ),
          Bonuses(bonusDueForPeriod,
            bonusDueYtd,
            bonusPaidYtd,
            claimReason match {
              case "LIFE_EVENT" => "Life Event"
              case "REGULAR_BONUS" => "Regular Bonus"
            }
          ),
          creationDate,
          status match {
            case "PENDING" => "Pending"
            case "PAID" => "Paid"
            case "VOID" => "Void"
            case "CANCELLED" => "Cancelled"
          }
        )
  )


  implicit val desGetAccountResponseReads: Reads[DesGetAccountResponse] = (
      (JsPath \ "investorId").read(JsonReads.investorId) and
      (JsPath \ "status").read[String] and
      (JsPath \ "creationDate").read(JsonReads.isoDate).map(new DateTime(_)) and
      (JsPath \ "creationReason").read[String] and
      (JsPath \ "accountClosureReason").readNullable[String] and
      (JsPath \ "lisaManagerClosureDate").readNullable(JsonReads.isoDate).map(_.map(new DateTime(_))) and
      (JsPath \ "subscriptionStatus").readNullable[String] and
      (JsPath \ "firstSubscriptionDate").read(JsonReads.isoDate).map(new DateTime(_)) and
      (JsPath \ "transferInDate").readNullable(JsonReads.isoDate).map(_.map(new DateTime(_))) and
      (JsPath \ "xferredFromAccountId").readNullable(JsonReads.accountId) and
      (JsPath \ "xferredFromLmrn").readNullable(JsonReads.lmrn)
  )(
    (investorId, status, _, creationReason, accountClosureReason, lisaManagerClosureDate, subscriptionStatus,
     firstSubscriptionDate, transferInDate, xferredFromAccountId, xferredFromLmrn) =>
      DesGetAccountResponse(
        accountId = "",
        investorId = investorId,
        creationReason = creationReason match {
          case "NEW" => "New"
          case "TRANSFERRED" => "Transferred"
          case "REINSTATED" => "Reinstated"
        },
        firstSubscriptionDate = firstSubscriptionDate.toString("yyyy-MM-dd"),
        accountStatus = status,
        subscriptionStatus = subscriptionStatus,
        accountClosureReason = accountClosureReason.map(cr => cr match {
          case "TRANSFERRED_OUT" => "Transferred out"
          case "ALL_FUNDS_WITHDRAWN" => "All funds withdrawn"
          case "VOIDED" => "Voided"
          case "CANCELLED" => "Cancellation"
        }),
        closureDate = lisaManagerClosureDate.map(_.toString("yyyy-MM-dd")),
        transferAccount = (xferredFromAccountId, xferredFromLmrn, transferInDate) match {
          case (Some(accountId), Some(lmrn), Some(date)) => Some(DesGetAccountTransferResponse(
            transferredFromAccountId = accountId,
            transferredFromLMRN = lmrn,
            transferInDate = date
          ))
          case _ => None
        }
      )
  )

  implicit val desGetAccountResponseWrites = Json.writes[DesGetAccountResponse]

}