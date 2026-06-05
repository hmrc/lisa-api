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

import play.api.http.Status.UNAUTHORIZED
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, RequestId}
import uk.gov.hmrc.lisaapi.models.*
import uk.gov.hmrc.lisaapi.models.hip.*

import java.util.UUID

class HipConnectorSpec extends HipConnectorTestHelper {

  import HipFails.*
  import HipSuccesses.*

  lazy val hipConnector: HipConnector = injector.instanceOf[HipConnector]
  private val baseTransactionUrl      = "/RESTAdapter/lisa/bonus-charge/manager"
  private val jsonContentType         = Map("Content-Type" -> Seq("application/json"))
  private val stringContentType       = Map("Content-Type" -> Seq("application/text"))

  "parseResponse" must {

    "parse a PENDING transaction" in {
      val res    = HttpResponse(OK, validHipPendingJson, jsonContentType)
      val result = hipConnector.parseResponse[HipGetTransactionResponse](res)
      result mustBe expectedHipPending
    }

    "parse a PAID transaction" in {
      val res    = HttpResponse(OK, validHipPaidJson, jsonContentType)
      val result = hipConnector.parseResponse[HipGetTransactionResponse](res)
      result mustBe expectedHipPaid
    }

    "parse returns HodErrorResponse for origin HOD" in {
      val res    = HttpResponse(503, validServiceUnavailableHodJson, jsonContentType)
      val result = hipConnector.parseResponse[HipServiceUnavailable](res)
      result mustBe HipOtherErrorResponse
    }

    "parse returns HipOtherErrorResponse for invalid json" in {
      val res    = HttpResponse(200, validHipOtherErrorjson, jsonContentType)
      val result = hipConnector.parseResponse[HipGetTransactionResponse](res)
      result mustBe HipOtherErrorResponse
    }

    "parse returns HipOtherErrorResponse for non json" in {
      val res    = HttpResponse(BAD_REQUEST, "I am not json", stringContentType)
      val result = hipConnector.parseResponse[HipGetTransactionResponse](res)
      result mustBe HipOtherErrorResponse
    }

    "parse HipServiceUnavailable" in {
      val res    = HttpResponse(SERVICE_UNAVAILABLE, validServiceUnavailableJson, jsonContentType)
      val result = hipConnector.parseResponse[HipServiceUnavailable](res)
      result mustBe expectedServiceUnavailable
    }

    "parse HipServerError" in {
      val res    = HttpResponse(INTERNAL_SERVER_ERROR, validServerErrorJson, jsonContentType)
      val result = hipConnector.parseResponse[HipServerError](res)
      result mustBe expectedServerError
    }

    "parse HipValidationError" in {
      val res    = HttpResponse(UNPROCESSABLE_ENTITY, validValidationErrorJson, jsonContentType)
      val result = hipConnector.parseResponse[HipValidationError](res)
      result mustBe expectedValidationError
    }

    "parse HipBadRequest" in {
      val res    = HttpResponse(BAD_REQUEST, validHipBadRequestJson, jsonContentType)
      val result = hipConnector.parseResponse[HipBadRequest](res)
      result mustBe expectedBadRequestError
    }
  }

  "getTransaction" must {
    "return HipGetTransactionPending" in {

      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"
      stubForGet(
        transactionUrl,
        OK,
        validHipPendingJson
      )

      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe expectedHipPending
      verifyHipGet(transactionUrl)
    }

    "return HipGetTransactionPaid" in {

      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"
      stubForGet(
        transactionUrl,
        OK,
        validHipPaidJson
      )

      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe expectedHipPaid
      verifyHipGet(transactionUrl)
    }

    "return BAD_REQUEST" in {

      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"
      stubForGet(
        transactionUrl,
        BAD_REQUEST,
        validHipBadRequestJson
      )

      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe expectedBadRequestError
      verifyHipGet(transactionUrl)
    }

    "return HipServiceUnavailable" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, SERVICE_UNAVAILABLE, validServiceUnavailableJson)
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response.asInstanceOf[HipServiceUnavailable] mustBe expectedServiceUnavailable
      verifyHipGet(transactionUrl)
    }

    "return INTERNAL_SERVER_ERROR" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, INTERNAL_SERVER_ERROR, validServerErrorJson)
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response.asInstanceOf[HipServerError] mustBe expectedServerError
      verifyHipGet(transactionUrl)
    }

    "return UNPROCESSABLE_ENTITY" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, UNPROCESSABLE_ENTITY, validValidationErrorJson)
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response.asInstanceOf[HipValidationError] mustBe expectedValidationError
      verifyHipGet(transactionUrl)
    }

    "return NOT_FOUND" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, NOT_FOUND, "")
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe HipNotFound
      verifyHipGet(transactionUrl)
    }

    "return UNAUTHORIZED" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, UNAUTHORIZED, "")
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe HipUnauthorized
      verifyHipGet(transactionUrl)
    }

    "return FORBIDDEN" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, FORBIDDEN, "")
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe HipForbidden
      verifyHipGet(transactionUrl)
    }

    "return HipOtherErrorResponse" in {
      val transactionUrl = s"$baseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

      stubForGet(transactionUrl, FAILED_DEPENDENCY, "")
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))
      response mustBe HipOtherErrorResponse
      verifyHipGet(transactionUrl)
    }

  }

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
