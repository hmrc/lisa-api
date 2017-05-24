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
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation

object ErrorConverter {

  def convert(error: Seq[(JsPath, Seq[ValidationError])]):List[ErrorValidation] = {
    error.map(e => {
      val details = getErrorDetails(e._2.head.message)

      ErrorValidation(
        errorCode = details._1,
        message = details._2,
        path = Some(e._1.toString())
      )
    }).toList
  }

  private def getErrorDetails(key: String):(String, String) = {
    key match {
      case f: String if f.matches("error\\.expected\\.js.*") => ("INVALID_DATA_TYPE", "Invalid data type")
      case "error.path.missing" => ("FIELD_MISSING", "Field missing")
    }
  }

}