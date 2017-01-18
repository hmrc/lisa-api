package unit.controllers

import org.scalatest.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.play.test.UnitSpec

class ErrorResponseSpec extends UnitSpec with Matchers{
  "errorResponse" should {
    "be translated to error Json with only the required fields" in {
      Json.toJson(ErrorAcceptHeaderInvalid).toString() shouldBe
        """{"code":"ACCEPT_HEADER_INVALID","message":"The accept header is missing or invalid"}"""
    }
  }

}
