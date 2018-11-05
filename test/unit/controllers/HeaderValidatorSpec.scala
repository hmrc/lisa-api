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

package unit.controllers

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.lisaapi.controllers.HeaderValidator

class HeaderValidatorSpec extends PlaySpec with HeaderValidator {

  "acceptHeaderValidationRules" must {
    "return false when the header value is missing" in {
      acceptHeaderValidationRules(None) mustBe false
    }
    "return true when the version and the content type in header value is well formatted" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0+json")) mustBe true
    }
    "return false when the content type in header value is missing" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0")) mustBe false
    }
    "return false when the content type in header value is not well formatted" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.v1+json")) mustBe false
    }
    "return false when the content type in header value is not valid" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.notvalid+XML")) mustBe false
    }
    "return false when the version in header value is not valid" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.notvalid+json")) mustBe false
    }
    "return false when the version in header value is not one of the available versions" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.9.0")) mustBe false
    }
  }
}

