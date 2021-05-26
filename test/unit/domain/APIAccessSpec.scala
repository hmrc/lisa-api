/*
 * Copyright 2021 HM Revenue & Customs
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

package unit.domain

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.lisaapi.domain.APIAccess

class APIAccessSpec extends PlaySpec {

  val expectedJson: JsValue = Json.parse(s"""{\"type\":\"PRIVATE\"}""")
  val access: APIAccess = APIAccess("PRIVATE")

  "With APIAccess spec " should {
    "Write a valid json " in {
      Json.toJson[APIAccess](access) must be (expectedJson)
    }
  }
}
