/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

class LisaExtensionsSpec extends BaseTestFixture with GuiceOneAppPerSuite {

  "LisaExtension" must {

    case class OneOption(first: Option[String])
    case class TestInts(anInt: Int, anOpInt: Option[Int])
    case class TestDouble(aDouble: Double, anOpDouble: Option[Double])
    case class TestFloat(aFloat: Float, anOpFloat: Option[Float])
    case class TestNested(value: String, inner: TestInts)
    case class TestNestedOption(value: String, inner: Option[TestInts])

    "Return empty for case class containing one None" in {
      val one = OneOption(None)
      one.toStringMap mustBe Map()
    }

    "Return Map for case with one Value" in {
      val one = OneOption(Some("one"))
      one.toStringMap mustBe Map("first" -> "one")
    }

    "Return Map with int and option int converted to strings" in {
      val testints = TestInts(1, Some(1))
      testints.toStringMap mustBe Map("anInt" -> "1", "anOpInt" -> "1")
    }

    "Return Map with Doubles and Option Doubles converted to strings" in {
      val testdoubles = TestDouble(1.2d, Some(1.2d))
      testdoubles.toStringMap mustBe Map("aDouble" -> "1.2", "anOpDouble" -> "1.2")
    }

    "Return Map with Floats and Option Floats converted to strings" in {
      val testfloats = TestFloat(1.5f, Some(2.5f))
      testfloats.toStringMap mustBe Map("aFloat" -> "1.5", "anOpFloat" -> "2.5")
    }

    "Return Map with nested Product" in {
      val nested = TestNested("outer", TestInts(42, Some(100)))
      val result = nested.toStringMap
      result("value")            mustBe "outer"
      result.contains("anInt")   mustBe true
      result.contains("anOpInt") mustBe true
    }

    "Return Map with nested Option[Product]" in {
      val nested = TestNestedOption("outer", Some(TestInts(42, None)))
      val result = nested.toStringMap
      result("value")          mustBe "outer"
      result.contains("anInt") mustBe true
    }

    "Return Map with nested Option[Product] as None" in {
      val nested = TestNestedOption("outer", None)
      val result = nested.toStringMap
      result mustBe Map("value" -> "outer")
    }

    "Handle ZonedDateTime conversion" in {
      import java.time.ZonedDateTime
      case class TestDateTime(date: ZonedDateTime)
      val now      = ZonedDateTime.now()
      val testDate = TestDateTime(now)
      testDate.toStringMap("date") mustBe now.toString
    }
  }

}
