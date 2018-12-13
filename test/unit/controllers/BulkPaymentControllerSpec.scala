/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.controllers

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Play
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeApplication, FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.{AppContext, LisaAuthConnector}
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{BulkPaymentService, CurrentDateService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BulkPaymentControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val currentDate = new DateTime("2020-01-01")
  val validDate = "2018-01-01"
  val invalidDate = "01-01-2018"
  val lmrn = "Z123456"

  before {
    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).
      thenReturn(Future(Some("1234")))

    when(mockCurrentDateService.now()).
      thenReturn(currentDate)
  }

  "Get Bulk Payment" must {

    "return 200 success" when {
      "the service returns a GetBulkPaymentSuccessResponse" in {
        when(mockService.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(successResponse))

        val oneYearInFuture = new DateTime(validDate).plusYears(1).toString("yyyy-MM-dd")
        val result = SUT.getBulkPayment(lmrn, validDate, oneYearInFuture).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe OK

        val json = contentAsJson(result)

        (json \ "lisaManagerReferenceNumber").as[String] mustBe lmrn
        (json \ "payments" \ 0 \ "paymentAmount").as[Amount] mustBe successResponse.payments(0).asInstanceOf[BulkPaymentPaid].paymentAmount
        (json \ "payments" \ 0 \ "paymentDate").as[String] mustBe successResponse.payments(0).asInstanceOf[BulkPaymentPaid].paymentDate.toString("yyyy-MM-dd")
        (json \ "payments" \ 0 \ "paymentReference").as[String] mustBe successResponse.payments(0).asInstanceOf[BulkPaymentPaid].paymentReference
      }
    }

    "return 400 BAD_REQUEST" when {
      "the lisa manager reference number is invalid" in {
        val result = SUT.getBulkPayment("Z1234567", validDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "BAD_REQUEST"
        (json \ "message").as[String] mustBe "lisaManagerReferenceNumber in the URL is in the wrong format"
      }
      "the startDate parameter is in the wrong format" in {
        val result = SUT.getBulkPayment(lmrn, invalidDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStart.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStart.message
      }
      "the endDate parameter is in the wrong format" in {
        val result = SUT.getBulkPayment(lmrn, validDate, invalidDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEnd.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEnd.message
      }
      "the startDate and endDate parameters are invalid" in {
        val result = SUT.getBulkPayment(lmrn, invalidDate, invalidDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStartEnd.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStartEnd.message
      }
    }

    "return 403 FORBIDDEN" when {
      "the endDate parameter is in the future" in {
        val futureDate = currentDate.plusDays(1).toString("yyyy-MM-dd")
        val result = SUT.getBulkPayment(lmrn, validDate, futureDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEndInFuture.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEndInFuture.message
      }
      "the endDate is before the startDate" in {
        val beforeDate = new DateTime(validDate).minusDays(1).toString("yyyy-MM-dd")
        val result = SUT.getBulkPayment(lmrn, validDate, beforeDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEndBeforeStart.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEndBeforeStart.message
      }
      "the startDate is before 6 April 2017" in {
        val result = SUT.getBulkPayment(lmrn, "2017-04-05", validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStartBefore6April2017.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStartBefore6April2017.message
      }
      "there's more than a year between startDate and endDate" in {
        val futureDate = new DateTime(validDate).plusYears(1).plusDays(1).toString("yyyy-MM-dd")
        val result = SUT.getBulkPayment(lmrn, validDate, futureDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe FORBIDDEN

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestOverYearBetweenStartAndEnd.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestOverYearBetweenStartAndEnd.message
      }
    }

    "return 404 TRANSACTION_NOT_FOUND" when {
      "the service returns a GetBulkPaymentNotFoundResponse" in {
        when(mockService.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBulkPaymentNotFoundResponse))

        val result = SUT.getBulkPayment(lmrn, validDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe NOT_FOUND

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "TRANSACTION_NOT_FOUND"
        (json \ "message").as[String] mustBe "No payments or debts exist for this date range"
      }
    }

    "return 500 INTERNAL_SERVER_ERROR" when {
      "the service return a GetBulkPaymentErrorResponse" in {
        when(mockService.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBulkPaymentErrorResponse))

        val result = SUT.getBulkPayment(lmrn, validDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe INTERNAL_SERVER_ERROR

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (json \ "message").as[String] mustBe "Internal server error"
      }
    }

  }

  "transformV1Response" must {

    "remove transactionType and status" in {
      val input = Json.obj(
        "lisaManagerReferenceNumber" -> "Z123456",
        "payments" -> Json.arr(
          Json.obj(
            "transactionId" -> "1",
            "transactionType" -> "Payment",
            "status" -> "Paid"
          ),
          Json.obj(
            "transactionId" -> "2",
            "transactionType" -> "Payment",
            "status" -> "Pending"
          )
        )
      )

      val expected = Json.obj(
        "lisaManagerReferenceNumber" -> "Z123456",
        "payments" -> Json.arr(
          Json.obj(
            "transactionId" -> "1"
          ),
          Json.obj(
            "transactionId" -> "2"
          )
        )
      )

      val result = Future.successful(SUT.transformV1Response(input))

      status(result) mustBe OK
      contentAsJson(result) mustBe expected
    }

    "return an error if the transform fails" in {
      val input = Json.obj(
        "lisaManagerReferenceNumber" -> "Z123456",
        "payments" -> "invalid"
      )

      val expected = Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")

      val result = Future.successful(SUT.transformV1Response(input))

      status(result) mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe expected
    }

  }

  val successResponse: GetBulkPaymentSuccessResponse = GetBulkPaymentSuccessResponse(
    lmrn,
    List(
      BulkPaymentPaid(75.15, new DateTime("2018-01-01"), "123"),
      BulkPaymentPending(100.0, new DateTime("2018-02-02"))
    )
  )

  val mockService: BulkPaymentService = mock[BulkPaymentService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val mockCurrentDateService: CurrentDateService = mock[CurrentDateService]

  val SUT = new BulkPaymentController(mockAuthCon, AppContext, mockCurrentDateService, mockService) {
    override lazy val v2endpointsEnabled = true
  }

}
