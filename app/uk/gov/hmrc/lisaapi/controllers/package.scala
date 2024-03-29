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

package uk.gov.hmrc.lisaapi

import play.api.libs.json.{Json, Writes}

import java.time.LocalDate

package object controllers {

  implicit val errorValidationWrite: Writes[ErrorValidation] =
    (e: ErrorValidation) => Json.obj("code" -> e.errorCode, "message" -> e.message, "path" -> e.path)

  implicit val errorResponseWrites: Writes[ErrorResponse] = {
    case e: ErrorResponseWithAccountId =>
      Json.obj("code" -> e.errorCode, "message" -> e.message, "accountId" -> e.accountId)
    case e: ErrorResponseWithErrors => Json.obj("code" -> e.errorCode, "message" -> e.message, "errors" -> e.errors)
    case e: ErrorResponseWithId => Json.obj("code" -> e.errorCode, "message" -> e.message, "id" -> e.id)
    case e: ErrorResponseWithLifeEventId =>
      Json.obj("code" -> e.errorCode, "message" -> e.message, "lifeEventId" -> e.lifeEventID)
    case e: ErrorResponseWithTransactionId =>
      Json.obj("code" -> e.errorCode, "message" -> e.message, "transactionId" -> e.transactionId)
    case e => Json.obj("code" -> e.errorCode, "message" -> e.message)
  }
}

trait LisaConstants {
  val ACCOUNTID: String     = "accountId"
  val ZREF: String          = "lisaManagerReferenceNumber"
  val NOTIFICATION: String  = "lateNotification"
  val DATE_ERROR            = "INVALID_DATE"
  val MISSING_ERROR         = "MISSING_FIELD"
  val MONETARY_ERROR        = "INVALID_MONETARY_AMOUNT"
  val LISA_START_DATE       = LocalDate.parse("2017-04-06")
  val LISA_START_DATE_ERROR = "The %s cannot be before 6 April 2017"
  val VERSION_1             = "1.0"
  val VERSION_2             = "2.0"
  val TAX_YEAR_START_MONTH  = 4
  val TAX_YEAR_START_DAY    = 6
}
