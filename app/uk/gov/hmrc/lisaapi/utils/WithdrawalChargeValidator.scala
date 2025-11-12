/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.utils

import com.google.inject.Inject
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models.ReportWithdrawalChargeRequest
import uk.gov.hmrc.lisaapi.services.CurrentDateService

case class WithdrawalChargeValidationRequest(data: ReportWithdrawalChargeRequest, errors: Seq[ErrorValidation] = Nil)

class WithdrawalChargeValidator @Inject() (currentDateService: CurrentDateService) extends LisaConstants {

  def validate(data: ReportWithdrawalChargeRequest): Seq[ErrorValidation] =
    (
      periodStartDateIsSixth andThen
        periodEndDateIsFifthOfMonthAfterPeriodStartDate andThen
        periodStartDateIsNotInFuture andThen
        periodStartDateIsNotBeforeFirstValidDate andThen
        periodEndDateIsNotBeforeFirstValidDate andThen
        regularWithdrawalIsNotSupersede andThen
        automaticRecoveryAmountNotEqualToWithdrawalChargeAmountWhenFundsDeducted andThen
        automaticRecoveryAmountLteWithdrawalChargeAmount
    ).apply(WithdrawalChargeValidationRequest(data)).errors

  private val periodStartDateIsSixth
    : PartialFunction[WithdrawalChargeValidationRequest, WithdrawalChargeValidationRequest] = {
    case req: WithdrawalChargeValidationRequest if req.data.claimPeriodStartDate.getDayOfMonth != 6 =>
      req.copy(errors =
        req.errors :+ ErrorValidation(
          DATE_ERROR,
          "The claimPeriodStartDate must be the 6th day of the month",
          Some(s"/claimPeriodStartDate")
        )
      )
    case req: WithdrawalChargeValidationRequest                                                       => req
  }

  private val periodStartDateIsNotInFuture
    : PartialFunction[WithdrawalChargeValidationRequest, WithdrawalChargeValidationRequest] = {
    case req: WithdrawalChargeValidationRequest
        if req.data.claimPeriodStartDate.isAfter(currentDateService.now()) =>
      req.copy(errors =
        req.errors :+ ErrorValidation(
          DATE_ERROR,
          "The claimPeriodStartDate may not be a future date",
          Some(s"/claimPeriodStartDate")
        )
      )
    case req: WithdrawalChargeValidationRequest => req
  }

  private val periodEndDateIsFifthOfMonthAfterPeriodStartDate
    : WithdrawalChargeValidationRequest => WithdrawalChargeValidationRequest =
    (req: WithdrawalChargeValidationRequest) => {
      val monthBeforeEnd = req.data.claimPeriodEndDate.minusMonths(1)
      val endDateIsValid = req.data.claimPeriodEndDate.getDayOfMonth == 5 &&
        req.data.claimPeriodStartDate.getYear == monthBeforeEnd.getYear &&
        req.data.claimPeriodStartDate.getMonth == monthBeforeEnd.getMonth

      if (endDateIsValid) {
        req
      } else {
        req.copy(errors =
          req.errors :+ ErrorValidation(
            errorCode = DATE_ERROR,
            message =
              "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            path = Some(s"/claimPeriodEndDate")
          )
        )
      }
    }

  private val periodStartDateIsNotBeforeFirstValidDate
    : WithdrawalChargeValidationRequest => WithdrawalChargeValidationRequest =
    (req: WithdrawalChargeValidationRequest) =>
      if (req.data.claimPeriodStartDate.isBefore(LISA_START_DATE)) {
        req.copy(errors =
          req.errors :+ ErrorValidation(
            errorCode = DATE_ERROR,
            message = LISA_START_DATE_ERROR.format("claimPeriodStartDate"),
            path = Some(s"/claimPeriodStartDate")
          )
        )
      } else {
        req
      }

  private val periodEndDateIsNotBeforeFirstValidDate
    : WithdrawalChargeValidationRequest => WithdrawalChargeValidationRequest =
    (req: WithdrawalChargeValidationRequest) =>
      if (req.data.claimPeriodEndDate.isBefore(LISA_START_DATE)) {
        req.copy(errors =
          req.errors :+ ErrorValidation(
            errorCode = DATE_ERROR,
            message = LISA_START_DATE_ERROR.format("claimPeriodEndDate"),
            path = Some(s"/claimPeriodEndDate")
          )
        )
      } else {
        req
      }

  private val regularWithdrawalIsNotSupersede
    : WithdrawalChargeValidationRequest => WithdrawalChargeValidationRequest =
    (req: WithdrawalChargeValidationRequest) =>
      if (req.data.supersede.isDefined && req.data.withdrawalReason == "Regular withdrawal") {
        req.copy(errors =
          req.errors :+ ErrorValidation(
            errorCode = "SUPERSEDE_NOT_ALLOWED",
            message = "Supersede details are not allowed",
            path = Some("/withdrawalReason")
          )
        )
      } else {
        req
      }

  private val automaticRecoveryAmountNotEqualToWithdrawalChargeAmountWhenFundsDeducted
    : WithdrawalChargeValidationRequest => WithdrawalChargeValidationRequest =
    (req: WithdrawalChargeValidationRequest) =>
      req.data.automaticRecoveryAmount match {
        case Some(amount) if amount != req.data.withdrawalChargeAmount && req.data.fundsDeductedDuringWithdrawal =>
          req.copy(errors =
            req.errors :+ ErrorValidation(
              errorCode = "AMOUNT_MISMATCH",
              message = "automaticRecoveryAmount and withdrawalChargeAmount must be the same",
              path = Some("/automaticRecoveryAmount")
            )
          )
        case _                                                                                                   => req
      }

  private val automaticRecoveryAmountLteWithdrawalChargeAmount
    : WithdrawalChargeValidationRequest => WithdrawalChargeValidationRequest =
    (req: WithdrawalChargeValidationRequest) =>
      req.data.automaticRecoveryAmount match {
        case Some(amount) if amount > req.data.withdrawalChargeAmount =>
          req.copy(errors =
            req.errors :+ ErrorValidation(
              errorCode = MONETARY_ERROR,
              message = "automaticRecoveryAmount cannot be more than withdrawalChargeAmount",
              path = Some("/automaticRecoveryAmount")
            )
          )
        case _                                                        => req
      }

}
