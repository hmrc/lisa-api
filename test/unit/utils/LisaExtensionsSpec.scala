/*
 * Copyright 2020 HM Revenue & Customs
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

package unit.utils

import helpers.BaseTestFixture
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

class LisaExtensionsSpec  extends BaseTestFixture
  with GuiceOneAppPerSuite {

  "LisaExtension" must {

    case class OneOption(first: Option[String])
    case class TestInts(anInt: Int, anOpInt: Option[Int])
    case class TestDouble(aDouble: Double, anOpDouble: Option[Double])
    "Return empty for case class containing one None" in {
      val one = OneOption(None)
      one.toStringMap mustBe Map()
    }

    "Return Map for case with one Value" in {
      val one = OneOption(Some("one"))
      one.toStringMap mustBe Map("first" -> "one")
    }

    "Return Map with int and option int converted to strings" in {
      val testints = TestInts(1,Some(1))
      testints.toStringMap mustBe Map("anInt" -> "1", "anOpInt" -> "1")
    }

    "Return Map with Doubles and Option Doubles converted to strings" in {
      val testdoubles = TestDouble(1.2d, Some(1.2d))
      testdoubles.toStringMap mustBe Map("aDouble" -> "1.2", "anOpDouble" -> "1.2")

    }
  }
}
