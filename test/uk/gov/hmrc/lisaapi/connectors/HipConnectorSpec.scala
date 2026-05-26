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
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}
import uk.gov.hmrc.lisaapi.models.*
import uk.gov.hmrc.lisaapi.models.des.*

import java.time.LocalDate
import java.util.UUID

class HipConnectorSpec extends DesConnectorTestHelper {

  lazy val hipConnector: HipConnector = injector.instanceOf[HipConnector] // lazy to allow wiremock to start

  val managerPath = "/lifetime-isa/manager"

  private val getTransactionUrl =
    s"$managerPath/Z123456/accounts/ABC12345/transaction/123456/bonusChargeDetails"

  private val bulkPaymentUrl =
    "/enterprise/financial-data/ZISA/Z123456/LISA?dateFrom=2018-01-01&dateTo=2018-01-01&onlyOpenItems=false"
  
  "Retrieve Transaction endpoint" must {

    "return a unavailable response when a 503 is returned" in {
      stubForGet(getTransactionUrl, SERVICE_UNAVAILABLE, "")
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

      response mustBe DesUnavailableResponse
      verifyDesGet(getTransactionUrl, withOriginator = true)
    }

    "return a failure response" when {

      "the HIP response is a failure response" in {
        stubForGet(getTransactionUrl, OK, """{ "code": "ERROR_CODE", "reason" : "ERROR MESSAGE" }""")
        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

        response mustBe HipFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        verifyDesGet(getTransactionUrl, withOriginator = true)
      }

      "the DES response has no json body" in {
        stubForGet(getTransactionUrl, OK, "")
        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

        response mustBe DesFailureResponse()
        verifyDesGet(getTransactionUrl, withOriginator = true)
      }

      "the DES response is invalid" in {
        stubForGet(getTransactionUrl, OK, """{ "status": "Due" }""")
        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

        response mustBe DesFailureResponse()
        verifyDesGet(getTransactionUrl, withOriginator = true)
      }
    }

    "return a success response" when {

      "the DES response is a valid collected Pending transaction" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{
            |  "paymentStatus": "PENDING",
            |  "paymentDate": "2000-01-01",
            |  "paymentReference": "002630000994",
            |  "paymentAmount": 2.00
            |}""".stripMargin
        )

        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

        response mustBe DesGetTransactionPending(
          paymentDueDate = LocalDate.parse("2000-01-01"),
          paymentReference = Some("002630000994"),
          paymentAmount = Some(2.0)
        )

        verifyDesGet(getTransactionUrl, withOriginator = true)
      }

      "the DES response is a valid paid Pending transaction" in {
        stubForGet(getTransactionUrl, OK, """{ "paymentStatus": "PENDING", "paymentDate": "2000-01-01" }""")
        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

        response mustBe DesGetTransactionPending(
          paymentDueDate = LocalDate.parse("2000-01-01"),
          paymentReference = None,
          paymentAmount = None
        )

        verifyDesGet(getTransactionUrl, withOriginator = true)
      }

      "the DES response is a valid Paid transaction" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{
            |  "paymentStatus": "PAID",
            |  "paymentDate": "2000-01-01",
            |  "paymentReference": "002630000993",
            |  "paymentAmount": 1.00
            |}""".stripMargin
        )

        val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

        response mustBe DesGetTransactionPaid(
          paymentDate = LocalDate.parse("2000-01-01"),
          paymentReference = "002630000993",
          paymentAmount = 1.0
        )

        verifyDesGet(getTransactionUrl, withOriginator = true)
      }
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
