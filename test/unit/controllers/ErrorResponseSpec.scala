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

package unit.controllers

import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.controllers.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.play.test.UnitSpec

class ErrorResponseSpec extends UnitSpec with Matchers{
  "errorResponse" should {
    "be translated to error Json with only the required fields" in {
      Json.toJson(ErrorAcceptHeaderInvalid).toString() shouldBe
        """{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}"""
    }
  }

}
