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

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, RequestId}
import uk.gov.hmrc.lisaapi.models.*
import uk.gov.hmrc.lisaapi.models.des.*
import uk.gov.hmrc.lisaapi.models.hip.{Hip422Error, HipError, HipFailures, HipGetTransactionPaid, HipGetTransactionPending, HipGetTransactionResponse, HipServerError, HipServiceUnavailable, HipValidationError}

import java.time.LocalDate
import java.util.UUID

class HipConnectorSpec extends DesConnectorTestHelper {

  lazy val hipConnector: HipConnector = injector.instanceOf[HipConnector] // lazy to allow wiremock to start


  private val getTransactionUrl = "http://localhost:8080/RESTAdapter/lisa/bonus-charge/manager"

  private val jsonContentType = Map("Content-Type" -> Seq("application/json"))


  private val validValidationErrorJson: String =
    """{
      |  "errors": {
      |    "processingDate": "2026-04-01T23:00:00Z",
      |    "code": "003",
      |    "text": "Request could not be processed"
      |  }
      |}""".stripMargin
  
  
  private val validServiceUnavailableJson: String =
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

  private val validServerErrorJson: String =
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







  private val expectedServiceUnavailable = HipServiceUnavailable(
    response = HipFailures(
      failures = Seq(
        HipError(`type` = "SERVICE_UNAVAILABLE", reason = "Dependent services maybe down")
      )
    )
  )

  private val expectedServerError = HipServerError(
    response = HipFailures(
      failures = Seq(
        HipError(`type` = "INTERNAL_SERVER_ERROR", reason = "Internal server error")
      )
    )
  )

  private val expectedValidationError = HipValidationError(
    errors = Hip422Error(
   processingDate =  "2026-04-01T23:00:00Z",
      code = "003",
      text = "Request could not be processed"
    )
  )


  "parseJsonResponse" must {
    "parse HipServiceUnavailable" in {
        val res = HttpResponse(503, validServiceUnavailableJson, jsonContentType)
        val result = hipConnector.parseJsonResponse[HipServiceUnavailable](res)

        result mustBe expectedServiceUnavailable
    }

    "parse HipServerError" in {
      val res = HttpResponse(500, validServerErrorJson, jsonContentType)
      val result = hipConnector.parseJsonResponse[HipServerError](res)

      result mustBe expectedServerError
    }

    "parse HipValidationError" in {
      val res = HttpResponse(422, validValidationErrorJson, jsonContentType)
      val result = hipConnector.parseJsonResponse[HipValidationError](res)

      result mustBe expectedValidationError
    }



    "parse a PENDING transaction" in {

      val json =
        """{
          |  "paymentStatus": "PENDING",
          |  "paymentDueDate": "2026-05-27"
          |}""".stripMargin
      val res = HttpResponse(200, json, jsonContentType)
      val result = hipConnector.parseJsonResponse[HipGetTransactionResponse](res)

      result mustBe HipGetTransactionPending(paymentDueDate = LocalDate.of(2026, 5, 27))
    }

    "parse a PAID transaction" in {
      val json =
        """{
          |  "paymentStatus":    "PAID",
          |  "paymentDate":      "2026-05-27",
          |  "paymentDueDate":   "2026-05-30",
          |  "paymentReference": "1234567890",
          |  "paymentAmount":    101.00
          |}""".stripMargin


      val res    = HttpResponse(200, json, jsonContentType)
      val result = hipConnector.parseJsonResponse[HipGetTransactionResponse](res)
      result mustBe HipGetTransactionPaid(
        paymentDate      = LocalDate.of(2026, 5, 27),
        paymentDueDate   = LocalDate.of(2026, 5, 30),
        paymentReference = "1234567890",
        paymentAmount    = BigDecimal(101.00)
      )



    }


  }


  "getTransaction" must {
    "return HipServiceUnavailable" in {
      stubForGet(getTransactionUrl, SERVICE_UNAVAILABLE, "")
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe HipServiceUnavailable

    }
  }



//
//  "Retrieve Transaction endpoint" must {
//
//    "return a unavailable response when a 503 is returned" in {
//      stubForGet(getTransactionUrl, SERVICE_UNAVAILABLE, "")
//      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//      case class HipError(code: String, logID: String, message: String)
//      response mustBe HipUnavailableResponse(HipError("503"))
//      verifyDesGet(getTransactionUrl, withOriginator = true)
//    }
//
//    "return a failure response" when {
//
//      "the HIP response is a failure response" in {
//        stubForGet(getTransactionUrl, OK, """{ "code": "ERROR_CODE", "reason" : "ERROR MESSAGE" }""")
//        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//
//        response mustBe HipFailureResponse("ERROR_CODE", "ERROR MESSAGE")
//        verifyDesGet(getTransactionUrl, withOriginator = true)
//      }
//
//      "the DES response has no json body" in {
//        stubForGet(getTransactionUrl, OK, "")
//        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//
//        response mustBe DesFailureResponse()
//        verifyDesGet(getTransactionUrl, withOriginator = true)
//      }
//
//      "the DES response is invalid" in {
//        stubForGet(getTransactionUrl, OK, """{ "status": "Due" }""")
//        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//
//        response mustBe DesFailureResponse()
//        verifyDesGet(getTransactionUrl, withOriginator = true)
//      }
//    }
//
//    "return a success response" when {
//
//      "the DES response is a valid collected Pending transaction" in {
//        stubForGet(
//          getTransactionUrl,
//          OK,
//          """{
//            |  "paymentStatus": "PENDING",
//            |  "paymentDate": "2000-01-01",
//            |  "paymentReference": "002630000994",
//            |  "paymentAmount": 2.00
//            |}""".stripMargin
//        )
//
//        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//
//        response mustBe DesGetTransactionPending(
//          paymentDueDate = LocalDate.parse("2000-01-01"),
//          paymentReference = Some("002630000994"),
//          paymentAmount = Some(2.0)
//        )
//
//        verifyDesGet(getTransactionUrl, withOriginator = true)
//      }
//
//      "the DES response is a valid paid Pending transaction" in {
//        stubForGet(getTransactionUrl, OK, """{ "paymentStatus": "PENDING", "paymentDate": "2000-01-01" }""")
//        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//
//        response mustBe DesGetTransactionPending(
//          paymentDueDate = LocalDate.parse("2000-01-01"),
//          paymentReference = None,
//          paymentAmount = None
//        )
//
//        verifyDesGet(getTransactionUrl, withOriginator = true)
//      }
//
//      "the DES response is a valid Paid transaction" in {
//        stubForGet(
//          getTransactionUrl,
//          OK,
//          """{
//            |  "paymentStatus": "PAID",
//            |  "paymentDate": "2000-01-01",
//            |  "paymentReference": "002630000993",
//            |  "paymentAmount": 1.00
//            |}""".stripMargin
//        )
//
//        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
//
//        response mustBe DesGetTransactionPaid(
//          paymentDate = LocalDate.parse("2000-01-01"),
//          paymentReference = "002630000993",
//          paymentAmount = 1.0
//        )
//
//        verifyDesGet(getTransactionUrl, withOriginator = true)
//      }
//    }
//  }

  "correlationId" must {

    "reuse the requestId given it matches the correlation id pattern, and return a valid UUID" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(requestId = Some(RequestId("abcd1234-ab12-cd34-ef56")))
      val result                     = hipConnector.correlationId

      result must startWith("abcd1234-ab12-cd34-ef56-")
      UUID.fromString(result) // throws if invalid UUID
    }
    "make a new, valid UUID when the requestId does not match the correlation id pattern" in {
      implicit val hc: HeaderCarrier =
        HeaderCarrier(requestId = Some(RequestId("not-a-valid-correlation-id-pattern")))

      val result = hipConnector.correlationId
      UUID.fromString(result)
    }
    "make a new, valid UUID when the requestId is empty" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(requestId = None)

      val result = hipConnector.correlationId
      UUID.fromString(result)
    }

  }

}
