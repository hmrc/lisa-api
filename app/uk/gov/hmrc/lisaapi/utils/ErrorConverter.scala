/*
 * Copyright 2019 HM Revenue & Customs
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

import play.api.libs.json.{JsPath, JsonValidationError}
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation

trait ErrorConverter {

  def convert(error: Seq[(JsPath, Seq[JsonValidationError])]):List[ErrorValidation] = {
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
      case f: String if f.matches("error\\.expected\\..*") => ("INVALID_DATA_TYPE", "Invalid data type has been used")
      case f2: String if f2.matches("error\\.formatting\\.date.*") => ("INVALID_DATE", "Date is invalid")
      case f2: String if f2.matches("error\\.formatting\\.currencyNegativeDisallowed.*") => ("INVALID_MONETARY_AMOUNT", "Amount cannot be negative, and can only have up to 2 decimal places")
      case f2: String if f2.matches("error\\.formatting\\.currencyNegativeAllowed.*") => ("INVALID_MONETARY_AMOUNT", "Amount can only have up to 2 decimal places")
      case f3: String if f3.matches("error\\.formatting\\.annualFigures.*") => ("INVALID_MONETARY_AMOUNT", "Amount cannot be negative")
      case f4: String if f4.matches("error\\.formatting\\..*") => ("INVALID_FORMAT", "Invalid format has been used")
      case "error.path.missing" => ("MISSING_FIELD", "This field is required")
      case _ => throw new MatchError("Could not match the JSON Validation error")
    }
  }

}

object ErrorConverter extends ErrorConverter