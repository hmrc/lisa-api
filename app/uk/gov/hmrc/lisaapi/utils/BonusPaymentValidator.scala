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

package uk.gov.hmrc.lisaapi.utils

import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models.RequestBonusPaymentRequest

import scala.collection.mutable.ListBuffer

case class BonusPaymentValidationRequest(data: RequestBonusPaymentRequest, errors: Seq[ErrorValidation] = Nil)

trait BonusPaymentValidator {

  val inboundPayments: String = "/inboundPayments"
  val htbTransfer: String = "/htbTransfer"
  val bonuses: String = "/bonuses"
  val errorCode: String = "INVALID_MONETARY_AMOUNT"

  def validate(data: RequestBonusPaymentRequest): Seq[ErrorValidation] = {
    (
      newSubsOrHtbTransferGtZero andThen
      newSubsYTDGtZeroIfNewSubsForPeriodGtZero andThen
      htbTransferTotalYTDGtZeroIfHtbTransferInForPeriodGtZero andThen
      totalSubsForPeriodGtZero andThen
      totalSubsYTDGteTotalSubsForPeriod andThen
      bonusDueForPeriodGtZero andThen
      totalBonusDueYTDGtZero
    ).apply(BonusPaymentValidationRequest(data)).errors
  }

  private val newSubsOrHtbTransferGtZero: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest = (req: BonusPaymentValidationRequest) => {
    val subsExists = req.data.inboundPayments.newSubsForPeriod.isDefined
    val htbExists = req.data.htbTransfer.isDefined

    val subsGtZero = req.data.inboundPayments.newSubsForPeriod.isDefined && req.data.inboundPayments.newSubsForPeriod.get > 0
    val htbGtZero = req.data.htbTransfer.isDefined && req.data.htbTransfer.get.htbTransferInForPeriod > 0
    val eitherGtZero = subsGtZero || htbGtZero

    val newErrs = if (eitherGtZero) req.errors else getErrors(subsExists, htbExists, eitherGtZero)

    req.copy(errors = newErrs)
  }

  private val newSubsYTDGtZeroIfNewSubsForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (
      req.data.inboundPayments.newSubsForPeriod.isDefined &&
      req.data.inboundPayments.newSubsForPeriod.get > 0 &&
      req.data.inboundPayments.newSubsYTD <= 0) => {

      req.copy(errors = req.errors :+ ErrorValidation(errorCode, "newSubsYTD must be greater than zero", Some(s"$inboundPayments/newSubsYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val htbTransferTotalYTDGtZeroIfHtbTransferInForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (
      req.data.htbTransfer.isDefined &&
      req.data.htbTransfer.get.htbTransferInForPeriod > 0 &&
      req.data.htbTransfer.get.htbTransferTotalYTD <= 0) => {

      req.copy(errors = req.errors :+ ErrorValidation(errorCode, "htbTransferTotalYTD must be greater than zero", Some(s"$htbTransfer/htbTransferTotalYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val totalSubsForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.inboundPayments.totalSubsForPeriod <= 0) => {
      req.copy(errors = req.errors :+ ErrorValidation(errorCode, "totalSubsForPeriod must be greater than zero", Some(s"$inboundPayments/totalSubsForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val totalSubsYTDGteTotalSubsForPeriod: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.inboundPayments.totalSubsYTD < req.data.inboundPayments.totalSubsForPeriod) => {
      req.copy(errors = req.errors :+
        ErrorValidation(errorCode, "totalSubsYTD must be greater than or equal to totalSubsForPeriod", Some(s"$inboundPayments/totalSubsYTD"))
      )
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val bonusDueForPeriodGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.bonuses.bonusDueForPeriod <= 0) => {
      req.copy(errors = req.errors :+ ErrorValidation(errorCode, "bonusDueForPeriod must be greater than zero", Some(s"$bonuses/bonusDueForPeriod")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private val totalBonusDueYTDGtZero: PartialFunction[BonusPaymentValidationRequest, BonusPaymentValidationRequest] = {
    case req: BonusPaymentValidationRequest if (req.data.bonuses.totalBonusDueYTD <= 0) => {
      req.copy(errors = req.errors :+ ErrorValidation(errorCode, "totalBonusDueYTD must be greater than zero", Some(s"$bonuses/totalBonusDueYTD")))
    }
    case req: BonusPaymentValidationRequest => req
  }

  private def getErrors(subsExists: Boolean, htbExists: Boolean, eitherGtZero: Boolean): Seq[ErrorValidation] = {
    val newErrs = new ListBuffer[ErrorValidation]()

    val showSubError = !subsExists && !htbExists || subsExists && !eitherGtZero
    val showHtbError = !subsExists && !htbExists || htbExists && !eitherGtZero

    val errorMessage = "newSubsForPeriod and htbTransferInForPeriod cannot both be zero"

    if (showSubError) newErrs += ErrorValidation(errorCode, errorMessage, Some(s"$inboundPayments/newSubsForPeriod"))
    if (showHtbError) newErrs += ErrorValidation(errorCode, errorMessage, Some(s"$htbTransfer/htbTransferInForPeriod"))

    newErrs
  }

}

object BonusPaymentValidator extends BonusPaymentValidator
