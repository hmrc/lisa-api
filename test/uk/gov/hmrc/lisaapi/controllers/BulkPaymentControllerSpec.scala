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

package uk.gov.hmrc.lisaapi.controllers

import org.mockito.ArgumentMatchers.{any, eq => matchersEquals}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.lisaapi.helpers.ControllerTestFixture
import uk.gov.hmrc.lisaapi.models._

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkPaymentControllerSpec extends ControllerTestFixture {

  val mockBulkPaymentController: BulkPaymentController = new BulkPaymentController(
    mockAuthConnector,
    mockAppContext,
    mockDateTimeService,
    mockBulkPaymentService,
    mockAuditService,
    mockLisaMetrics,
    mockControllerComponents,
    mockParser
  ) {
    override lazy val v2endpointsEnabled = true
  }

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val currentDate: LocalDate = LocalDate.parse("2020-01-01")
  val validDate: String = "2018-01-01"
  val invalidDate: String = "01-01-2018"
  val lmrn: String = "Z123456"

  val successResponse: GetBulkPaymentSuccessResponse = GetBulkPaymentSuccessResponse(
    lmrn,
    List(
      BulkPaymentPaid(75.15, Some(LocalDate.parse("2018-01-01")), Some("123")),
      BulkPaymentPending(100.0, Some(LocalDate.parse("2018-02-02")))
    )
  )

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    reset(mockAuthConnector)

    mockAuthorize(lmrn)

    when(mockDateTimeService.now())
      .thenReturn(currentDate)
  }

  "Get Bulk Payment" must {

    "return 200 success" when {
      "the service returns a GetBulkPaymentSuccessResponse" in {
        when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(successResponse))

        val oneYearInFuture = LocalDate.parse(validDate).plusYears(1).toString
        val result          = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, oneYearInFuture)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe OK

        val json = contentAsJson(result)

        (json \ "lisaManagerReferenceNumber").as[String] mustBe lmrn
        (json \ "payments" \ 0 \ "paymentAmount")
          .as[Amount] mustBe successResponse.payments.head.asInstanceOf[BulkPaymentPaid].paymentAmount
        (json \ "payments" \ 0 \ "paymentDate").as[String] mustBe successResponse
          .payments.head
          .asInstanceOf[BulkPaymentPaid]
          .paymentDate
          .get
          .toString
        (json \ "payments" \ 0 \ "paymentReference")
          .as[String] mustBe successResponse.payments.head.asInstanceOf[BulkPaymentPaid].paymentReference.get
      }
    }

    "return 400 BAD_REQUEST" when {
      "the lisa manager reference number is invalid" in {
        val result = mockBulkPaymentController
          .getBulkPayment("Z1234567", validDate, validDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
      }
      "the startDate parameter is in the wrong format" in {
        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, invalidDate, validDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStart.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStart.message
      }
      "the endDate parameter is in the wrong format" in {
        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, invalidDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEnd.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEnd.message
      }
      "the startDate and endDate parameters are invalid" in {
        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, invalidDate, invalidDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStartEnd.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStartEnd.message
      }
    }

    "return 403 FORBIDDEN" when {
      "the endDate parameter is in the future" in {
        val futureDate = currentDate.plusDays(1).toString
        val result     = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, futureDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEndInFuture.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEndInFuture.message
      }
      "the endDate is before the startDate" in {
        val beforeDate = LocalDate.parse(validDate).minusDays(1).toString
        val result     = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, beforeDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEndBeforeStart.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEndBeforeStart.message
      }
      "the startDate is before 6 April 2017" in {
        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, "2017-04-05", validDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStartBefore6April2017.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStartBefore6April2017.message
      }
      "there's more than a year between startDate and endDate" in {
        val futureDate = LocalDate.parse(validDate).plusYears(1).plusDays(1).toString
        val result     = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, futureDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestOverYearBetweenStartAndEnd.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestOverYearBetweenStartAndEnd.message
      }
    }

    "return 404 TRANSACTION_NOT_FOUND" when {
      "the service returns a GetBulkPaymentNotFoundResponse" in {
        when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(GetBulkPaymentNotFoundResponse))

        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, validDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe NOT_FOUND

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "TRANSACTION_NOT_FOUND"
        (json \ "message").as[String] mustBe "No payments or debts exist for this date range"
      }
    }

    "return 500 INTERNAL_SERVER_ERROR" when {
      "the service return a GetBulkPaymentErrorResponse" in {
        when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(GetBulkPaymentErrorResponse))

        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, validDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe INTERNAL_SERVER_ERROR

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (json \ "message").as[String] mustBe "Internal server error"
      }
    }

    "return 503 SERVER_ERROR" when {
      "the service return a GetBulkPaymentServiceUnavailableResponse" in {
        when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
          .thenReturn(Future.successful(GetBulkPaymentServiceUnavailableResponse))

        val result = mockBulkPaymentController
          .getBulkPayment(lmrn, validDate, validDate)
          .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe SERVICE_UNAVAILABLE

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "SERVER_ERROR"
        (json \ "message").as[String] mustBe "Service unavailable"
      }
    }

  }

  "transformV1Response" must {

    "remove transactionType and status" in {
      val input = Json.obj(
        "lisaManagerReferenceNumber" -> "Z123456",
        "payments"                   -> Json.arr(
          Json.obj(
            "transactionId"   -> "1",
            "transactionType" -> "Payment",
            "status"          -> "Paid"
          ),
          Json.obj(
            "transactionId"   -> "2",
            "transactionType" -> "Payment",
            "status"          -> "Pending"
          )
        )
      )

      val expected = Json.obj(
        "lisaManagerReferenceNumber" -> "Z123456",
        "payments"                   -> Json.arr(
          Json.obj(
            "transactionId" -> "1"
          ),
          Json.obj(
            "transactionId" -> "2"
          )
        )
      )

      val result = Future.successful(mockBulkPaymentController.transformV1Response(input))

      status(result) mustBe OK
      contentAsJson(result) mustBe expected
    }

    "return an error if the transform fails" in {
      val input = Json.obj(
        "lisaManagerReferenceNumber" -> "Z123456",
        "payments"                   -> "invalid"
      )

      val expected = Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")

      val result = Future.successful(mockBulkPaymentController.transformV1Response(input))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expected
    }

  }

  "audit getBulkPaymentReported" when {
    "the service returns a GetBulkPaymentSuccessResponse" in {
      when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
        .thenReturn(Future.successful(successResponse))

      val oneYearInFuture = LocalDate.parse(validDate).plusYears(1).toString
      val result          = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, oneYearInFuture)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn
          )
        )
      )(any())
    }
  }

  "audit getBulkPaymentNotReported" when {
    "the startDate parameter is in the wrong format" in {
      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, invalidDate, validDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestStart.errorCode
          )
        )
      )(any())
    }
    "the endDate parameter is in the wrong format" in {
      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, invalidDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestEnd.errorCode
          )
        )
      )(any())
    }
    "the startDate and endDate parameters are invalid" in {
      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, invalidDate, invalidDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestStartEnd.errorCode
          )
        )
      )(any())
    }
    "the endDate parameter is in the future" in {
      val futureDate = currentDate.plusDays(1).toString
      val result     = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, futureDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestEndInFuture.errorCode
          )
        )
      )(any())
    }
    "the endDate is before the startDate" in {
      val beforeDate = LocalDate.parse(validDate).minusDays(1).toString
      val result     = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, beforeDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestEndBeforeStart.errorCode
          )
        )
      )(any())
    }
    "the startDate is before 6 April 2017" in {
      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, "2017-04-05", validDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestStartBefore6April2017.errorCode
          )
        )
      )(any())
    }
    "there's more than a year between startDate and endDate" in {
      val futureDate = LocalDate.parse(validDate).plusYears(1).plusDays(1).toString
      val result     = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, futureDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> ErrorBadRequestOverYearBetweenStartAndEnd.errorCode
          )
        )
      )(any())
    }
    "the service returns a GetBulkPaymentNotFoundResponse" in {
      when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
        .thenReturn(Future.successful(GetBulkPaymentNotFoundResponse))

      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, validDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> "TRANSACTION_NOT_FOUND"
          )
        )
      )(any())
    }
    "the service return a GetBulkPaymentErrorResponse" in {
      when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
        .thenReturn(Future.successful(GetBulkPaymentErrorResponse))

      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, validDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> "INTERNAL_SERVER_ERROR"
          )
        )
      )(any())
    }
    "the service return a GetBulkPaymentServiceUnavailableResponse" in {
      when(mockBulkPaymentService.getBulkPayment(any(), any(), any())(any()))
        .thenReturn(Future.successful(GetBulkPaymentServiceUnavailableResponse))

      val result = mockBulkPaymentController
        .getBulkPayment(lmrn, validDate, validDate)
        .apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
      await(result)

      verify(mockAuditService).audit(
        auditType = matchersEquals("getBulkPaymentNotReported"),
        path = matchersEquals(s"/manager/$lmrn/payments"),
        auditData = matchersEquals(
          Map(
            "lisaManagerReferenceNumber" -> lmrn,
            "reasonNotReported"          -> "SERVER_ERROR"
          )
        )
      )(any())
    }
  }

}
