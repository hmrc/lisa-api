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
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{BulkPaymentController, ErrorBadRequestEnd, ErrorBadRequestEndInFuture, ErrorBadRequestStart, ErrorBadRequestStartEnd}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.BulkPaymentService

import scala.concurrent.Future

class BulkPaymentControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val validDate = "2018-01-01"
  val invalidDate = "01-01-2018"
  val lmrn = "Z123456"

  "Get Bulk Payment" must {

    "return 200 success" when {
      "the service returns a GetBulkPaymentSuccessResponse" in {
        when(mockService.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(successResponse))

        val result = SUT.getBulkPayment(lmrn, validDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe OK

        val json = contentAsJson(result)

        (json \ "lisaManagerReferenceNumber").as[String] mustBe lmrn
        (json \ "payments" \ 0 \ "paymentAmount").as[Amount] mustBe successResponse.payments(0).paymentAmount
        (json \ "payments" \ 0 \ "paymentDate").as[String] mustBe successResponse.payments(0).paymentDate.toString("yyyy-MM-dd")
        (json \ "payments" \ 0 \ "paymentReference").as[String] mustBe successResponse.payments(0).paymentReference
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
      "the startDate parameter is invalid" in {
        val result = SUT.getBulkPayment(lmrn, invalidDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestStart.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestStart.message
      }
      "the endDate parameter is invalid" in {
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
      // TODO: add more error validation. error when:
      "the endDate parameter is in the future" in {
        val futureDate = DateTime.now().plusDays(1).toString("yyyy-MM-dd")
        val result = SUT.getBulkPayment(lmrn, validDate, futureDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe BAD_REQUEST

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe ErrorBadRequestEndInFuture.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestEndInFuture.message
      }
      // * end date is in the future
      // * end date is before start date
      // * start date is before 6 april 2017
      // * there's more than a year between start date and end date
    }

    "return 404 PAYMENT_NOT_FOUND" when {
      "the service returns a GetBulkPaymentNotFoundResponse" in {
        when(mockService.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBulkPaymentNotFoundResponse))

        val result = SUT.getBulkPayment(lmrn, validDate, validDate).
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe NOT_FOUND

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "PAYMENT_NOT_FOUND"
        (json \ "message").as[String] mustBe "No bonus payments have been made for this date range"
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

  val successResponse: GetBulkPaymentSuccessResponse = GetBulkPaymentSuccessResponse(
    lmrn,
    List(BulkPayment(new DateTime("2018-01-01"), "123", 75.15))
  )
  val mockService: BulkPaymentService = mock[BulkPaymentService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new BulkPaymentController {
    override val service: BulkPaymentService = mockService
    override val authConnector = mockAuthCon
  }

}
