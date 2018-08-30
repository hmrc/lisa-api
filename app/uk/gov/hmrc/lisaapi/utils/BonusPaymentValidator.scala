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

package uk.gov.hmrc.lisaapi.utils

import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models.{Amount, BonusRecovery, RequestBonusPaymentRequest}
import uk.gov.hmrc.lisaapi.services.CurrentDateService

import scala.collection.mutable.ListBuffer

case class BonusPaymentValidationRequest(data: RequestBonusPaymentRequest, errors: Seq[ErrorValidation] = Nil)

trait BonusPaymentValidator extends LisaConstants {

  val inboundPayments: String = "/inboundPayments"
  val htbTransfer: String = "/htbTransfer"
  val bonuses: String = "/bonuses"
  val currentDateService: CurrentDateService

  def validate(data: RequestBonusPaymentRequest): Seq[ErrorValidation] = {
    (
      htbTransferTotalYTDGtZeroIfHtbTransferInForPeriodGtZero andThen
      totalSubsYTDGteTotalSubsForPeriod andThen
      newSubsYTDGtZeroIfNewSubsForPeriodGtZero andThen
      newSubsOrHtbTransferGtZero andThen
      totalSubsForPeriodGtZero andThen
      bonusDueForPeriodGtZero andThen
      totalBonusDueYTDGtZero andThen
      bonusRecoveryNewSubsGteZero andThen
      bonusRecoveryHtbTransferGteZero andThen
      bonusRecoveryTotalSubsForPeriodGteZero andThen
      bonusRecoveryBonusDueForPeriodGteZero andThen
      bonusRecoveryTotalBonusDueYTDGteZero andThen
      periodStartDateIsSixth andThen
      periodEndDateIsFifthOfMonthAfterPeriodStartDate andThen
      periodStartDateIsNotInFuture andThen
      periodStartDateIsNotBeforeFirstValidDate andThen
      periodEndDateIsNotBeforeFirstValidDate andThen
      supersedeExistsIfSupersedingClaimReason
    ).apply(BonusPaymentValidationRequest(data)).errors
  }

  private def isBonusRecovery(data: RequestBonusPaymentRequest): Boolean = {
    data.supersede match {
      case Some(_: BonusRecovery) => true
      case _ => false
    }
  }

  private val newSubsOrHtbTransferGtZero: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest = (req: BonusPaymentValidationRequest) => {
    if (isBonusRecovery(req.data)) {
      req
    }
    else {
      val subsExists = req.data.inboundPayments.newSubsForPeriod.isDefined
      val htbExists = req.data.htbTransfer.isDefined

      val subsGtZero = req.data.inboundPayments.newSubsForPeriod.isDefined && req.data.inboundPayments.newSubsForPeriod.get > 0
      val htbGtZero = req.data.htbTransfer.isDefined && req.data.htbTransfer.get.htbTransferInForPeriod > 0
      val eitherGtZero = subsGtZero || htbGtZero

      val newErrs = if (eitherGtZero) req.errors else getErrors(subsExists, htbExists, eitherGtZero)

      req.copy(errors = newErrs)
    }
  }

  private val newSubsYTDGtZeroIfNewSubsForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (
      req.data.inboundPayments.newSubsForPeriod.isDefined &&
      req.data.inboundPayments.newSubsForPeriod.get > 0 &&
      req.data.inboundPayments.newSubsYTD <= 0) => {

      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "newSubsYTD must be more than 0", Some(s"$inboundPayments/newSubsYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val htbTransferTotalYTDGtZeroIfHtbTransferInForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (
      req.data.htbTransfer.isDefined &&
      req.data.htbTransfer.get.htbTransferInForPeriod > 0 &&
      req.data.htbTransfer.get.htbTransferTotalYTD <= 0) => {

      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "htbTransferTotalYTD must be more than 0", Some(s"$htbTransfer/htbTransferTotalYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val totalSubsForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.inboundPayments.totalSubsForPeriod <= 0 && !isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "totalSubsForPeriod must be more than 0", Some(s"$inboundPayments/totalSubsForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val totalSubsYTDGteTotalSubsForPeriod: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.inboundPayments.totalSubsYTD < req.data.inboundPayments.totalSubsForPeriod) => {
      req.copy(errors = req.errors :+
        ErrorValidation(MONETARY_ERROR, "totalSubsYTD must be more than or equal to totalSubsForPeriod", Some(s"$inboundPayments/totalSubsYTD"))
      )
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val bonusDueForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.bonuses.bonusDueForPeriod <= 0 && !isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "bonusDueForPeriod must be more than 0", Some(s"$bonuses/bonusDueForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val totalBonusDueYTDGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.bonuses.totalBonusDueYTD <= 0 && !isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "totalBonusDueYTD must be more than 0", Some(s"$bonuses/totalBonusDueYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }




  private val bonusRecoveryNewSubsGteZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.inboundPayments.newSubsForPeriod.getOrElse[Amount](0) < 0 && isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "newSubsForPeriod cannot be less than 0", Some(s"$inboundPayments/newSubsForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }
  private val bonusRecoveryHtbTransferGteZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.htbTransfer.fold(BigDecimal(0))(_.htbTransferInForPeriod) < 0 && isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "htbTransferInForPeriod cannot be less than 0", Some(s"$htbTransfer/htbTransferInForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }
  private val bonusRecoveryTotalSubsForPeriodGteZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.inboundPayments.totalSubsForPeriod < 0 && isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "totalSubsForPeriod cannot be less than 0", Some(s"$inboundPayments/totalSubsForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }
  private val bonusRecoveryBonusDueForPeriodGteZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.bonuses.bonusDueForPeriod < 0 && isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "bonusDueForPeriod cannot be less than 0", Some(s"$bonuses/bonusDueForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }
  private val bonusRecoveryTotalBonusDueYTDGteZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.bonuses.totalBonusDueYTD < 0 && isBonusRecovery(req.data)) => {
      req.copy(errors = req.errors :+ ErrorValidation(MONETARY_ERROR, "totalBonusDueYTD cannot be less than 0", Some(s"$bonuses/totalBonusDueYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }















  private val periodStartDateIsSixth: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if req.data.periodStartDate.getDayOfMonth() != 6 => {
      req.copy(errors = req.errors :+ ErrorValidation(DATE_ERROR, "The periodStartDate must be the 6th day of the month", Some(s"/periodStartDate")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val periodStartDateIsNotInFuture: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if req.data.periodStartDate.toDate.after(currentDateService.now().toDate) => {
      req.copy(errors = req.errors :+ ErrorValidation(DATE_ERROR, "The periodStartDate may not be a future date", Some(s"/periodStartDate")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val periodEndDateIsFifthOfMonthAfterPeriodStartDate: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest =
                                                        (req: BonusPaymentValidationRequest) => {
    val monthBeforeEnd = req.data.periodEndDate.minusMonths(1)
    val endDateIsValid = req.data.periodEndDate.getDayOfMonth() == 5 &&
                         req.data.periodStartDate.getYear() == monthBeforeEnd.getYear() &&
                         req.data.periodStartDate.getMonthOfYear() == monthBeforeEnd.getMonthOfYear()

    if (endDateIsValid) {
      req
    }
    else {
      req.copy(errors = req.errors :+ ErrorValidation(
        errorCode = DATE_ERROR,
        message = "The periodEndDate must be the 5th day of the month which occurs after the periodStartDate",
        path = Some(s"/periodEndDate")
      ))
    }
  }

  private val periodStartDateIsNotBeforeFirstValidDate: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest =
    (req: BonusPaymentValidationRequest) => {

    if (req.data.periodStartDate.isBefore(LISA_START_DATE)) {
      req.copy(errors = req.errors :+ ErrorValidation(
        errorCode = DATE_ERROR,
        message = LISA_START_DATE_ERROR.format("periodStartDate"),
        path = Some(s"/periodStartDate")
      ))
    }
    else {
      req
    }
  }

  private val periodEndDateIsNotBeforeFirstValidDate: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest =
    (req: BonusPaymentValidationRequest) => {

    if (req.data.periodEndDate.isBefore(LISA_START_DATE)) {
      req.copy(errors = req.errors :+ ErrorValidation(
        errorCode = DATE_ERROR,
        message = LISA_START_DATE_ERROR.format("periodEndDate"),
        path = Some(s"/periodEndDate")
      ))
    }
    else {
      req
    }
  }

  private val supersedeExistsIfSupersedingClaimReason: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest =
    (req: BonusPaymentValidationRequest) => {
      (req.data.bonuses.claimReason, req.data.supersede) match {
        case ("Superseding bonus claim", None) => {
          req.copy(errors = req.errors :+ ErrorValidation(
            errorCode = MISSING_ERROR,
            message = "This field is required",
            path = Some(s"/supersede")
          ))
        }
        case _ => {
          req
        }
      }
    }

  private def getErrors(subsExists: Boolean, htbExists: Boolean, eitherGtZero: Boolean): Seq[ErrorValidation] = {
    val newErrs = new ListBuffer[ErrorValidation]()

    val showSubError = !subsExists && !htbExists || subsExists && !eitherGtZero
    val showHtbError = !subsExists && !htbExists || htbExists && !eitherGtZero

    val errorMessage = "newSubsForPeriod and htbTransferInForPeriod cannot both be 0"

    if (showSubError) newErrs += ErrorValidation(MONETARY_ERROR, errorMessage, Some(s"$inboundPayments/newSubsForPeriod"))
    if (showHtbError) newErrs += ErrorValidation(MONETARY_ERROR, errorMessage, Some(s"$htbTransfer/htbTransferInForPeriod"))

    newErrs
  }

}

object BonusPaymentValidator extends BonusPaymentValidator {
  val currentDateService: CurrentDateService = CurrentDateService
}
