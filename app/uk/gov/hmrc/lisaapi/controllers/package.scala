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

package uk.gov.hmrc.lisaapi

import play.api.libs.json.{JsPath, JsValue, Json, Writes}

package object controllers {

  implicit val errorValidationWrite = new Writes[ErrorValidation] {
    def writes(e: ErrorValidation): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message, "path" -> e.path)
  }


  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message, "errors"-> e.errors)
  }


  implicit val errorResponseWithIdWrites = new Writes[ErrorResponseWithId] {
    def writes(e: ErrorResponseWithId): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message, "id" -> e.id)
  }
}
trait LisaConstants {
  val ZREF: String =  "lisaManagerReferenceNumber"
}
