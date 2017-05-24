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

import play.api.libs.functional.syntax.unlift
import play.api.libs.json.{JsPath, JsValue, Json, Writes}
import play.api.libs.functional.syntax._
import uk.gov.hmrc.lisaapi.controllers.ErrorResponse

package object controllers {
  implicit val errorResponseWrites = new Writes[ErrorResponse] {
    def writes(e: ErrorResponse): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message)
  }

//  implicit val errorResponseWrites: Writes[ErrorResponse] = (
//    (JsPath \ "code").write[String] and
//    (JsPath \ "message").write[String] and
//    (JsPath \ "errors").writeNullable[Seq[ErrorValidation]]
//    )(unlift(uk.gov.hmrc.lisaapi.controllers.ErrorResponse.unapply()))


  implicit val errorValidationWrite = new Writes[ErrorValidation] {
    def writes(e: ErrorValidation): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message, "path" -> e.path)
  }

  implicit val errorResponseWithIdWrites = new Writes[ErrorResponseWithId] {
    def writes(e: ErrorResponseWithId): JsValue = Json.obj("code" -> e.errorCode, "message" -> e.message, "id" -> e.id)
  }
}
trait LisaConstants {
  val ZREF: String =  "lisaManagerReferenceNumber"
}
