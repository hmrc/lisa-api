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

package uk.gov.hmrc.lisaapi

import org.joda.time.DateTime
import play.api.libs.json.{JsPath, JsValue, Json, Writes}

package object controllers {

  implicit val errorValidationWrite = new Writes[ErrorValidation] {
    def writes(e: ErrorValidation): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message, "path" -> e.path)
  }

  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = e match {
      case e:ErrorResponseWithAccountId => Json.obj("code" -> e.errorCode, "message" -> e.message, "accountId" -> e.accountId)
      case e:ErrorResponseWithErrors => Json.obj("code" -> e.errorCode, "message" -> e.message, "errors" -> e.errors)
      case e:ErrorResponseWithId => Json.obj("code" -> e.errorCode, "message" -> e.message, "id" -> e.id)
      case e:ErrorResponseWithLifeEventId => Json.obj("code" -> e.errorCode, "message" -> e.message, "lifeEventId" -> e.lifeEventID)
      case e:ErrorResponseWithTransactionId => {
        e.transactionId match {
          case Some(transactionId) => Json.obj("code" -> e.errorCode, "message" -> e.message, "transactionId" -> transactionId)
          case None => Json.obj("code" -> e.errorCode, "message" -> e.message)
        }
      }
      case _ => Json.obj ("code" -> e.errorCode, "message" -> e.message)
    }
  }
}

trait LisaConstants {
  val ZREF: String = "lisaManagerReferenceNumber"
  val NOTIFICATION:String  = "lateNotification"
  val DATE_ERROR = "INVALID_DATE"
  val MISSING_ERROR = "MISSING_FIELD"
  val MONETARY_ERROR = "INVALID_MONETARY_AMOUNT"
  val LISA_START_DATE = new DateTime("2017-04-06")
  val LISA_START_DATE_ERROR = "The %s cannot be before 6 April 2017"
  val VERSION_1 = "1.0"
  val VERSION_2 = "2.0"
}
