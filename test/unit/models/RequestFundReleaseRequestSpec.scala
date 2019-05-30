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

package unit.models

import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.models._

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

      val input = FundReleasePropertyDetails(
        nameOrNumber = "1",
        postalCode = "AA11 1AA"
      )
      val expected = """{"nameOrNumber":"1","postalCode":"AA11 1AA"}"""

      Json.toJson[FundReleasePropertyDetails](input).toString() mustBe expected

    }

  }

  "InitialFundReleaseRequest" must {

    "serialize from json" in {

      val json = """{"eventDate":"2017-05-10","withdrawalAmount":4000,"conveyancerReference":"CR12345-6789","propertyDetails":{"nameOrNumber":"1","postalCode":"AA11 1AA"}}"""

      Json.parse(json).as[RequestFundReleaseRequest] mustBe InitialFundReleaseRequest(
        eventDate = new DateTime("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = Some(FundReleasePropertyDetails(
          nameOrNumber = "1",
          postalCode = "AA11 1AA"
        ))
      )

    }

    "deserialize to json" in {

      val input = InitialFundReleaseRequest(
        eventDate = new DateTime("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = Some(FundReleasePropertyDetails(
          nameOrNumber = "1",
          postalCode = "AA11 1AA"
        ))
      )
      val expected = """{"eventType":"Funds Release","eventDate":"2017-05-10","withdrawalAmount":4000,"conveyancerReference":"CR12345-6789","propertyDetails":{"nameOrNumber":"1","postalCode":"AA11 1AA"}}"""

      Json.toJson[RequestFundReleaseRequest](input).toString() mustBe expected

    }

    "deserialize to json without FundReleasePropertyDetails" in {

      val input = InitialFundReleaseRequest(
        eventDate = new DateTime("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = Some("CR12345-6789"),
        propertyDetails = None
      )
      val expected = """{"eventType":"Funds Release","eventDate":"2017-05-10","withdrawalAmount":4000,"conveyancerReference":"CR12345-6789"}"""

      Json.toJson[RequestFundReleaseRequest](input).toString() mustBe expected

    }

    "deserialize to json without conveyancerReference" in {

      val input = InitialFundReleaseRequest(
        eventDate = new DateTime("2017-05-10"),
        withdrawalAmount = 4000,
        conveyancerReference = None,
        propertyDetails = Some(FundReleasePropertyDetails(
          nameOrNumber = "1",
          postalCode = "AA11 1AA"
        ))
      )
      val expected = """{"eventType":"Funds Release","eventDate":"2017-05-10","withdrawalAmount":4000,"propertyDetails":{"nameOrNumber":"1","postalCode":"AA11 1AA"}}"""

      Json.toJson[RequestFundReleaseRequest](input).toString() mustBe expected

    }

  }

  "SupersedeFundReleaseRequest" must {

    "serialize from json" in {

      val json = """{"eventDate":"2017-05-05","withdrawalAmount":5000,"supersede":{"originalLifeEventId":"3456789000","originalEventDate":"2017-05-10"}}"""

      Json.parse(json).as[RequestFundReleaseRequest] mustBe SupersedeFundReleaseRequest(
        eventDate = new DateTime("2017-05-05"),
        withdrawalAmount = 5000,
        supersede = FundReleaseSupersedeDetails(
          originalLifeEventId = "3456789000",
          originalEventDate = new DateTime("2017-05-10")
        )
      )

    }

    "deserialize to json" in {

      val input = SupersedeFundReleaseRequest(
        eventDate = new DateTime("2017-05-05"),
        withdrawalAmount = 5000,
        supersede = FundReleaseSupersedeDetails(
          originalLifeEventId = "3456789000",
          originalEventDate = new DateTime("2017-05-10")
        )
      )
      val expected = """{"eventType":"Funds Release","eventDate":"2017-05-05","withdrawalAmount":5000,"supersededLifeEventDate":"2017-05-10","supersededLifeEventID":"3456789000"}"""

      Json.toJson[RequestFundReleaseRequest](input).toString() mustBe expected

    }

  }

}
