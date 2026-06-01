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

package uk.gov.hmrc.lisaapi.connectors

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.helpers.ConnectorSpecHelper
import uk.gov.hmrc.lisaapi.models.hip.*

trait HipConnectorTestHelper extends ConnectorSpecHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val validHipBadRequestJson: String =
    """{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "BAD_REQUEST",
      |        "reason": "Invalid request"
      |      }
      |    ]
      |  }
      |}""".stripMargin

  val validValidationErrorJson: String =
    """{
      |  "errors": {
      |    "processingDate": "2026-04-01T23:00:00Z",
      |    "code": "003",
      |    "text": "Request could not be processed"
      |  }
      |}""".stripMargin

  val validServiceUnavailableHodJson: String =
    """{
      |  "origin": "HoD",
      |  "response": {
      |    "error": {
      |      "code": "500",
      |      "message": "string",
      |      "logID": "D82EBAB67AC6D7565C0682CA91BDC577"
      |    }
      |  }
      |}""".stripMargin

  val validServiceUnavailableJson: String =
    """{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "SERVICE_UNAVAILABLE",
      |        "reason": "Dependent services maybe down"
      |      }
      |    ]
      |  }
      |}""".stripMargin

  val validServerErrorJson: String =
    """{
      |  "origin": "HIP",
      |  "response": {
      |    "failures": [
      |      {
      |        "type": "INTERNAL_SERVER_ERROR",
      |        "reason": "Internal server error"
      |      }
      |    ]
      |  }
      |}""".stripMargin

  val expectedServiceUnavailable = HipServiceUnavailable(
    response = HipFailures(
      failures = Seq(
        HipError(`type` = "SERVICE_UNAVAILABLE", reason = "Dependent services maybe down")
      )
    )
  )

  val expectedServerError = HipServerError(
    response = HipFailures(
      failures = Seq(
        HipError(`type` = "INTERNAL_SERVER_ERROR", reason = "Internal server error")
      )
    )
  )

  val expectedBadRequestError = HipBadRequest(
    response = HipFailures(
      failures = Seq(
        HipError(`type` = "BAD_REQUEST", reason = "Invalid request")
      )
    )
  )

  val expectedValidationError = HipValidationError(
    errors = Hip422Error(
      processingDate = "2026-04-01T23:00:00Z",
      code = "003",
      text = "Request could not be processed"
    )
  )

}
