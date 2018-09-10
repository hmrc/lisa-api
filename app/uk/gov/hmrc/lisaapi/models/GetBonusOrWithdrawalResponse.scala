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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}
import uk.gov.hmrc.lisaapi.models.des.DesResponse

trait GetBonusOrWithdrawalResponse extends DesResponse

case object GetBonusOrWithdrawalTransactionNotFoundResponse extends GetBonusOrWithdrawalResponse
case object GetBonusOrWithdrawalInvestorNotFoundResponse extends GetBonusOrWithdrawalResponse
case object GetBonusOrWithdrawalErrorResponse extends GetBonusOrWithdrawalResponse

case class GetBonusResponse(
  lifeEventId: Option[LifeEventId],
  periodStartDate: DateTime,
  periodEndDate: DateTime,
  htbTransfer: Option[HelpToBuyTransfer],
  inboundPayments: InboundPayments,
  bonuses: Bonuses,
  supersededBy: Option[TransactionId] = None,
  supersede: Option[Supersede] = None,
  paymentStatus: String,
  creationDate: DateTime
) extends GetBonusOrWithdrawalResponse

case class GetWithdrawalResponse(
                                  periodStartDate: DateTime,
                                  periodEndDate: DateTime,
                                  automaticRecoveryAmount: Option[Amount],
                                  withdrawalAmount: Amount,
                                  withdrawalChargeAmount: Amount,
                                  withdrawalChargeAmountYtd: Amount,
                                  fundsDeductedDuringWithdrawal: Boolean,
                                  withdrawalReason: String,
                                  supersededBy: Option[TransactionId] = None,
                                  supersede: Option[WithdrawalSupersede] = None,
                                  paymentStatus: String,
                                  creationDate: DateTime
) extends GetBonusOrWithdrawalResponse

object GetBonusOrWithdrawalResponse {
  implicit val bonusReads: Reads[GetBonusResponse] = (
    (JsPath \ "lifeEventId").readNullable(JsonReads.lifeEventId) and
    (JsPath \ "periodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "periodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "htbInAmountForPeriod").readNullable[Amount] and
    (JsPath \ "htbInAmountYtd").readNullable[Amount] and
    (JsPath \ "newSubsInPeriod").readNullable[Amount] and
    (JsPath \ "newSubsYtd").read[Amount] and
    (JsPath \ "totalSubsInPeriod").read[Amount] and
    (JsPath \ "totalSubsYtd").read[Amount] and
    (JsPath \ "bonusDueForPeriod").read[Amount] and
    (JsPath \ "bonusDueYtd").read[Amount] and
    (JsPath \ "bonusPaidYtd").readNullable[Amount] and
    (JsPath \ "claimReason").read[BonusClaimReason] and
    (JsPath \ "supersededTransactionById").readNullable(JsonReads.transactionId) and
    (JsPath \ "supersededTransactionId").readNullable(JsonReads.transactionId) and
    (JsPath \ "supersededTransactionAmount").readNullable[Amount] and
    (JsPath \ "supersededTransactionResult").readNullable[Amount] and
    (JsPath \ "supersededReason").readNullable[BonusClaimSupersedeReason] and
    (JsPath \ "automaticRecoveryamount").readNullable[Amount] and
    (JsPath \ "paymentStatus").read[String] and
    (JsPath \ "creationDate").read(JsonReads.isoDate).map(new DateTime(_))
  )(
    (
      lifeEventId,
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
      supersededTransactionById,
      supersededTransactionId,
      supersededTransactionAmount,
      supersededTransactionResult,
      supersededReason,
      automaticRecoveryAmount,
      paymentStatus,
      creationDate
    ) =>
      GetBonusResponse(
        lifeEventId = lifeEventId,
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
        htbTransfer = (htbInAmountForPeriod, htbInAmountYtd) match {
          case (Some(htbPeriod), Some(htbYtd)) => Some(HelpToBuyTransfer(htbPeriod, htbYtd))
          case _ => None
        },
        inboundPayments = InboundPayments(newSubsInPeriod, newSubsYtd, totalSubsInPeriod, totalSubsYtd),
        bonuses = Bonuses(
          bonusDueForPeriod,
          bonusDueYtd,
          bonusPaidYtd,
          claimReason match {
            case "LIFE_EVENT" => "Life Event"
            case "REGULAR_BONUS" => "Regular Bonus"
            case "SUPERSEDING_BONUS_CLAIM" => "Superseding bonus claim"
          }
        ),
        supersededBy = supersededTransactionById,
        supersede = (supersededTransactionId, supersededTransactionAmount, supersededTransactionResult, supersededReason, automaticRecoveryAmount) match {
          case (Some(id), Some(amount), Some(result), Some("Bonus Recovery"), Some(recovery)) => Some(
            BonusRecovery(recovery, id, amount, result)
          )
          case (Some(id), Some(amount), Some(result), Some("Additional Bonus"), _) => Some(
            AdditionalBonus(id, amount, result)
          )
          case _ => None
        },
        paymentStatus = paymentStatus match {
          case "PENDING" => "Pending"
          case "PAID" => "Paid"
          case "VOID" => "Void"
          case "CANCELLED" => "Cancelled"
        },
        creationDate = creationDate
      )
  )

  implicit val withdrawalReads: Reads[GetWithdrawalResponse] = (
    (JsPath \ "periodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "periodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "automaticRecoveryamount").readNullable[Amount] and
    (JsPath \ "withdrawalAmount").read[Amount] and
    (JsPath \ "withdrawalChargeAmount").read[Amount] and
    (JsPath \ "withdrawalChargeAmountYTD").read[Amount] and
    (JsPath \ "fundsDeductedDuringWithdrawal").read[Boolean] and
    (JsPath \ "withdrawalReason").read[WithdrawalReason] and
    (JsPath \ "supersededTransactionById").readNullable(JsonReads.transactionId) and
    (JsPath \ "supersededTransactionId").readNullable(JsonReads.transactionId) and
    (JsPath \ "supersededTransactionAmount").readNullable[Amount] and
    (JsPath \ "supersededTransactionResult").readNullable[Amount] and
    (JsPath \ "supersededReason").readNullable[WithdrawalSupersedeReason] and
    (JsPath \ "paymentStatus").read[String] and
    (JsPath \ "creationDate").read(JsonReads.isoDate).map(new DateTime(_))
  )(
    (
      periodStartDate,
      periodEndDate,
      automaticRecoveryAmount,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      withdrawalReason,
      supersededTransactionById,
      supersededTransactionId,
      supersededTransactionAmount,
      supersededTransactionResult,
      supersededReason,
      paymentStatus,
      creationDate
    ) =>
      GetWithdrawalResponse(
        periodStartDate = periodStartDate,
        periodEndDate = periodEndDate,
        automaticRecoveryAmount = automaticRecoveryAmount,
        withdrawalAmount = withdrawalAmount,
        withdrawalChargeAmount = withdrawalChargeAmount,
        withdrawalChargeAmountYtd = withdrawalChargeAmountYTD,
        fundsDeductedDuringWithdrawal = fundsDeductedDuringWithdrawal,
        withdrawalReason = withdrawalReason,
        supersededBy = supersededTransactionById,
        supersede = (supersededTransactionId, supersededTransactionAmount, supersededTransactionResult, supersededReason) match {
          case (Some(id), Some(amount), Some(result), Some(reason)) => Some(WithdrawalSuperseded(id, amount, result, reason))
          case _ => None
        },
        paymentStatus = paymentStatus match {
          case "DUE" => "Due"
          case "COLLECTED" => "Collected"
        },
        creationDate = creationDate
      )
  )

  implicit val bonusOrWithdrawalReads: Reads[GetBonusOrWithdrawalResponse] = Reads[GetBonusOrWithdrawalResponse] { json =>
    val reason = (json \ "withdrawalReason").asOpt[String]

    reason match {
      case Some(_) => withdrawalReads.reads(json)
      case None => bonusReads.reads(json)
    }
  }

  implicit val bonusWrites: Writes[GetBonusResponse] = (
    (JsPath \ "lifeEventId").writeNullable[String] and
    (JsPath \ "periodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "periodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "htbTransfer").writeNullable[HelpToBuyTransfer] and
    (JsPath \ "inboundPayments").write[InboundPayments] and
    (JsPath \ "bonuses").write[Bonuses] and
    (JsPath \ "supersededBy").writeNullable[String] and
    (JsPath \ "supersede").writeNullable[Supersede](Supersede.getSupersedeWrites)
  ){
    req: GetBonusResponse => (
      req.lifeEventId,
      req.periodStartDate,
      req.periodEndDate,
      req.htbTransfer,
      req.inboundPayments,
      req.bonuses,
      req.supersededBy,
      req.supersede
    )
  }

  implicit val withdrawalWrites: Writes[GetWithdrawalResponse] = (
      (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
      (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
      (JsPath \ "automaticRecoveryAmount").writeNullable[Amount] and
      (JsPath \ "withdrawalAmount").write[Amount] and
      (JsPath \ "withdrawalChargeAmount").write[Amount] and
      (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
      (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
      (JsPath \ "withdrawalReason").write[WithdrawalReason] and
      (JsPath \ "supersededBy").writeNullable[TransactionId] and
      (JsPath \ "supersede").writeNullable[WithdrawalSupersede]
    ){
    req: GetWithdrawalResponse => (
      req.periodStartDate,
      req.periodEndDate,
      req.automaticRecoveryAmount,
      req.withdrawalAmount,
      req.withdrawalChargeAmount,
      req.withdrawalChargeAmountYtd,
      req.fundsDeductedDuringWithdrawal,
      req.withdrawalReason,
      req.supersededBy,
      req.supersede
    )
  }

  implicit val bonusOrWithdrawalWrites: Writes[GetBonusOrWithdrawalResponse] = Writes[GetBonusOrWithdrawalResponse] {
    case w:GetWithdrawalResponse => withdrawalWrites.writes(w)
    case b:GetBonusResponse => bonusWrites.writes(b)
  }
}