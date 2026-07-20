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

package uk.gov.hmrc.lisaapi.models.hip

import org.scalatestplus.play.PlaySpec
import play.api.libs.json._
import uk.gov.hmrc.lisaapi.models.hip.HipFailure._

import java.time.LocalDate

class HipResponseSpec extends PlaySpec {

  val pendingJson: String =
    """{"success": {"paymentStatus": "PENDING", "paymentDueDate": "2025-06-01", "paymentAmount": 123.45, "paymentReference": "ref123"}}"""

  val paidJson: String =
    """{"success": {"paymentStatus": "PAID", "paymentDate": "2025-05-20", "paymentDueDate": "2025-06-01", "paymentReference": "ref123", "paymentAmount": 123.45}}"""

  val failuresJson: String =
    """{"response": {"failures": [{"type": "SOME_TYPE", "reason": "some reason"}]}}"""

  val expectedFailures: HipFailures = HipFailures(Seq(HipError("SOME_TYPE", "some reason")))

  "HipGetTransactionResponse reads" must {

    "deserialize a PENDING response with all fields" in {
      val res = Json.parse(pendingJson).validate[HipGetTransactionResponse]

      res                   mustBe JsSuccess(
        HipGetTransactionPending(
          paymentDueDate = LocalDate.parse("2025-06-01"),
          paymentAmount = Some(BigDecimal("123.45")),
          paymentReference = Some("ref123")
        )
      )
      res.get.paymentStatus mustBe "PENDING"
    }

    "deserialize a PENDING response without the optional fields" in {
      val json = """{"success": {"paymentStatus": "PENDING", "paymentDueDate": "2025-06-01"}}"""
      val res  = Json.parse(json).validate[HipGetTransactionResponse]

      res mustBe JsSuccess(
        HipGetTransactionPending(
          paymentDueDate = LocalDate.parse("2025-06-01"),
          paymentAmount = None,
          paymentReference = None
        )
      )
    }

    "deserialize a PAID response" in {
      val res = Json.parse(paidJson).validate[HipGetTransactionResponse]

      res                   mustBe JsSuccess(
        HipGetTransactionPaid(
          paymentDate = LocalDate.parse("2025-05-20"),
          paymentDueDate = LocalDate.parse("2025-06-01"),
          paymentReference = "ref123",
          paymentAmount = BigDecimal("123.45")
        )
      )
      res.get.paymentStatus mustBe "PAID"
    }

    "return an error for an unknown payment status" in {
      val json = """{"success": {"paymentStatus": "CANCELLED"}}"""

      Json.parse(json).validate[HipGetTransactionResponse] mustBe JsError("Unknown payment status: CANCELLED")
    }

    "return an error when the payment status is missing" in {
      Json.parse("""{"foo": "bar"}""").validate[HipGetTransactionResponse] match {
        case JsError(errors) => errors.toString must include("Unknown type")
        case JsSuccess(_, _) => fail("expected a JsError")
      }
    }

  }

  "HipFailure reads" must {

    "deserialize a HipError" in {
      Json.parse("""{"type": "SOME_TYPE", "reason": "some reason"}""").validate[HipError] mustBe
        JsSuccess(HipError("SOME_TYPE", "some reason"))
    }

    "deserialize a HipFailures" in {
      Json.parse("""{"failures": [{"type": "SOME_TYPE", "reason": "some reason"}]}""").validate[HipFailures] mustBe
        JsSuccess(expectedFailures)
    }

    "deserialize a HipServiceUnavailable" in {
      Json.parse(failuresJson).validate[HipServiceUnavailable] mustBe
        JsSuccess(HipServiceUnavailable(expectedFailures))
    }

    "deserialize a HipServerError" in {
      Json.parse(failuresJson).validate[HipServerError] mustBe JsSuccess(HipServerError(expectedFailures))
    }

    "deserialize a HipBadRequest" in {
      Json.parse(failuresJson).validate[HipBadRequest] mustBe JsSuccess(HipBadRequest(expectedFailures))
    }

    "deserialize a Hip422Error" in {
      Json
        .parse("""{"processingDate": "2025-06-01", "code": "001", "text": "some error"}""")
        .validate[Hip422Error] mustBe
        JsSuccess(Hip422Error("2025-06-01", "001", "some error"))
    }

    "deserialize a HipValidationError" in {
      val json = """{"errors": {"processingDate": "2025-06-01", "code": "001", "text": "some error"}}"""

      Json.parse(json).validate[HipValidationError] mustBe
        JsSuccess(HipValidationError(Hip422Error("2025-06-01", "001", "some error")))
    }

    "deserialize a HodErrorBody" in {
      Json.parse("""{"code": "001", "message": "some error", "logId": "log-1"}""").validate[HodErrorBody] mustBe
        JsSuccess(HodErrorBody("001", "some error", "log-1"))
    }

    "deserialize a HodError" in {
      Json.parse("""{"error": {"code": "001", "message": "some error", "logId": "log-1"}}""").validate[HodError] mustBe
        JsSuccess(HodError(HodErrorBody("001", "some error", "log-1")))
    }

    "deserialize a HodErrorResponse" in {
      val json = """{"response": {"error": {"code": "001", "message": "some error", "logId": "log-1"}}}"""

      Json.parse(json).validate[HodErrorResponse] mustBe
        JsSuccess(HodErrorResponse(HodError(HodErrorBody("001", "some error", "log-1"))))
    }

  }

  "HipFailure types" must {

    "expose the type and reason of a HipFailureResponse" in {
      val failure = HipFailureResponse(`type` = "SOME_TYPE", reason = "some reason")

      failure.`type` mustBe "SOME_TYPE"
      failure.reason mustBe "some reason"
    }

    "all be instances of HipFailure" in {
      val failures: Seq[HipFailure] =
        Seq(HipNotFound, HipUnauthorized, HipForbidden, HipOtherErrorResponse, HipOriginUnknown)

      failures.size mustBe 5
    }

  }

}
