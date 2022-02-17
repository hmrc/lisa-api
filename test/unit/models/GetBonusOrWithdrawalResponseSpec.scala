/*
 * Copyright 2022 HM Revenue & Customs
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

package unit.models

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.models._

class GetBonusOrWithdrawalResponseSpec extends PlaySpec {

  val validBonusJson = Json.obj(
    "creationDate" -> "2018-02-20",
    "claimPeriodStart" -> "2018-01-06",
    "claimPeriodEnd" -> "2018-02-05",
    "lifeEventId" -> "1234567890",
    "htbInAmountForPeriod" -> 100,
    "htbInAmountYtd" -> 200,
    "newSubsInPeriod" -> 100,
    "newSubsYtd" -> 200,
    "totalSubsInPeriod" -> 100,
    "totalSubsYtd" -> 200,
    "bonusDueForPeriod" -> 25,
    "bonusDueYtd" -> 50,
    "bonusPaidYtd" -> 25,
    "claimReason" -> "LIFE_EVENT",
    "paymentStatus" -> "PAID",
    "transactionSupersededById" -> "9999999999"
  )

  val validWithdrawalJson = Json.obj(
    "creationDate" -> "2018-02-20",
    "claimPeriodStart" -> "2018-01-06",
    "claimPeriodEnd" -> "2018-02-05",
    "automaticRecoveryAmount" -> 250,
    "withdrawalAmount" -> 1000,
    "withdrawalChargeAmount" -> 250,
    "withdrawalChargeAmountYtd" -> 250,
    "fundsDeductedDuringWithdrawal" -> "YES",
    "withdrawalReason" -> "REGULAR_WITHDRAWAL_CHARGE",
    "paymentStatus" -> "COLLECTION_ACTION",
    "transactionSupersededById" -> "8888888888"
  )

  "GetBonusOrWithdrawal" must {

    "return a valid bonus" when {

      "given a valid regular bonus" in {
        val result = validBonusJson.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetBonusResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              lifeEventId = Some("1234567890"),
              htbTransfer = Some(HelpToBuyTransfer(htbTransferInForPeriod = 100, htbTransferTotalYTD = 200)),
              paymentStatus = "Paid",
              inboundPayments = InboundPayments(newSubsForPeriod = Some(100), newSubsYTD = 200, totalSubsForPeriod = 100, totalSubsYTD = 200),
              bonuses = Bonuses(bonusDueForPeriod = 25, totalBonusDueYTD = 50, bonusPaidYTD = Some(25), claimReason = "Life Event"),
              supersededBy = Some("9999999999")
            )
          }
        )
      }

      "given a valid superseding bonus recovery" in {
        val json = validBonusJson ++ Json.obj(
          "supersededTransactionId" -> "0987654321",
          "supersededTransactionAmount" -> 50,
          "supersededTransactionResult" -> -25,
          "supersededReason" -> "BONUS_RECOVERY",
          "automaticRecoveryAmount" -> 25,
          "claimReason" -> "SUPERSEDED_BONUS"
        )

        val result = json.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetBonusResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              lifeEventId = Some("1234567890"),
              htbTransfer = Some(HelpToBuyTransfer(htbTransferInForPeriod = 100, htbTransferTotalYTD = 200)),
              paymentStatus = "Paid",
              inboundPayments = InboundPayments(newSubsForPeriod = Some(100), newSubsYTD = 200, totalSubsForPeriod = 100, totalSubsYTD = 200),
              bonuses = Bonuses(bonusDueForPeriod = 25, totalBonusDueYTD = 50, bonusPaidYTD = Some(25), claimReason = "Superseded Bonus"),
              supersededBy = Some("9999999999"),
              supersede = Some(BonusRecovery(
                automaticRecoveryAmount = 25,
                originalTransactionId = "0987654321",
                originalBonusDueForPeriod = 50,
                transactionResult = -25
              ))
            )
          }
        )
      }

      "given a valid superseding additional bonus" in {
        val json = validBonusJson ++ Json.obj(
          "supersededTransactionId" -> "0987654321",
          "supersededTransactionAmount" -> 10,
          "supersededTransactionResult" -> 15,
          "supersededReason" -> "ADDITIONAL_BONUS",
          "claimReason" -> "SUPERSEDED_BONUS"
        )

        val result = json.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetBonusResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              lifeEventId = Some("1234567890"),
              htbTransfer = Some(HelpToBuyTransfer(htbTransferInForPeriod = 100, htbTransferTotalYTD = 200)),
              paymentStatus = "Paid",
              inboundPayments = InboundPayments(newSubsForPeriod = Some(100), newSubsYTD = 200, totalSubsForPeriod = 100, totalSubsYTD = 200),
              bonuses = Bonuses(bonusDueForPeriod = 25, totalBonusDueYTD = 50, bonusPaidYTD = Some(25), claimReason = "Superseded Bonus"),
              supersededBy = Some("9999999999"),
              supersede = Some(AdditionalBonus(
                originalTransactionId = "0987654321",
                originalBonusDueForPeriod = 10,
                transactionResult = 15
              ))
            )
          }
        )
      }

    }

    "return a valid withdrawal" when {

      "given a valid regular withdrawal" in {
        val result = validWithdrawalJson.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetWithdrawalResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              automaticRecoveryAmount = Some(250),
              withdrawalAmount = 1000,
              withdrawalChargeAmount = 250,
              withdrawalChargeAmountYtd = 250,
              fundsDeductedDuringWithdrawal = true,
              withdrawalReason = "Regular withdrawal",
              paymentStatus = "Collected",
              supersededBy = Some("8888888888")
            )
          }
        )
      }

      "given a valid superseding additional withdrawal" in {
        val json = validWithdrawalJson ++ Json.obj(
          "supersededTransactionId" -> "0987654321",
          "supersededTransactionAmount" -> 500,
          "supersededTransactionResult" -> 125,
          "supersededReason" -> "ADDITIONAL_WITHDRAWAL",
          "withdrawalReason" -> "SUPERSEDED_WITHDRAWAL_CHARGE"
        )

        val result = json.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetWithdrawalResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              automaticRecoveryAmount = Some(250),
              withdrawalAmount = 1000,
              withdrawalChargeAmount = 250,
              withdrawalChargeAmountYtd = 250,
              fundsDeductedDuringWithdrawal = true,
              withdrawalReason = "Superseded withdrawal",
              paymentStatus = "Collected",
              supersededBy = Some("8888888888"),
              supersede = Some(WithdrawalSuperseded(
                originalTransactionId = "0987654321",
                originalWithdrawalChargeAmount = 500,
                transactionResult = 125,
                reason = "Additional withdrawal"
              ))
            )
          }
        )
      }

      "given a valid superseding withdrawal reduction" in {
        val json = validWithdrawalJson ++ Json.obj(
          "supersededTransactionId" -> "0987654321",
          "supersededTransactionAmount" -> 2000,
          "supersededTransactionResult" -> -1000,
          "supersededReason" -> "WITHDRAWAL_REDUCTION",
          "withdrawalReason" -> "SUPERSEDED_WITHDRAWAL_CHARGE"
        )

        val result = json.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetWithdrawalResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              automaticRecoveryAmount = Some(250),
              withdrawalAmount = 1000,
              withdrawalChargeAmount = 250,
              withdrawalChargeAmountYtd = 250,
              fundsDeductedDuringWithdrawal = true,
              withdrawalReason = "Superseded withdrawal",
              paymentStatus = "Collected",
              supersededBy = Some("8888888888"),
              supersede = Some(WithdrawalSuperseded(
                originalTransactionId = "0987654321",
                originalWithdrawalChargeAmount = 2000,
                transactionResult = -1000,
                reason = "Withdrawal reduction"
              ))
            )
          }
        )
      }

      "given a valid superseding withdrawal refund" in {
        val json = validWithdrawalJson ++ Json.obj(
          "supersededTransactionId" -> "0987654321",
          "supersededTransactionAmount" -> 1000,
          "supersededTransactionResult" -> -1000,
          "supersededReason" -> "WITHDRAWAL_REFUND",
          "withdrawalReason" -> "SUPERSEDED_WITHDRAWAL_CHARGE",
          "withdrawalAmount" -> 0,
          "withdrawalChargeAmount" -> 0,
          "withdrawalChargeAmountYtd" -> 0,
          "automaticRecoveryAmount" -> 0
        )

        val result = json.validate[GetBonusOrWithdrawalResponse]

        result.fold(
          errors => fail(errors.toString()),
          success => {
            success mustBe GetWithdrawalResponse(
              creationDate = new DateTime("2018-02-20"),
              periodStartDate = new DateTime("2018-01-06"),
              periodEndDate = new DateTime("2018-02-05"),
              automaticRecoveryAmount = Some(0),
              withdrawalAmount = 0,
              withdrawalChargeAmount = 0,
              withdrawalChargeAmountYtd = 0,
              fundsDeductedDuringWithdrawal = true,
              withdrawalReason = "Superseded withdrawal",
              paymentStatus = "Collected",
              supersededBy = Some("8888888888"),
              supersede = Some(WithdrawalSuperseded(
                originalTransactionId = "0987654321",
                originalWithdrawalChargeAmount = 1000,
                transactionResult = -1000,
                reason = "Withdrawal refund"
              ))
            )
          }
        )
      }

    }

  }

}