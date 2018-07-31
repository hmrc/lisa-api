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
import play.api.libs.json.{JsPath, Json, Reads, Writes}

sealed trait ReportWithdrawalChargeRequest extends Product

case class RegularWithdrawalChargeRequest(
  claimPeriodStartDate: DateTime,
  claimPeriodEndDate: DateTime,
  withdrawalAmount: Amount,
  withdrawalChargeAmount: Amount,
  withdrawalChargeAmountYTD: Amount,
  fundsDeductedDuringWithdrawal: Boolean
) extends ReportWithdrawalChargeRequest

case class SupersededWithdrawalChargeRequest(
  claimPeriodStartDate: DateTime,
  claimPeriodEndDate: DateTime,
  withdrawalAmount: Amount,
  withdrawalChargeAmount: Amount,
  withdrawalChargeAmountYTD: Amount,
  fundsDeductedDuringWithdrawal: Boolean,
  supersede: WithdrawalSupersede
) extends ReportWithdrawalChargeRequest

object ReportWithdrawalChargeRequest {
  val withdrawalFormatError = "error.formatting.withdrawalReason"

  implicit val regularWithdrawalChargeReads: Reads[RegularWithdrawalChargeRequest] = (
    (JsPath \ "claimPeriodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "claimPeriodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "withdrawalAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmountYTD").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "fundsDeductedDuringWithdrawal").read[Boolean] and
    (JsPath \ "withdrawalReason").read[String](Reads.pattern("Regular withdrawal".r, withdrawalFormatError))
  )((
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      _
    ) => RegularWithdrawalChargeRequest(
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal
    )
  )

  implicit val supersededWithdrawalChargeReads: Reads[SupersededWithdrawalChargeRequest] = (
    (JsPath \ "claimPeriodStartDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "claimPeriodEndDate").read(JsonReads.isoDate).map(new DateTime(_)) and
    (JsPath \ "withdrawalAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmount").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "withdrawalChargeAmountYTD").read[Amount](JsonReads.nonNegativeAmount) and
    (JsPath \ "fundsDeductedDuringWithdrawal").read[Boolean] and
    (JsPath \ "supersede").read[WithdrawalSupersede] and
    (JsPath \ "withdrawalReason").read[String](Reads.pattern("Superseded withdrawal".r, withdrawalFormatError))
  )((
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      supersede,
      _
    ) => SupersededWithdrawalChargeRequest(
      claimPeriodStartDate,
      claimPeriodEndDate,
      withdrawalAmount,
      withdrawalChargeAmount,
      withdrawalChargeAmountYTD,
      fundsDeductedDuringWithdrawal,
      supersede
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
    (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmount").write[Amount] and
    (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
    (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
    (JsPath \ "withdrawalReason").write[String]
  ){req: RegularWithdrawalChargeRequest => (
    req.claimPeriodStartDate,
    req.claimPeriodEndDate,
    req.withdrawalAmount,
    req.withdrawalChargeAmount,
    req.withdrawalChargeAmountYTD,
    req.fundsDeductedDuringWithdrawal,
    "Regular withdrawal"
  )}

  implicit val supersededWithdrawalWrites: Writes[SupersededWithdrawalChargeRequest] = (
    (JsPath \ "claimPeriodStartDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
      (JsPath \ "claimPeriodEndDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
      (JsPath \ "withdrawalAmount").write[Amount] and
      (JsPath \ "withdrawalChargeAmount").write[Amount] and
      (JsPath \ "withdrawalChargeAmountYTD").write[Amount] and
      (JsPath \ "fundsDeductedDuringWithdrawal").write[Boolean] and
      (JsPath \ "withdrawalReason").write[String] and
      (JsPath \ "supersede").write[WithdrawalSupersede]
    ){req: SupersededWithdrawalChargeRequest => (
    req.claimPeriodStartDate,
    req.claimPeriodEndDate,
    req.withdrawalAmount,
    req.withdrawalChargeAmount,
    req.withdrawalChargeAmountYTD,
    req.fundsDeductedDuringWithdrawal,
    "Superseded withdrawal",
    req.supersede
  )}

  implicit val reportWithdrawalChargeWrites: Writes[ReportWithdrawalChargeRequest] = Writes[ReportWithdrawalChargeRequest] { obj =>
    obj match {
      case regular: RegularWithdrawalChargeRequest => regularWithdrawalWrites.writes(regular)
      case superseded: SupersededWithdrawalChargeRequest => supersededWithdrawalWrites.writes(superseded)
    }
  }
}

sealed trait WithdrawalSupersede

case class WithdrawalIncrease(
  automaticRecoveryAmount: Amount,
  originalTransactionId: String,
  originalWithdrawalChargeAmount: Amount,
  transactionResult: Amount
) extends WithdrawalSupersede

case class WithdrawalDecrease(
  originalTransactionId: String,
  originalWithdrawalChargeAmount: Amount,
  transactionResult: Amount
) extends WithdrawalSupersede

object WithdrawalSupersede {

  val withdrawalIncreaseReads: Reads[WithdrawalIncrease] = (
    (JsPath \ "automaticRecoveryAmount").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "originalTransactionId").read(JsonReads.transactionId) and
    (JsPath \ "originalWithdrawalChargeAmount").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "transactionResult").read(JsonReads.amount) and
    (JsPath \ "reason").read[String](Reads.pattern("Additional withdrawal".r, "error.formatting.reason"))
  )((automaticRecoveryAmount, transactionId, transactionAmount, transactionResult, _) => WithdrawalIncrease(
    automaticRecoveryAmount, transactionId, transactionAmount, transactionResult
  ))

  val withdrawalDecreaseReads: Reads[WithdrawalDecrease] = (
    (JsPath \ "originalTransactionId").read(JsonReads.transactionId) and
    (JsPath \ "originalWithdrawalChargeAmount").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "transactionResult").read(JsonReads.amount) and
    (JsPath \ "reason").read[String](Reads.pattern("Withdrawal reduction".r, "error.formatting.reason"))
  )((transactionId, transactionAmount, transactionResult, _) => WithdrawalDecrease(
    transactionId, transactionAmount, transactionResult
  ))

  implicit val supersedeReads: Reads[WithdrawalSupersede] = Reads[WithdrawalSupersede] { json =>
    val reason = (json \ "reason").as[String]

    reason match {
      case "Additional withdrawal" => withdrawalIncreaseReads.reads(json)
      case _ => withdrawalDecreaseReads.reads(json)
    }
  }

  implicit val withdrawalIncreaseWrites: Writes[WithdrawalIncrease] = (
    (JsPath \ "automaticRecoveryAmount").write[Amount] and
    (JsPath \ "originalTransactionId").write[String] and
    (JsPath \ "originalBonusDueForPeriod").write[Amount] and
    (JsPath \ "transactionResult").write[Amount] and
    (JsPath \ "reason").write[String]
  ){
    b: WithdrawalIncrease => (
      b.automaticRecoveryAmount,
      b.originalTransactionId,
      b.originalWithdrawalChargeAmount,
      b.transactionResult,
      "Additional withdrawal"
    )
  }

  implicit val withdrawalDecreaseWrites: Writes[WithdrawalDecrease] = (
    (JsPath \ "originalTransactionId").write[String] and
    (JsPath \ "originalBonusDueForPeriod").write[Amount] and
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

  implicit val supersedeWrites: Writes[WithdrawalSupersede] = Writes[WithdrawalSupersede] {
    case inc: WithdrawalIncrease => withdrawalIncreaseWrites.writes(inc)
    case dec: WithdrawalDecrease => withdrawalDecreaseWrites.writes(dec)
  }

}