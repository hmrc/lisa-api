/*
 * Copyright 2023 HM Revenue & Customs
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

sealed trait WithdrawalSupersede extends Product

case class WithdrawalSuperseded(
  originalTransactionId: String,
  originalWithdrawalChargeAmount: Amount,
  transactionResult: Amount,
  reason: String
) extends WithdrawalSupersede

case class WithdrawalIncrease(
                               originalTransactionId: String,
                               originalWithdrawalChargeAmount: Amount,
                               transactionResult: Amount,
                               reason: String
                             ) extends WithdrawalSupersede

case class WithdrawalDecrease(
                               originalTransactionId: String,
                               originalWithdrawalChargeAmount: Amount,
                               transactionResult: Amount,
                               reason: String
                             ) extends WithdrawalSupersede

case class WithdrawalRefund(
                               originalTransactionId: String,
                               originalWithdrawalChargeAmount: Amount,
                               transactionResult: Amount,
                               reason: String
                             ) extends WithdrawalSupersede

object WithdrawalSupersede {

  val withdrawalIncreaseReads: Reads[WithdrawalIncrease] = (
    (JsPath \ "originalTransactionId").read(JsonReads.transactionId) and
    (JsPath \ "originalWithdrawalChargeAmount").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "transactionResult").read(JsonReads.amount) and
    (JsPath \ "reason").read[String](Reads.pattern("Additional withdrawal".r, "error.formatting.reason"))
  )((transactionId, transactionAmount, transactionResult, reason) => WithdrawalIncrease(
    transactionId, transactionAmount, transactionResult, reason
  ))

  val withdrawalDecreaseReads: Reads[WithdrawalDecrease] = (
    (JsPath \ "originalTransactionId").read(JsonReads.transactionId) and
    (JsPath \ "originalWithdrawalChargeAmount").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "transactionResult").read(JsonReads.amount) and
    (JsPath \ "reason").read[String](Reads.pattern("Withdrawal reduction".r, "error.formatting.reason"))
  )((transactionId, transactionAmount, transactionResult, reason) => WithdrawalDecrease(
    transactionId, transactionAmount, transactionResult, reason
  ))

  val withdrawalRefundReads: Reads[WithdrawalRefund] = (
    (JsPath \ "originalTransactionId").read(JsonReads.transactionId) and
    (JsPath \ "originalWithdrawalChargeAmount").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "transactionResult").read(JsonReads.amount) and
    (JsPath \ "reason").read[String](Reads.pattern("Withdrawal refund".r, "error.formatting.reason"))
  )((transactionId, transactionAmount, transactionResult, reason) => WithdrawalRefund(
  transactionId, transactionAmount, transactionResult, reason
  ))

  implicit val supersedeReads: Reads[WithdrawalSupersede] = Reads[WithdrawalSupersede] { json =>
    val reason = (json \ "reason").as[String]

    reason match {
      case "Additional withdrawal" => withdrawalIncreaseReads.reads(json)
      case "Withdrawal refund" => withdrawalRefundReads.reads(json)
      case _ => withdrawalDecreaseReads.reads(json)
    }
  }

  implicit val withdrawalIncreaseWrites: Writes[WithdrawalIncrease] = (
    (JsPath \ "originalTransactionId").write[String] and
    (JsPath \ "originalWithdrawalChargeAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalIncrease => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Additional withdrawal"
    )
  }

  implicit val withdrawalDecreaseWrites: Writes[WithdrawalDecrease] = (
    (JsPath \ "originalTransactionId").write[String] and
    (JsPath \ "originalWithdrawalChargeAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalDecrease => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Withdrawal reduction"
    )
  }

  implicit val withdrawalRefundWrites: Writes[WithdrawalRefund] = (
    (JsPath \ "originalTransactionId").write[String] and
    (JsPath \ "originalWithdrawalChargeAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalRefund => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Withdrawal refund"
    )
  }

  implicit val withdrawalSupersededWrites: Writes[WithdrawalSuperseded] = (
    (JsPath \ "originalTransactionId").write[String] and
    (JsPath \ "originalWithdrawalChargeAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalSuperseded => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      b.reason
    )
  }

  implicit val desWithdrawalIncreaseWrites: Writes[WithdrawalIncrease] = (
    (JsPath \ "transactionId").write[String] and
    (JsPath \ "transactionAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalIncrease => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Additional Withdrawal"
    )
  }

  implicit val desWithdrawalDecreaseWrites: Writes[WithdrawalDecrease] = (
    (JsPath \ "transactionId").write[String] and
    (JsPath \ "transactionAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalDecrease => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Withdrawal Reduction"
    )
  }

  implicit val desWithdrawalRefundWrites: Writes[WithdrawalRefund] = (
    (JsPath \ "transactionId").write[String] and
    (JsPath \ "transactionAmount").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalRefund => (
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Withdrawal Refund"
    )
  }

  implicit val supersedeWrites: Writes[WithdrawalSupersede] = Writes[WithdrawalSupersede] {
    case inc: WithdrawalIncrease => withdrawalIncreaseWrites.writes(inc)
    case dec: WithdrawalDecrease => withdrawalDecreaseWrites.writes(dec)
    case ref: WithdrawalRefund => withdrawalRefundWrites.writes(ref)
    case get: WithdrawalSuperseded => withdrawalSupersededWrites.writes(get)
  }

  val desSupersedeWrites: Writes[WithdrawalSupersede] = Writes[WithdrawalSupersede] {
    case inc: WithdrawalIncrease => desWithdrawalIncreaseWrites.writes(inc)
    case dec: WithdrawalDecrease => desWithdrawalDecreaseWrites.writes(dec)
    case ref: WithdrawalRefund => desWithdrawalRefundWrites.writes(ref)
  }

}

sealed trait ReportWithdrawalChargeRequest extends Product {
  val claimPeriodStartDate: DateTime
  val claimPeriodEndDate: DateTime
  val automaticRecoveryAmount: Option[Amount]
  val withdrawalChargeAmount: Amount
  val fundsDeductedDuringWithdrawal: Boolean
  val withdrawalReason: String
  val supersede: Option[WithdrawalSupersede]
}

case class RegularWithdrawalChargeRequest(
  automaticRecoveryAmount: Option[Amount],
  claimPeriodStartDate: DateTime,
  claimPeriodEndDate: DateTime,
  withdrawalAmount: Amount,
  withdrawalChargeAmount: Amount,
  withdrawalChargeAmountYTD: Amount,
  fundsDeductedDuringWithdrawal: Boolean,
  withdrawalReason: String,
  supersede: Option[WithdrawalSupersede] = None
) extends ReportWithdrawalChargeRequest

case class SupersededWithdrawalChargeRequest(
  automaticRecoveryAmount: Option[Amount],
  claimPeriodStartDate: DateTime,
  claimPeriodEndDate: DateTime,
  withdrawalAmount: Amount,
  withdrawalChargeAmount: Amount,
  withdrawalChargeAmountYTD: Amount,
  fundsDeductedDuringWithdrawal: Boolean,
  supersede: Option[WithdrawalSupersede],
  withdrawalReason: String
) extends ReportWithdrawalChargeRequest

object ReportWithdrawalChargeRequest {
  val withdrawalFormatError = "error.formatting.withdrawalReason"

  implicit val regularWithdrawalChargeReads: Reads[RegularWithdrawalChargeRequest] = (
    (JsPath \ "automaticRecoveryAmount").readNullable(JsonReads.nonNegativeAmount) and
    (JsPath \ "claimPeriodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "claimPeriodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "withdrawalAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmountYTD").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "fundsDeductedDuringWithdrawal").read[Boolean] and
    (JsPath \ "supersede").readNullable[WithdrawalSupersede] and
    (JsPath \ "withdrawalReason").read[String](Reads.pattern("Regular withdrawal".r, withdrawalFormatError))
  )((
      automaticRecoveryAmount,
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      supersede,
      withdrawalReason
    ) => RegularWithdrawalChargeRequest(
      automaticRecoveryAmount,
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      withdrawalReason,
      supersede
    )
  )

  implicit val supersededWithdrawalChargeReads: Reads[SupersededWithdrawalChargeRequest] = (
    (JsPath \ "automaticRecoveryAmount").readNullable(JsonReads.nonNegativeAmount) and
    (JsPath \ "claimPeriodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "claimPeriodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "withdrawalAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmountYTD").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "fundsDeductedDuringWithdrawal").read[Boolean] and
    (JsPath \ "supersede").read[WithdrawalSupersede] and
    (JsPath \ "withdrawalReason").read[String](Reads.pattern("Superseded withdrawal".r, withdrawalFormatError))
  )((
      automaticRecoveryAmount,
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      supersede,
      withdrawalReason
    ) => SupersededWithdrawalChargeRequest(
      automaticRecoveryAmount,
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      Some(supersede),
      withdrawalReason
    )
  )

  implicit val reportWithdrawalChargeReads: Reads[ReportWithdrawalChargeRequest] = Reads[ReportWithdrawalChargeRequest] { json =>
    val withdrawalReason = (json \ "withdrawalReason").asOpt[String]

    withdrawalReason match {
      case Some("Superseded withdrawal") => supersededWithdrawalChargeReads.reads(json)
      case _ => regularWithdrawalChargeReads.reads(json)
    }
  }

  implicit val regularWithdrawalWrites: Writes[RegularWithdrawalChargeRequest] = (
    (JsPath \ "automaticRecoveryAmount").writeNullable[Amount] and
    (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
    (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
    (JsPath \ "withdrawalReason").write[String]
  ){req: RegularWithdrawalChargeRequest => (
    req.automaticRecoveryAmount,
    req.claimPeriodStartDate,
    req.claimPeriodEndDate,
    req.withdrawalAmount,
    req.withdrawalChargeAmount,
    req.withdrawalChargeAmountYTD,
    req.fundsDeductedDuringWithdrawal,
    "Regular withdrawal"
  )}

  implicit val supersededWithdrawalWrites: Writes[SupersededWithdrawalChargeRequest] = (
    (JsPath \ "automaticRecoveryAmount").writeNullable[Amount] and
    (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
    (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
    (JsPath \ "withdrawalReason").write[String] and
    (JsPath \ "supersede").writeNullable[WithdrawalSupersede]
  ){req: SupersededWithdrawalChargeRequest => (
    req.automaticRecoveryAmount,
    req.claimPeriodStartDate,
    req.claimPeriodEndDate,
    req.withdrawalAmount,
    req.withdrawalChargeAmount,
    req.withdrawalChargeAmountYTD,
    req.fundsDeductedDuringWithdrawal,
    "Superseded withdrawal",
    req.supersede
  )}

  implicit val reportWithdrawalChargeWrites: Writes[ReportWithdrawalChargeRequest] = Writes[ReportWithdrawalChargeRequest] {
    case regular: RegularWithdrawalChargeRequest => regularWithdrawalWrites.writes(regular)
    case superseded: SupersededWithdrawalChargeRequest => supersededWithdrawalWrites.writes(superseded)
  }

  implicit val desRegularWithdrawalWrites: Writes[RegularWithdrawalChargeRequest] = (
    (JsPath \ "automaticRecoveryAmount").writeNullable[Amount] and
    (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
    (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
    (JsPath \ "withdrawalReason").write[String]
  ){req: RegularWithdrawalChargeRequest => (
    req.automaticRecoveryAmount,
    req.claimPeriodStartDate,
    req.claimPeriodEndDate,
    req.withdrawalAmount,
    req.withdrawalChargeAmount,
    req.withdrawalChargeAmountYTD,
    req.fundsDeductedDuringWithdrawal,
    "Regular Withdrawal Charge"
  )}

  implicit val desSupersededWithdrawalWrites: Writes[SupersededWithdrawalChargeRequest] = (
    (JsPath \ "automaticRecoveryAmount").writeNullable[Amount] and
    (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
    (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
    (JsPath \ "withdrawalReason").write[String] and
    (JsPath \ "supersededDetail").writeNullable[WithdrawalSupersede](WithdrawalSupersede.desSupersedeWrites)
  ){req: SupersededWithdrawalChargeRequest => (
    req.automaticRecoveryAmount,
    req.claimPeriodStartDate,
    req.claimPeriodEndDate,
    req.withdrawalAmount,
    req.withdrawalChargeAmount,
    req.withdrawalChargeAmountYTD,
    req.fundsDeductedDuringWithdrawal,
    "Superseded Withdrawal Charge",
    req.supersede
  )}

  val desReportWithdrawalChargeWrites: Writes[ReportWithdrawalChargeRequest] = Writes[ReportWithdrawalChargeRequest] {
    case regular: RegularWithdrawalChargeRequest => desRegularWithdrawalWrites.writes(regular)
    case superseded: SupersededWithdrawalChargeRequest => desSupersededWithdrawalWrites.writes(superseded)
  }
}