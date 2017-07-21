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

import play.api.data.validation.ValidationError
import play.api.libs.json.JsPath
import uk.gov.hmrc.lisaapi.models.RequestBonusPaymentRequest

import scala.collection.mutable.ListBuffer

case class BonusPaymentValidationRequest(data: RequestBonusPaymentRequest, errors: Seq[(JsPath, Seq[ValidationError])] = Nil)

object BonusPaymentValidator {

  def validate(data: RequestBonusPaymentRequest): Seq[(JsPath, Seq[ValidationError])] = {
    Function.chain(Seq(
      newSubsYTDGtZeroIfNoNewForPeriod,
      htbTransferTotalYTDGtZeroIfNoTransferForPeriod
    )).apply(BonusPaymentValidationRequest(data)).errors
  }

  val newSubsOrHtbTransferGtZero: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest = (req: BonusPaymentValidationRequest) => {
    val subsExists = req.data.inboundPayments.newSubsForPeriod.isDefined
    val htbExists = req.data.htbTransfer.isDefined

    val subsGtZero = req.data.inboundPayments.newSubsForPeriod.isDefined && req.data.inboundPayments.newSubsForPeriod.get > 0
    val htbGtZero = req.data.htbTransfer.isDefined && req.data.htbTransfer.get.htbTransferInForPeriod > 0
    val eitherGtZero = subsGtZero || htbGtZero

    val newErrs = if (eitherGtZero) req.errors else getErrors(subsExists, htbExists, eitherGtZero)

    req.copy(errors = newErrs)
  }

  val newSubsYTDGtZeroIfNoNewForPeriod: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest = (req: BonusPaymentValidationRequest) => {
    val subsGtZero = req.data.inboundPayments.newSubsForPeriod.isDefined && req.data.inboundPayments.newSubsForPeriod.get > 0

    if (subsGtZero && req.data.inboundPayments.newSubsYTD <= 0) {
      val newErrors = req.errors :+ ((JsPath \ "inboundPayments" \ "newSubsYTD", Seq(ValidationError("newSubsYTD must be greater than zero"))))

      req.copy(errors = newErrors)
    }
    else {
      req
    }
  }

  val htbTransferTotalYTDGtZeroIfNoTransferForPeriod: (BonusPaymentValidationRequest) => BonusPaymentValidationRequest =
    (req: BonusPaymentValidationRequest) => {

    val htbGtZero = req.data.htbTransfer.isDefined && req.data.htbTransfer.get.htbTransferInForPeriod > 0

    if (htbGtZero && req.data.htbTransfer.get.htbTransferTotalYTD <= 0) {
      val newErrors = req.errors :+ ((JsPath \ "htbTransfer" \ "htbTransferTotalYTD", Seq(ValidationError("htbTransferTotalYTD must be greater than zero"))))

      req.copy(errors = newErrors)
    }
    else {
      req
    }
  }

  private def getErrors(subsExists: Boolean, htbExists: Boolean, eitherGtZero: Boolean): Seq[(JsPath, Seq[ValidationError])] = {
    val newErrs = new ListBuffer[(JsPath, Seq[ValidationError])]()

    val showSubError = !subsExists && !htbExists || subsExists && !eitherGtZero
    val showHtbError = !subsExists && !htbExists || htbExists && !eitherGtZero

    val errorMessage = "newSubsForPeriod and htbTransferForPeriod cannot both be zero"

    if (showSubError) newErrs += ((JsPath \ "inboundPayments" \ "newSubsForPeriod", Seq(ValidationError(errorMessage))))
    if (showHtbError) newErrs += ((JsPath \ "htbTransfer" \ "htbTransferInForPeriod", Seq(ValidationError(errorMessage))))

    newErrs
  }

}
