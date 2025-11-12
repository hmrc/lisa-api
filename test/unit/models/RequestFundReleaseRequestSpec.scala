/*
 * Copyright 2025 HM Revenue & Customs
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

package unit.models

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.controllers.ErrorValidation
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.utils.ErrorConverter

import java.time.LocalDate

class RequestFundReleaseRequestSpec extends PlaySpec {

  "FundReleasePropertyDetails" must {

    "serialize from json" in {

      val json = """{"nameOrNumber":"1","postalCode":"AA11 1AA"}"""

      Json.parse(json).as[FundReleasePropertyDetails] mustBe FundReleasePropertyDetails(
        nameOrNumber = "1",
        postalCode = "AA11 1AA"
      )

    }

    "deserialize to json" in {

      val input    = FundReleasePropertyDetails(
        nameOrNumber = "1",
        postalCode = "AA11 1AA"
      )
      val expected = """{"nameOrNumber":"1","postalCode":"AA11 1AA"}"""

      Json.toJson[FundReleasePropertyDetails](input).toString() mustBe expected

    }

  }

  "InitialFundReleaseRequest" must {

    "serialize from json" in {

      val json =
        """{"eventDate":"2017-05-10","withdrawalAmount":4000,"conveyancerReference":"CR12345-6789","propertyDetails":{"nameOrNumber":"1","postalCode":"AA11 1AA"}}"""

      Json.parse(json).as[RequestFundReleaseRequest] mustBe InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = Some(
          FundReleasePropertyDetails(
            nameOrNumber = "1",
            postalCode = "AA11 1AA"
          )
        )
      )

    }

    "deserialize to json" in {

      val input    = InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = Some(
          FundReleasePropertyDetails(
            nameOrNumber = "1",
            postalCode = "AA11 1AA"
          )
        )
      )
      val expected =
        """{"eventType":"Funds Release","eventDate":"2017-05-10","withdrawalAmount":4000,"conveyancerReference":"CR12345-6789","propertyDetails":{"nameOrNumber":"1","postalCode":"AA11 1AA"}}"""

      Json.toJson(input)(RequestFundReleaseRequest.initialWrites).toString() mustBe expected

    }

    "deserialize to json without FundReleasePropertyDetails" in {

      val input    = InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = None
      )
      val expected =
        """{"eventType":"Funds Release","eventDate":"2017-05-10","withdrawalAmount":4000,"conveyancerReference":"CR12345-6789"}"""

      Json.toJson(input)(RequestFundReleaseRequest.initialWrites).toString() mustBe expected

    }

    "deserialize to json without conveyancerReference" in {

      val input    = InitialFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = None,
        propertyDetails = Some(
          FundReleasePropertyDetails(
            nameOrNumber = "1",
            postalCode = "AA11 1AA"
          )
        )
      )
      val expected =
        """{"eventType":"Funds Release","eventDate":"2017-05-10","withdrawalAmount":4000,"propertyDetails":{"nameOrNumber":"1","postalCode":"AA11 1AA"}}"""

      Json.toJson(input)(RequestFundReleaseRequest.initialWrites).toString() mustBe expected

    }

  }

  "SupersedeFundReleaseRequest" must {

    "serialize from json" in {

      val json =
        """{"eventDate":"2017-05-05","withdrawalAmount":5000,"supersede":{"originalLifeEventId":"3456789000","originalEventDate":"2017-05-10"}}"""

      Json.parse(json).as[RequestFundReleaseRequest] mustBe SupersedeFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-05"),
        withdrawalAmount = 5000,
        supersede = FundReleaseSupersedeDetails(
          originalLifeEventId = "3456789000",
          originalEventDate = LocalDate.parse("2017-05-10")
        )
      )

    }

    "deserialize to json" in {

      val input    = SupersedeFundReleaseRequest(
        eventDate = LocalDate.parse("2017-05-05"),
        withdrawalAmount = 5000,
        supersede = FundReleaseSupersedeDetails(
          originalLifeEventId = "3456789000",
          originalEventDate = LocalDate.parse("2017-05-10")
        )
      )
      val expected =
        """{"eventType":"Funds Release","eventDate":"2017-05-05","withdrawalAmount":5000,"supersededLifeEventDate":"2017-05-10","supersededLifeEventID":"3456789000"}"""

      Json.toJson(input)(RequestFundReleaseRequest.supersedeWrites).toString() mustBe expected

    }

  }

  private def extractErrorValidation(result: JsResult[FundReleasePropertyDetails]) =
    result match {
      case JsError(errors) => ErrorConverter.convert(errors)
      case _               => throw new Exception("no error message found")
    }

  private def createJson(nameOrNumber: String, postalCode: String): JsObject =
    Json.obj(
      "nameOrNumber" -> nameOrNumber,
      "postalCode"   -> postalCode
    )

  "validate nameOrNumber" when {

    "nameOrNumber is Empty" in {

      val invalidNameOrNumberJson = createJson("", "AA11 1AA")

      extractErrorValidation(invalidNameOrNumberJson.validate[FundReleasePropertyDetails]) must contain(
        ErrorValidation("INVALID_NAME_OR_NUMBER", "Enter nameOrNumber", Some("/nameOrNumber"))
      )
    }

    "nameOrNumber is 36 characters long" in {

      val invalidNameOrNumberJson = createJson("a" * 36, "AA11 1AA")

      extractErrorValidation(invalidNameOrNumberJson.validate[FundReleasePropertyDetails]) must contain(
        ErrorValidation("INVALID_NAME_OR_NUMBER", "nameOrNumber must be 35 characters or less", Some("/nameOrNumber"))
      )
    }

    "nameOrNumber is 35 characters long" in {

      val validNameOrNumberJson = createJson("a" * 35, "AA11 1AA")

      validNameOrNumberJson.validate[FundReleasePropertyDetails] must be(
        JsSuccess(FundReleasePropertyDetails("a" * 35, "AA11 1AA"))
      )
    }

    "nameOrNumber is invalid" in {

      val invalidNameOrNumberJson = createJson("%%%%%", "AA11 1AA")

      extractErrorValidation(invalidNameOrNumberJson.validate[FundReleasePropertyDetails]) must contain(
        ErrorValidation(
          "INVALID_NAME_OR_NUMBER",
          "nameOrNumber must only include letters a to z, numbers 0 to 9, colons, forward slashes, hyphen and spaces",
          Some("/nameOrNumber")
        )
      )
    }

    "nameOrNumber is valid" in {

      val validNameOrNumberJson = createJson("007 Park Avenue", "AA11 1AA")

      validNameOrNumberJson.validate[FundReleasePropertyDetails] must be(
        JsSuccess(FundReleasePropertyDetails("007 Park Avenue", "AA11 1AA"))
      )
    }
  }

  "validate postalCode" when {

    "postalCode is Empty" in {

      val invalidPostalCodeJson = createJson("007 Park Avenue", "")

      extractErrorValidation(invalidPostalCodeJson.validate[FundReleasePropertyDetails]) must contain(
        ErrorValidation("INVALID_POSTAL_CODE", "Enter a postcode", Some("/postalCode"))
      )
    }

    "postalCode is having invalid characters" in {

      val invalidPostalCodeJson = createJson("007 Park Avenue", "AA|2 4AA")

      extractErrorValidation(invalidPostalCodeJson.validate[FundReleasePropertyDetails]) must contain(
        ErrorValidation(
          "INVALID_POSTAL_CODE",
          "Postcode must only include letters a to z and numbers 0 to 9, like AA1 1AA",
          Some("/postalCode")
        )
      )
    }

    "postalCode is valid" in {

      val validPostalCodeJson = createJson("007 Park Avenue", "AA11 1AA")

      validPostalCodeJson.validate[FundReleasePropertyDetails] must be(
        JsSuccess(FundReleasePropertyDetails("007 Park Avenue", "AA11 1AA"))
      )
    }
  }

}
