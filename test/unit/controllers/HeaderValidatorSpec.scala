package unit.controllers

import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class HeaderValidatorSpec extends UnitSpec with Matchers with HeaderValidator{

  "acceptHeaderValidationRules" should {
    "return false when the header value is missing" in {
      acceptHeaderValidationRules(None) shouldBe false
    }
  }

  "acceptHeaderValidationRules" should {
    "return true when the version and the content type in header value is well formatted" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0+json")) shouldBe true
    }
  }

  "acceptHeaderValidationRules" should {
    "return false when the content type in header value is missing" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.1.0")) shouldBe false
    }
  }

  "acceptHeaderValidationRules" should {
    "return false when the content type in header value is not well formatted" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.v1+json")) shouldBe false
    }
  }

  "acceptHeaderValidationRules" should {
    "return false when the content type in header value is not valid" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.notvalid+XML")) shouldBe false
    }
  }

  "acceptHeaderValidationRules" should {
    "return false when the version in header value is not valid" in {
      acceptHeaderValidationRules(Some("application/vnd.hmrc.notvalid+json")) shouldBe false
    }
  }
}

