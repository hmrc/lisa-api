package uk.gov.hmrc.models

import uk.gov.hmrc.play.test.UnitSpec
import uk.gov.hmrc.lisaapi.models.CreationReason

import scala.util.Try


class ModelSpec extends UnitSpec{
  "CreationReason" should {
    "allow only New and Transferred" in {
      Try(CreationReason("New")).isSuccess shouldBe true

      CreationReason("Transferred") shouldBe isInstanceOf[CreationReason]

      Try(CreationReason("Transfer")).isFailure shouldBe true


    }
  }


}
