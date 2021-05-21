/*
 * Copyright 2021 HM Revenue & Customs
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

import helpers.ControllerTestFixture
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, eq => MatcherEquals}
import org.mockito.Mockito.{reset, verify, when}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountAlreadyCancelled, ErrorAccountAlreadyVoided, ErrorAccountNotFound, ErrorInternalServerError, ErrorServiceUnavailable, ErrorValidation, ErrorWithdrawalNotFound, ErrorWithdrawalTimescalesExceeded, WithdrawalController}
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class WithdrawalControllerSpec extends ControllerTestFixture {

  val withdrawalController: WithdrawalController = new WithdrawalController(mockAuthConnector, mockAppContext, mockWithdrawalService, mockBonusOrWithdrawalService, mockAuditService, mockWithdrawalChargeValidator,
    mockDateTimeService, mockLisaMetrics, mockControllerComponents, mockParser) {
    override lazy val v2endpointsEnabled = true
  }

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val transactionId = "1234567890"
  val validWithdrawalJson: String = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.withdrawal-charge.json")).mkString

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockDateTimeService)
    reset(mockWithdrawalChargeValidator)

    when(mockAuthConnector.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockDateTimeService.now()).thenReturn(new DateTime("2018-01-01"))
    when(mockWithdrawalChargeValidator.validate(any())).thenReturn(Nil)
  }

  "the POST withdrawal charge endpoint" must {

    "return with status 201 created" when {

      "given a ReportWithdrawalChargeOnTimeResponse from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeOnTimeResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created"
        }
      }

      "given a ReportWithdrawalChargeLateResponse from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeLateResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created - late notification"
        }
      }

      "given a ReportWithdrawalChargeSupersededResponse from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
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
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyCancelled.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyCancelled.message
        }
      }

      "given a ReportWithdrawalChargeAccountVoid from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountVoid))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyVoided.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyVoided.message
        }
      }

      "given a ReportWithdrawalChargeAccountCancelled from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyCancelled.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyCancelled.message
        }
      }

      "given a ReportWithdrawalChargeReportingError from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeReportingError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_REPORTING_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The withdrawal charge as a percentage of the withdrawal amount is incorrect. " +
            "For withdrawals made between 06/03/2020 and 05/04/2021 the withdrawal charge is 20%. For all other withdrawals it is 25%."
        }
      }

      "given a ReportWithdrawalChargeAlreadySuperseded from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadySuperseded(transactionId)))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED"
          (contentAsJson(res) \ "message").as[String] mustBe "This withdrawal charge has already been superseded"
          (contentAsJson(res) \ "transactionId").as[String] mustBe transactionId
        }
      }

      "given a ReportWithdrawalChargeSupersedeAmountMismatch from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeAmountMismatch))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH"
          (contentAsJson(res) \ "message").as[String] mustBe "originalTransactionId and the originalWithdrawalChargeAmount do not match the information in the original request"
        }
      }

      "given a ReportWithdrawalChargeSupersedeOutcomeError from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
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
        val requestJson = Json.toJson(request)(ReportWithdrawalChargeRequest.supersededWithdrawalWrites).toString()

        doRequest(requestJson) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorWithdrawalTimescalesExceeded.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalTimescalesExceeded.message
        }
      }

      "the json request fails business validation" in {
        val errors = List(
          ErrorValidation(
            DATE_ERROR,
            "The claimPeriodStartDate must be the 6th day of the month",
            Some("/claimPeriodStartDate")
          ),
          ErrorValidation(
            DATE_ERROR,
            "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            Some("/claimPeriodEndDate")
          )
        )

        when(mockWithdrawalChargeValidator.validate(any())).thenReturn(errors)

        val validWithdrawalCharge = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]
        val requestJson = Json.toJson(validWithdrawalCharge)(ReportWithdrawalChargeRequest.supersededWithdrawalWrites).toString()

        doRequest(requestJson) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"

          (json \ "errors" \ 0 \ "path").as[String] mustBe "/claimPeriodStartDate"
          (json \ "errors" \ 1 \ "path").as[String] mustBe "/claimPeriodEndDate"
        }
      }

    }

    "return with status 404 not found" when {

      "given a ReportWithdrawalChargeAccountNotFound from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountNotFound))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe NOT_FOUND
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountNotFound.message
        }
      }

    }

    "return with status 406 not acceptable" when {

      "attempting to use the v1 of the api" in {
        doRequest(validWithdrawalJson, header = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")) { res =>
          status(res) mustBe NOT_ACCEPTABLE
          (contentAsJson(res) \ "code").as[String] mustBe "ACCEPT_HEADER_INVALID"
          (contentAsJson(res) \ "message").as[String] mustBe "The accept header has an invalid version for this endpoint"
        }
      }

    }

    "return with status 409 conflict" when {

      "given a ReportWithdrawalChargeAlreadyExists from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadyExists(transactionId)))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CONFLICT
          (contentAsJson(res) \ "code").as[String] mustBe "WITHDRAWAL_CHARGE_ALREADY_EXISTS"
          (contentAsJson(res) \ "message").as[String] mustBe "A withdrawal charge with these details has already been requested for this investor"
          (contentAsJson(res) \ "transactionId").as[String] mustBe transactionId
        }
      }

    }

    "return with status 500 internal server error" when {

      "an exception gets thrown" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validWithdrawalJson) { res =>
          reset(mockWithdrawalService) // removes the thenThrow

          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

      "given a RequestWithdrawalChargeError from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

    }

    "return with status 503 service unavailable" when {

      "given a ReportWithdrawalChargeServiceUnavailable from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeServiceUnavailable))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe SERVICE_UNAVAILABLE
          contentAsJson(res) mustBe ErrorServiceUnavailable.asJson
        }
      }

    }

    "audit withdrawalChargeRequested" when {
      "given a ReportWithdrawalChargeOnTimeResponse from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeOnTimeResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "lateNotification" -> "no",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal"
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeLateResponse from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeLateResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "lateNotification" -> "yes",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal"
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeSupersededResponse from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersededResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal"
            ))
          )(any())
        }
      }
    }

    "audit withdrawalChargeNotRequested" when {
      "given a ReportWithdrawalChargeAccountClosed from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> ErrorAccountAlreadyCancelled.errorCode
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeAccountVoid from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountVoid))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> ErrorAccountAlreadyVoided.errorCode
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeAccountCancelled from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountCancelled))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> ErrorAccountAlreadyCancelled.errorCode
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeReportingError from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeReportingError))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "WITHDRAWAL_REPORTING_ERROR"
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeAlreadySuperseded from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadySuperseded(transactionId)))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED"
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeSupersedeAmountMismatch from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeAmountMismatch))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH"
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeSupersedeOutcomeError from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersedeOutcomeError))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "SUPERSEDED_WITHDRAWAL_CHARGE_OUTCOME_ERROR"
            ))
          )(any())
        }
      }
      "the claimPeriodEndDate is more than 6 years and 14 days in the past" in {
        val now = new DateTime("2050-01-20")

        when(mockDateTimeService.now()).thenReturn(now)

        val testEndDate = now.minusYears(6).withDayOfMonth(5)
        val testStartDate = testEndDate.minusMonths(1).plusDays(1)

        val validWithdrawalCharge = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]
        val request = validWithdrawalCharge.copy(claimPeriodStartDate = testStartDate, claimPeriodEndDate = testEndDate)
        val requestJson = Json.toJson(request)(ReportWithdrawalChargeRequest.supersededWithdrawalWrites).toString()

        doRequest(requestJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250",
              "claimPeriodStartDate" -> "2043-12-06",
              "claimPeriodEndDate" -> "2044-01-05",
              "withdrawalAmount" -> "2000",
              "originalWithdrawalChargeAmount" -> "250",
              "withdrawalChargeAmount" -> "500",
              "withdrawalChargeAmountYTD" -> "750",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> ErrorWithdrawalTimescalesExceeded.errorCode
            ))
          )(any())
        }
      }
      "the json request fails business validation" in {
        val errors = List(
          ErrorValidation(
            DATE_ERROR,
            "The claimPeriodStartDate must be the 6th day of the month",
            Some("/claimPeriodStartDate")
          ),
          ErrorValidation(
            DATE_ERROR,
            "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            Some("/claimPeriodEndDate")
          )
        )

        when(mockWithdrawalChargeValidator.validate(any())).thenReturn(errors)

        val validWithdrawalCharge = Json.parse(validWithdrawalJson).as[SupersededWithdrawalChargeRequest]
        val requestJson = Json.toJson(validWithdrawalCharge)(ReportWithdrawalChargeRequest.supersededWithdrawalWrites).toString()

        doRequest(requestJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000",
              "originalWithdrawalChargeAmount" -> "250",
              "withdrawalChargeAmount" -> "500",
              "withdrawalChargeAmountYTD" -> "750",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeAccountNotFound from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountNotFound))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> ErrorAccountNotFound.errorCode
            ))
          )(any())
        }
      }
      "given a ReportWithdrawalChargeAlreadyExists from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAlreadyExists(transactionId)))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "WITHDRAWAL_CHARGE_ALREADY_EXISTS"
            ))
          )(any())
        }
      }
      "given a RequestWithdrawalChargeError from the service layer" in {
        when(mockWithdrawalService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeError))

        doRequest(validWithdrawalJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("withdrawalChargeNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges"),
            auditData = MatcherEquals(Map(
              "originalTransactionId" -> "2345678901",
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionResult" -> "250.00",
              "claimPeriodStartDate" -> "2017-12-06",
              "claimPeriodEndDate" -> "2018-01-05",
              "withdrawalAmount" -> "2000.00",
              "originalWithdrawalChargeAmount" -> "250.00",
              "withdrawalChargeAmount" -> "500.00",
              "withdrawalChargeAmountYTD" -> "750.00",
              "fundsDeductedDuringWithdrawal" -> "true",
              "automaticRecoveryAmount" -> "500.00",
              "withdrawalReason" -> "Superseded withdrawal",
              "reason" -> "Additional withdrawal",
              "reasonNotRequested" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
    }

  }

  "the GET withdrawal charge endpoint" must {

    "return 200 success response" in {
      when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetWithdrawalResponse(
        periodStartDate = new DateTime("2017-05-06"),
        periodEndDate = new DateTime("2017-06-05"),
        automaticRecoveryAmount = Some(250),
        withdrawalAmount = 2000,
        withdrawalChargeAmount = 500,
        withdrawalChargeAmountYtd = 500,
        fundsDeductedDuringWithdrawal = true,
        withdrawalReason = "Superseded withdrawal",
        supersededBy = Some("1234567892"),
        supersede = Some(WithdrawalSuperseded("1234567890", 250, 250, "Additional withdrawal")),
        paymentStatus = "Collected",
        creationDate = new DateTime("2017-06-19")
      )))

      doGetRequest(res => {
        status(res) mustBe OK
        contentAsJson(res) mustBe Json.obj(
          "claimPeriodStartDate" -> "2017-05-06",
          "claimPeriodEndDate" -> "2017-06-05",
          "automaticRecoveryAmount" -> 250,
          "withdrawalAmount" -> 2000,
          "withdrawalChargeAmount" -> 500,
          "withdrawalChargeAmountYTD" -> 500,
          "fundsDeductedDuringWithdrawal" -> true,
          "withdrawalReason" -> "Superseded withdrawal",
          "supersededBy" -> "1234567892",
          "supersede" -> Json.obj(
            "originalTransactionId" -> "1234567890",
            "originalWithdrawalChargeAmount" -> 250,
            "transactionResult" -> 250,
            "reason" -> "Additional withdrawal"
          )
        )
      })
    }

    "return 404 status invalid lisa account (investor id not found)" in {
      when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalInvestorNotFoundResponse))
      doGetRequest(res => {
        status(res) mustBe (NOT_FOUND)
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
        (json \ "message").as[String] mustBe ErrorAccountNotFound.message
      })
    }

    "return 404 transaction not found" when {

      "given a transaction not found error from the connector" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalTransactionNotFoundResponse))
        doGetRequest(res => {
          status(res) mustBe (NOT_FOUND)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorWithdrawalNotFound.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalNotFound.message
        })
      }

      "given a withdrawal charge transaction from the connector" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0, 10)),
          InboundPayments(Some(4000), 4000, 4000, 4000),
          Bonuses(1000, 1000, Some(1000), "Life Event"),
          Some("1234567892"),
          Some(BonusRecovery(100, "1234567890", 1100, -100)),
          "Paid",
          new DateTime("2017-05-20"))
        ))

        doGetRequest(res => {
          status(res) mustBe (NOT_FOUND)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorWithdrawalNotFound.errorCode
          (json \ "message").as[String] mustBe ErrorWithdrawalNotFound.message
        })
      }

    }

    "return with status 406 not acceptable" when {

      "attempting to use the v1 of the api" in {
        doGetRequest(
          res => {
            status(res) mustBe NOT_ACCEPTABLE
            (contentAsJson(res) \ "code").as[String] mustBe "ACCEPT_HEADER_INVALID"
            (contentAsJson(res) \ "message").as[String] mustBe "The accept header has an invalid version for this endpoint"
          },
          header = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
        )
      }

    }

    "return a internal server error response" in {
      when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).
        thenReturn(Future.successful(GetBonusOrWithdrawalErrorResponse))

      doGetRequest(res => {
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      })
    }

    "return a service unavailable response" in {
      when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).
        thenReturn(Future.successful(GetBonusOrWithdrawalServiceUnavailableResponse))

      doGetRequest(res => {
        status(res) mustBe SERVICE_UNAVAILABLE
        (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
      })
    }

    "audit getWithdrawalChargeReported" when {
      "given a successful response" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetWithdrawalResponse(
          periodStartDate = new DateTime("2017-05-06"),
          periodEndDate = new DateTime("2017-06-05"),
          automaticRecoveryAmount = Some(250),
          withdrawalAmount = 2000,
          withdrawalChargeAmount = 500,
          withdrawalChargeAmountYtd = 500,
          fundsDeductedDuringWithdrawal = true,
          withdrawalReason = "Superseded withdrawal",
          supersededBy = Some("1234567892"),
          supersede = Some(WithdrawalSuperseded("1234567890", 250, 250, "Additional withdrawal")),
          paymentStatus = "Collected",
          creationDate = new DateTime("2017-06-19")
        )))

        doGetRequest(res => {
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("getWithdrawalChargeReported"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionId" -> transactionId
            ))
          )(any())
        })
      }
    }

    "audit getWithdrawalChargeNotReported" when {
      "given an investor id is not found" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalInvestorNotFoundResponse))
        doGetRequest(res => {
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("getWithdrawalChargeNotReported"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionId" -> transactionId,
              "reasonNotReported" -> ErrorAccountNotFound.errorCode
            ))
          )(any())
        })
      }
      "given a transaction not found error from the connector" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalTransactionNotFoundResponse))
        doGetRequest(res => {
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("getWithdrawalChargeNotReported"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionId" -> transactionId,
              "reasonNotReported" -> ErrorWithdrawalNotFound.errorCode
            ))
          )(any())
        })
      }

      "given a withdrawal charge transaction from the connector" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0, 10)),
          InboundPayments(Some(4000), 4000, 4000, 4000),
          Bonuses(1000, 1000, Some(1000), "Life Event"),
          Some("1234567892"),
          Some(BonusRecovery(100, "1234567890", 1100, -100)),
          "Paid",
          new DateTime("2017-05-20"))
        ))

        doGetRequest(res => {
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("getWithdrawalChargeNotReported"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionId" -> transactionId,
              "reasonNotReported" -> ErrorWithdrawalNotFound.errorCode
            ))
          )(any())
        })
      }
      "given a internal server error response" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBonusOrWithdrawalErrorResponse))

        doGetRequest(res => {
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("getWithdrawalChargeNotReported"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionId" -> transactionId,
              "reasonNotReported" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        })
      }

      "given a service unavailable response" in {
        when(mockBonusOrWithdrawalService.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBonusOrWithdrawalServiceUnavailableResponse))

        doGetRequest(res => {
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("getWithdrawalChargeNotReported"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/withdrawal-charges/$transactionId"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "transactionId" -> transactionId,
              "reasonNotReported" -> "SERVER_ERROR"
            ))
          )(any())
        })
      }
    }

  }

  def doRequest(jsonString: String, lmrn: String = lisaManager, header: (String, String) = acceptHeader)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = withdrawalController.reportWithdrawalCharge(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(header).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doGetRequest(callback: (Future[Result]) =>  Unit, header: (String, String) = acceptHeader): Unit = {
    val res = withdrawalController.getWithdrawalCharge(lisaManager, accountId, transactionId).apply(FakeRequest(Helpers.GET, "/").withHeaders(header))

    callback(res)
  }
}
