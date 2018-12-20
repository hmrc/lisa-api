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
import org.mockito.Matchers.{eq => MatcherEquals, _}
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsError, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{BonusPaymentController, ErrorAccountNotFound, ErrorBadRequestLmrn, ErrorBonusPaymentTransactionNotFound, ErrorValidation}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusOrWithdrawalService, BonusPaymentService, CurrentDateService}
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class BonusPaymentControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach
  with LisaConstants {

  case object TestBonusPaymentResponse extends RequestBonusPaymentResponse

  val acceptHeaderV1: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val acceptHeaderV2: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val transactionId = "1234567890"
  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString
  val validBonusPaymentMinimumFieldsJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.min.json")).mkString
  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockDateTimeService)
    reset(mockValidator)

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockDateTimeService.now()).thenReturn(new DateTime("2018-01-01"))
    when(mockValidator.validate(any())).thenReturn(Nil)
  }

  "the POST bonus payment endpoint" must {

    "return with status 201 created" when {

      "given a RequestBonusPaymentOnTimeResponse from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentOnTimeResponse("1928374")))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Bonus transaction created"
        }
      }

      "given a RequestBonusPaymentLateResponse from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentLateResponse("1928374")))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Bonus transaction created - late notification"
        }
      }

      "given a RequestBonusPaymentSupersededResponse from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSupersededResponse("1928374")))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Bonus transaction superseded"
        }
      }

    }

    "return with status 400 bad request" when {

      "provided an invalid json object" in {
        doRequest(validBonusPaymentJson.replace("1234567891","X")) { res =>
          status(res) mustBe (BAD_REQUEST)
          (contentAsJson(res) \ "code").as[String] mustBe ("BAD_REQUEST")
          (contentAsJson(res) \ "message").as[String] mustBe ("Bad Request")
        }
      }

      "provided an invalid lmrn" in {
        doRequest(validBonusPaymentJson, "Z1234567") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }

      "attempting a superseded claim on v1 of the api" in {
        doRequest(validBonusPaymentJson.replace("Life Event", "Superseded Bonus"))(
          res => {
            status(res) mustBe BAD_REQUEST
            val json = contentAsJson(res)
            (json \ "code").as[String] mustBe "BAD_REQUEST"
            (json \ "message").as[String] mustBe "Bad Request"
            (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_FORMAT"
            (json \ "errors" \ 0 \ "message").as[String] mustBe "Invalid format has been used"
            (json \ "errors" \ 0 \ "path").as[String] mustBe "/bonuses/claimReason"
          },
          acceptHeaderV1
        )
      }

    }

    "return with status 403 forbidden" when {

      "the bonus claim reason is life event and no life event id is provided" in {
        doRequest(validBonusPaymentJson.replace(""""lifeEventId": "1234567891",""", "")) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_NOT_PROVIDED"
          (contentAsJson(res) \ "message").as[String] mustBe "lifeEventId is required when the claimReason is a life event"
        }
      }

      "the json request fails business validation" in {
        val errors = List(
          ErrorValidation(
            MONETARY_ERROR,
            "htbTransferTotalYTD must be more than 0",
            Some("/htbTransfer/htbTransferTotalYTD")
          ),
          ErrorValidation(
            DATE_ERROR,
            "The periodStartDate must be the 6th day of the month",
            Some("/periodStartDate")
          )
        )

        when(mockValidator.validate(any())).thenReturn(errors)

        val request = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]

        doRequest(Json.toJson(request).toString()) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"

          (json \ "errors" \ 0 \ "path").as[String] mustBe "/htbTransfer/htbTransferTotalYTD"
          (json \ "errors" \ 1 \ "path").as[String] mustBe "/periodStartDate"
        }
      }

      "the periodEndDate is more than 6 years and 14 days in the past" in {
        val now = new DateTime("2050-01-20")

        when(mockDateTimeService.now()).thenReturn(now)

        val testEndDate = now.minusYears(6).withDayOfMonth(5)
        val testStartDate = testEndDate.minusMonths(1).plusDays(1)

        val validBonusPayment = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]
        val request = validBonusPayment.copy(periodStartDate = testStartDate, periodEndDate = testEndDate)

        doRequest(Json.toJson(request).toString()) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "BONUS_CLAIM_TIMESCALES_EXCEEDED"
          (json \ "message").as[String] mustBe "The timescale for claiming a bonus has passed. The claim period lasts for 6 years and 14 days"
        }
      }

      "help to buy is populated for a claim with a start date of 6 April 2018 or after" in {
        val validBonusPayment = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]
        val request = validBonusPayment.copy(periodStartDate = new DateTime("2018-04-06"), periodEndDate = new DateTime("2018-05-05"))

        doRequest(Json.toJson(request).toString()) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "HELP_TO_BUY_NOT_APPLICABLE"
          (json \ "message").as[String] mustBe "Help to Buy is not applicable on this account"
        }
      }

      "given a RequestBonusPaymentBonusClaimError response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentBonusClaimError))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "BONUS_CLAIM_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The bonus amount given is above the maximum annual amount, " +
          "or the qualifying deposits are above the maximum annual amount or the bonus claim does not equal the correct " +
          "percentage of qualifying funds"
        }
      }

      "given a RequestBonusPaymentAccountClosedOrVoid response from the service layer for v1" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountClosedOrVoid))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe FORBIDDEN
            (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID"
            (contentAsJson(res) \ "message").as[String] mustBe "This LISA account has already been closed or been made void by HMRC"
          },
          acceptHeaderV1
        )
      }

      "given a RequestBonusPaymentAccountClosed response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountClosed))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED"
          (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already closed"
        }
      }

      "given a RequestBonusPaymentAccountCancelled response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountCancelled))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
          (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already cancelled"
        }
      }

      "given a RequestBonusPaymentAccountVoid response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountVoid))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_VOID"
          (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already void"
        }
      }

      "given a RequestBonusPaymentSupersededAmountMismatch response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentSupersededAmountMismatch))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_BONUS_CLAIM_AMOUNT_MISMATCH"
          (contentAsJson(res) \ "message").as[String] mustBe "originalTransactionId and the originalBonusDueForPeriod amount do not match the information in the original bonus request"
        }

      }

      "given a RequestBonusPaymentSupersededOutcomeError response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentSupersededOutcomeError))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_BONUS_REQUEST_OUTCOME_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "The calculation from your superseded bonus claim is incorrect"
        }

      }

      "given a RequestBonusPaymentNoSubscriptions response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentNoSubscriptions))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe FORBIDDEN
          (contentAsJson(res) \ "code").as[String] mustBe "ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR"
          (contentAsJson(res) \ "message").as[String] mustBe "A bonus payment is not possible because the account has no subscriptions for that tax year"
        }

      }

    }

    "return with status 404 not found" when {

      "given a RequestBonusPaymentAccountNotFound response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentAccountNotFound))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe NOT_FOUND
          (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
          (contentAsJson(res) \ "message").as[String] mustBe "The accountId does not match HMRC’s records"
        }
      }

      "given a RequestBonusPaymentLifeEventNotFound response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentLifeEventNotFound))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe NOT_FOUND
          (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_NOT_FOUND"
          (contentAsJson(res) \ "message").as[String] mustBe "The lifeEventId does not match with HMRC’s records"
        }
      }

    }

    "return with status 409 conflict" when {

      "given a RequestBonusPaymentClaimAlreadyExists response from the service layer for api v2" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentClaimAlreadyExists(transactionId)))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe CONFLICT
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "BONUS_CLAIM_ALREADY_EXISTS"
          (json \ "message").as[String] mustBe "The investor’s bonus payment has already been requested"
          (json \ "transactionId").as[String] mustBe transactionId
        }
      }

      "given a RequestBonusPaymentAlreadySuperseded response from the service layer for v2" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentAlreadySuperseded(transactionId)))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe CONFLICT
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "BONUS_CLAIM_ALREADY_SUPERSEDED"
          (json \ "message").as[String] mustBe "This bonus claim has already been superseded"
          (json \ "transactionId").as[String] mustBe transactionId
        }
      }

    }

    "return with status 500 internal server error" when {

      "a exception gets thrown" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validBonusPaymentJson) { res =>
          reset(mockPostService) // removes the thenThrow
          status(res) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(res) mustBe internalServerError
        }
      }

      "a unexpected result comes back from the service" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(TestBonusPaymentResponse))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(res) mustBe internalServerError
        }
      }

      "a RequestBonusPaymentError response is received" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentError))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(res) mustBe internalServerError
        }
      }

      "a RequestBonusPaymentAccountClosed response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountClosed))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentAccountCancelled response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountCancelled))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentAccountVoid response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountVoid))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentSupersededAmountMismatch response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentSupersededAmountMismatch))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentSupersededOutcomeError response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentSupersededOutcomeError))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentClaimAlreadyExists response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentClaimAlreadyExists(transactionId)))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentAlreadySuperseded response is received for version 1 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentAlreadySuperseded(transactionId)))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }

      "a RequestBonusPaymentAccountClosedOrVoid response is received for version 2 of the api" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).thenReturn(
          Future.successful(RequestBonusPaymentAccountClosedOrVoid))

        doRequest(validBonusPaymentJson)(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV2
        )
      }

    }

    "return with status 503 service unavailable" when {

      "a exception gets thrown" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentServiceUnavailable))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe SERVICE_UNAVAILABLE
          (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        }
      }

    }

    "audit bonusPaymentRequested" when {

      "given a success response from the service layer and all optional fields" when {
        "using v1 of the API" in {
          when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
            thenReturn(Future.successful(RequestBonusPaymentOnTimeResponse("1928374")))

          doSyncRequest(validBonusPaymentJson)(
            _ => {
              val json = Json.parse(validBonusPaymentJson)
              val htb = json \ "htbTransfer"
              val inboundPayments = json \ "inboundPayments"
              val bonuses = json \ "bonuses"

              verify(mockAuditService).audit(
                auditType = MatcherEquals("bonusPaymentRequested"),
                path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
                auditData = MatcherEquals(Map(
                  "lisaManagerReferenceNumber" -> lisaManager,
                  "accountId" -> accountId,
                  "lateNotification" -> "no",
                  "lifeEventId" -> (json \ "lifeEventId").as[String],
                  "periodStartDate" -> (json \ "periodStartDate").as[String],
                  "periodEndDate" -> (json \ "periodEndDate").as[String],
                  "htbTransferInForPeriod" -> (htb \ "htbTransferInForPeriod").as[Amount].toString,
                  "htbTransferTotalYTD" -> (htb \ "htbTransferTotalYTD").as[Amount].toString,
                  "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Amount].toString,
                  "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Amount].toString,
                  "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Amount].toString,
                  "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Amount].toString,
                  "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Amount].toString,
                  "bonusPaidYTD" -> (bonuses \ "bonusPaidYTD").as[Amount].toString,
                  "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Amount].toString,
                  "claimReason" -> (bonuses \ "claimReason").as[String]
                ))
              )(any())
            },
            acceptHeaderV1
          )
        }
        "using v2 of the API" in {
          when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
            thenReturn(Future.successful(RequestBonusPaymentOnTimeResponse("1928374")))

          doSyncRequest(validBonusPaymentJson)(
            _ => {
              val json = Json.parse(validBonusPaymentJson)
              val htb = json \ "htbTransfer"
              val inboundPayments = json \ "inboundPayments"
              val bonuses = json \ "bonuses"
              val supersede = json \ "supersede"

              verify(mockAuditService).audit(
                auditType = MatcherEquals("bonusPaymentRequested"),
                path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
                auditData = MatcherEquals(Map(
                  "lisaManagerReferenceNumber" -> lisaManager,
                  "accountId" -> accountId,
                  "lateNotification" -> "no",
                  "lifeEventId" -> (json \ "lifeEventId").as[String],
                  "periodStartDate" -> (json \ "periodStartDate").as[String],
                  "periodEndDate" -> (json \ "periodEndDate").as[String],
                  "htbTransferInForPeriod" -> (htb \ "htbTransferInForPeriod").as[Amount].toString,
                  "htbTransferTotalYTD" -> (htb \ "htbTransferTotalYTD").as[Amount].toString,
                  "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Amount].toString,
                  "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Amount].toString,
                  "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Amount].toString,
                  "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Amount].toString,
                  "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Amount].toString,
                  "bonusPaidYTD" -> (bonuses \ "bonusPaidYTD").as[Amount].toString,
                  "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Amount].toString,
                  "claimReason" -> (bonuses \ "claimReason").as[String],
                  "automaticRecoveryAmount" -> (supersede \ "automaticRecoveryAmount").as[Amount].toString,
                  "originalTransactionId" -> (supersede \ "originalTransactionId").as[String],
                  "originalBonusDueForPeriod" -> (supersede \ "originalBonusDueForPeriod").as[Amount].toString,
                  "transactionResult" -> (supersede \ "transactionResult").as[Amount].toString,
                  "reason" -> (supersede \ "reason").as[String]
                ))
              )(any())
            },
            acceptHeaderV2
          )
        }
      }

      "given a success response from the service layer and no optional fields" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentLateResponse("1928374")))

        doSyncRequest(validBonusPaymentMinimumFieldsJson)(
          _ => {
            val json = Json.parse(validBonusPaymentMinimumFieldsJson)
            val inboundPayments = json \ "inboundPayments"
            val bonuses = json \ "bonuses"

            verify(mockAuditService).audit(
              auditType = MatcherEquals("bonusPaymentRequested"),
              path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
              auditData = MatcherEquals(Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountId" -> accountId,
                "lateNotification" -> "yes",
                "periodStartDate" -> (json \ "periodStartDate").as[String],
                "periodEndDate" -> (json \ "periodEndDate").as[String],
                "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Amount].toString,
                "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Amount].toString,
                "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Amount].toString,
                "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Amount].toString,
                "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Amount].toString,
                "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Amount].toString,
                "claimReason" -> (bonuses \ "claimReason").as[String]
              )
              ))(any())
          },
          acceptHeaderV1
        )
      }

    }

    "audit bonusPaymentNotRequested" when {

      "given an error response from the service layer" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentLifeEventNotFound))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { res =>

          val json = Json.parse(validBonusPaymentMinimumFieldsJson)
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Amount].toString,
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Amount].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Amount].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Amount].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Amount].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Amount].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "LIFE_EVENT_NOT_FOUND"
            )
            ))(any())
        }
      }

      "the bonus claim reason is life event and no life event id is provided" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        val invalidJson = validBonusPaymentJson.replace("\"lifeEventId\": \"1234567891\",", "")

        doSyncRequest(invalidJson) { res =>
          reset(mockPostService) // removes the thenThrow

          val json = Json.parse(invalidJson)
          val htb = json \ "htbTransfer"
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"
          val supersede = json \ "supersede"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "htbTransferInForPeriod" -> (htb \ "htbTransferInForPeriod").as[Amount].toString,
              "htbTransferTotalYTD" -> (htb \ "htbTransferTotalYTD").as[Amount].toString,
              "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Amount].toString,
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Amount].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Amount].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Amount].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Amount].toString,
              "bonusPaidYTD" -> (bonuses \ "bonusPaidYTD").as[Amount].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Amount].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "LIFE_EVENT_NOT_PROVIDED",
              "automaticRecoveryAmount" -> (supersede \ "automaticRecoveryAmount").as[Amount].toString,
              "originalTransactionId" -> (supersede \ "originalTransactionId").as[String],
              "originalBonusDueForPeriod" -> (supersede \ "originalBonusDueForPeriod").as[Amount].toString,
              "transactionResult" -> (supersede \ "transactionResult").as[Amount].toString,
              "reason" -> (supersede \ "reason").as[String]
            )
            ))(any())
        }
      }

      "the request fails business rule validation" in {
        val errors = List(
          ErrorValidation(
            MONETARY_ERROR,
            "htbTransferTotalYTD must be more than 0",
            Some("/htbTransfer/htbTransferTotalYTD")
          ),
          ErrorValidation(
            DATE_ERROR,
            "The periodStartDate must be the 6th day of the month",
            Some("/periodStartDate")
          )
        )

        when(mockValidator.validate(any())).thenReturn(errors)

        val request = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]
        val json = Json.toJson(request)

        reset(mockPostService)

        doSyncRequest(json.toString()) { _ =>
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"
          val htb = json \ "htbTransfer"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "lifeEventId" -> (json \ "lifeEventId").as[String],
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "htbTransferInForPeriod" -> (htb \ "htbTransferInForPeriod").as[Int].toString,
              "htbTransferTotalYTD" -> (htb \ "htbTransferTotalYTD").as[Int].toString,
              "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Int].toString,
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Int].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Int].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Int].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Int].toString,
              "bonusPaidYTD" -> (bonuses \ "bonusPaidYTD").as[Int].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Int].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "FORBIDDEN"
            ))
          )(any())
        }
      }

      "an error occurs" in {
        when(mockPostService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { _ =>
          reset(mockPostService) // removes the thenThrow

          val json = Json.parse(validBonusPaymentMinimumFieldsJson)
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Amount].toString,
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Amount].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Amount].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Amount].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Amount].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Amount].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "INTERNAL_SERVER_ERROR"
            )
            ))(any())
        }
      }

    }

  }

  "the GET bonus payment endpoint" must {

    "return 200 success response with all supersede data" when {
      "given an api version of 2" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0f, 10f)),
          InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
          Bonuses(1000f, 1000f, Some(1000f), "Life Event"),
          Some("1234567892"),
          Some(BonusRecovery(100, "1234567890", 1100, -100)),
          "Paid",
          new DateTime("2017-05-20"))
        ))

        doGetBonusPaymentTransactionRequest(
          res => {
            status(res) mustBe OK
            contentAsJson(res) mustBe Json.obj(
              "lifeEventId" -> "1234567891",
              "periodStartDate" -> "2017-04-06",
              "periodEndDate" -> "2017-05-05",
              "htbTransfer" -> Json.obj(
                "htbTransferInForPeriod" -> 0,
                "htbTransferTotalYTD" -> 10
              ),
              "inboundPayments" -> Json.obj(
                "newSubsForPeriod" -> 4000,
                "newSubsYTD" -> 4000,
                "totalSubsForPeriod" -> 4000,
                "totalSubsYTD" -> 4000
              ),
              "bonuses" -> Json.obj(
                "bonusDueForPeriod" -> 1000,
                "totalBonusDueYTD" -> 1000,
                "bonusPaidYTD" -> 1000,
                "claimReason" -> "Life Event"
              ),
              "supersede" -> Json.obj(
                "automaticRecoveryAmount" -> 100,
                "originalTransactionId" -> "1234567890",
                "originalBonusDueForPeriod" -> 1100,
                "transactionResult" -> -100,
                "reason" -> "Bonus recovery"
              ),
              "supersededBy" -> "1234567892"
            )
          }
        )
      }
    }

    "return 200 success response without supersededBy data" when {
      "given an api version of 1 for a request which has been superseded" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0f, 10f)),
          InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
          Bonuses(1000f, 1000f, Some(1000f), "Life Event"),
          Some("1234567892"),
          None,
          "Paid",
          new DateTime("2017-05-20"))
        ))

        doGetBonusPaymentTransactionRequest(
          res => {
            status(res) mustBe OK
            contentAsJson(res) mustBe Json.obj(
              "lifeEventId" -> "1234567891",
              "periodStartDate" -> "2017-04-06",
              "periodEndDate" -> "2017-05-05",
              "htbTransfer" -> Json.obj(
                "htbTransferInForPeriod" -> 0,
                "htbTransferTotalYTD" -> 10
              ),
              "inboundPayments" -> Json.obj(
                "newSubsForPeriod" -> 4000,
                "newSubsYTD" -> 4000,
                "totalSubsForPeriod" -> 4000,
                "totalSubsYTD" -> 4000
              ),
              "bonuses" -> Json.obj(
                "bonusDueForPeriod" -> 1000,
                "totalBonusDueYTD" -> 1000,
                "bonusPaidYTD" -> 1000,
                "claimReason" -> "Life Event"
              )
            )
          },
          acceptHeaderV1
        )
      }
    }

    "return 404 status invalid lisa account (investor id not found)" in {
      when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalInvestorNotFoundResponse))
      doGetBonusPaymentTransactionRequest(res => {
        status(res) mustBe (NOT_FOUND)
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
        (json \ "message").as[String] mustBe ErrorAccountNotFound.message
      })
    }

    "return 404 transaction not found" when {

      "given a transaction not found error from the connector" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalTransactionNotFoundResponse))
        doGetBonusPaymentTransactionRequest(res => {
          status(res) mustBe (NOT_FOUND)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBonusPaymentTransactionNotFound.errorCode
          (json \ "message").as[String] mustBe ErrorBonusPaymentTransactionNotFound.message
        })
      }

      "given a withdrawal charge transaction from the connector" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetWithdrawalResponse(
          new DateTime("2017-05-06"),
          new DateTime("2017-06-05"),
          None,
          1000,
          250,
          250,
          true,
          "",
          None,
          None,
          "Collected",
          new DateTime("2017-06-19")
        )))
        doGetBonusPaymentTransactionRequest(res => {
          status(res) mustBe (NOT_FOUND)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBonusPaymentTransactionNotFound.errorCode
          (json \ "message").as[String] mustBe ErrorBonusPaymentTransactionNotFound.message
        })
      }

    }

    "return a internal server error response" when {
      "an error is returned from the service layer" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusOrWithdrawalErrorResponse))
        doGetBonusPaymentTransactionRequest(res => {
          contentAsJson(res) mustBe internalServerError
        })
      }
      "given an api version of 1 for a request which supersedes another" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusResponse(
          Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          Some(HelpToBuyTransfer(0f, 10f)),
          InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
          Bonuses(1000f, 1000f, Some(1000f), "Superseded Bonus"),
          Some("1234567892"),
          Some(BonusRecovery(100, "1234567890", 1100, -100)),
          "Paid",
          new DateTime("2017-05-20"))
        ))

        doGetBonusPaymentTransactionRequest(
          res => {
            status(res) mustBe INTERNAL_SERVER_ERROR
            contentAsJson(res) mustBe internalServerError
          },
          acceptHeaderV1
        )
      }
    }

    "return a service unavailable response" when {

      "GetBonusOrWithdrawalServiceUnavailableResponse is returned from the service layer" in {
        when(mockGetService.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBonusOrWithdrawalServiceUnavailableResponse))

        doGetBonusPaymentTransactionRequest(res => {
          status(res) mustBe SERVICE_UNAVAILABLE
          (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        })
      }
    }

  }

  val internalServerError = Json.obj("code" -> "INTERNAL_SERVER_ERROR", "message" -> "Internal server error")

  def doRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) =>  Unit, header: (String, String) = acceptHeaderV2): Unit = {
    val res = SUT.requestBonusPayment(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(header).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncRequest(jsonString: String)(callback: (Result) =>  Unit, header: (String, String) = acceptHeaderV2): Unit = {
    val res = await(SUT.requestBonusPayment(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(header).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  def doGetBonusPaymentTransactionRequest(callback: (Future[Result]) => Unit, header: (String, String) = acceptHeaderV2) {
    val res = SUT.getBonusPayment(lisaManager, accountId, transactionId).apply(FakeRequest(Helpers.GET, "/").withHeaders(header))
    callback(res)
  }

  val mockPostService: BonusPaymentService = mock[BonusPaymentService]
  val mockGetService: BonusOrWithdrawalService = mock[BonusOrWithdrawalService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val mockDateTimeService: CurrentDateService = mock[CurrentDateService]
  val mockValidator: BonusPaymentValidator = mock[BonusPaymentValidator]

  val SUT = new BonusPaymentController {
    override val postService: BonusPaymentService = mockPostService
    override val getService: BonusOrWithdrawalService = mockGetService
    override val auditService: AuditService = mockAuditService
    override val authConnector: LisaAuthConnector = mockAuthCon
    override val validator: BonusPaymentValidator = mockValidator
    override val dateTimeService: CurrentDateService = mockDateTimeService
    override lazy val v2endpointsEnabled = true
  }
}
