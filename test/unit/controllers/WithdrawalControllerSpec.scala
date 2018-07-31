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
import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountAlreadyClosed, ErrorAccountAlreadyVoided, ErrorAccountNotFound, ErrorInternalServerError, ErrorWithdrawalExists, ErrorWithdrawalTimescalesExceeded, WithdrawalController}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, CurrentDateService, WithdrawalService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class WithdrawalControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach
  with LisaConstants {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val transactionId = "1234567890"
  val validWithdrawalJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.withdrawal-charge.json")).mkString
  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockDateTimeService)

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockDateTimeService.now()).thenReturn(new DateTime("2018-01-01"))
  }

  "the POST bonus payment endpoint" must {

    "return with status 201 created" when {

      "given a ReportWithdrawalChargeOnTimeResponse from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeOnTimeResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created"
        }
      }

      "given a ReportWithdrawalChargeLateResponse from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeLateResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created - late notification"
        }
      }

      "given a ReportWithdrawalChargeSupersededResponse from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersededResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction superseded"
        }
      }

    }

    "return with status 403 forbidden" when {

      "given a ReportWithdrawalChargeAccountClosed from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountClosed))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyClosed.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyClosed.message
        }
      }

      "given a ReportWithdrawalChargeAccountVoid from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountVoid))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyVoided.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyVoided.message
        }
      }

      "given a ReportWithdrawalChargeAccountCancelled from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyClosed.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyClosed.message
        }
      }

      "given a ReportWithdrawalChargeReportingError from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeReportingError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_REPORTING_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The withdrawal charge does not equal 25% of the withdrawal amount"
        }
      }

      "given a ReportWithdrawalChargeAlreadySuperseded from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadySuperseded))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED"
          (contentAsJson(res) \ "message").as[String] mustBe "This withdrawal charge has already been superseded"
        }
      }

      "given a ReportWithdrawalChargeSupersedeAmountMismatch from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeAmountMismatch))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH"
          (contentAsJson(res) \ "message").as[String] mustBe "originalTransactionId and the originalWithdrawalChargeAmount do not match the information in the original request"
        }
      }

      "given a ReportWithdrawalChargeSupersedeOutcomeError from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeOutcomeError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_OUTCOME_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The calculation from your superseded withdrawal charge is incorrect"
        }
      }

      "the claimPeriodEndDate is more than 6 years and 14 days in the past" in {
        val now = new DateTime("2050-01-20")

        when(mockDateTimeService.now()).thenReturn(now)

        val testEndDate = now.minusYears(6).withDayOfMonth(5)
        val testStartDate = testEndDate.minusMonths(1).plusDays(1)

        val validWithdrawalCharge = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]
        val request = validWithdrawalCharge.copy(claimPeriodStartDate = testStartDate, claimPeriodEndDate = testEndDate)
        val requestJson = Json.toJson(request).toString()

        doRequest(requestJson) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorWithdrawalTimescalesExceeded.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalTimescalesExceeded.message
        }
      }

    }

    "return with status 404 not found" when {

      "given a ReportWithdrawalChargeAccountNotFound from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountNotFound))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe NOT_FOUND
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountNotFound.message
        }
      }

    }

    "return with status 409 conflict" when {

      "given a ReportWithdrawalChargeAlreadyExists from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadyExists))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CONFLICT
          (contentAsJson(res) \ "code").as[String] mustBe ErrorWithdrawalExists.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorWithdrawalExists.message
        }
      }

    }

    "return with status 500 internal server error" when {

      "an exception gets thrown" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validWithdrawalJson) { res =>
          reset(mockService) // removes the thenThrow

          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

      "given a RequestWithdrawalChargeError from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

    }

  }

  def doRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.reportWithdrawalCharge(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService: WithdrawalService = mock[WithdrawalService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val mockDateTimeService: CurrentDateService = mock[CurrentDateService]

  val SUT = new WithdrawalController {
    override val service: WithdrawalService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector: LisaAuthConnector = mockAuthCon
    override val dateTimeService: CurrentDateService = mockDateTimeService
  }

}
