/*
 * Copyright 2017 HM Revenue & Customs
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

import org.mockito.Matchers._
import org.mockito.Matchers.{eq=>MatcherEquals, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json._
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.{BonusPaymentController, JsonFormats}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusPaymentService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.io.Source

case object TestBonusPaymentResponse extends RequestBonusPaymentResponse

class BonusPaymentControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach
  with JsonFormats {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"
  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString
  val validBonusPaymentMinimumFieldsJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.min.json")).mkString
  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
  }

  "The Life Event Controller" should {

    "return with status 201 created" when {

      "given a Success Response from the service layer" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374")))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe ("1928374")
        }
      }

    }

    "return with status 403 forbidden" when {

      "given a BONUS_CLAIM_ERROR from the service layer" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentErrorResponse(403, DesFailureResponse("BONUS_CLAIM_ERROR", "xyz"))))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("BONUS_CLAIM_ERROR")
          (contentAsJson(res) \ "message").as[String] mustBe ("xyz")
        }
      }

      "the bonus claim reason is life event and no life event id is provided" in {
        doRequest(validBonusPaymentJson.replace(""""lifeEventID": "1234567891",""", "")) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("LIFE_EVENT_NOT_PROVIDED")
          (contentAsJson(res) \ "message").as[String] mustBe ("lifeEventID is required when the claimReason is \"Life Event\"")
        }
      }

      "when account is closed return with INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID code " in {
          when(mockService.requestBonusPayment(any(), any(),any())(any())).thenReturn(
            Future.successful(RequestBonusPaymentErrorResponse(403, DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID", "xyz"))))

        doRequest(validBonusPaymentJson)  { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID")
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

    }

    "return with status 500 internal server error" when {

      "an exception gets thrown" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validBonusPaymentJson) { res =>
          reset(mockService) // removes the thenThrow

          status(res) mustBe (INTERNAL_SERVER_ERROR)
          (contentAsJson(res) \ "code").as[String] mustBe ("INTERNAL_SERVER_ERROR")
        }
      }

      "an unexpected result comes back from the service" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(TestBonusPaymentResponse))

        doRequest(validBonusPaymentJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
          (contentAsJson(res) \ "code").as[String] mustBe ("INTERNAL_SERVER_ERROR")
        }
      }

    }

    "audit bonusPaymentRequested" when {

      "given a success response from the service layer and all optional fields" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374")))

        doSyncRequest(validBonusPaymentJson) { res =>

          val json = Json.parse(validBonusPaymentJson)
          val htb = json \ "htbTransfer"
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "lifeEventID" -> (json \ "lifeEventID").as[String],
              "transactionType" -> (json \ "transactionType").as[String],
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "htbTransferInForPeriod" -> (htb \ "htbTransferInForPeriod").as[Float].toString,
              "htbTransferTotalYTD" -> (htb \ "htbTransferTotalYTD").as[Float].toString,
              "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Float].toString,
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Float].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Float].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Float].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Float].toString,
              "bonusPaidYTD" -> (bonuses \ "bonusPaidYTD").as[Float].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Float].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String]
            ))
          )(any())
        }
      }

      "given a success response from the service layer and no optional fields" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374")))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { res =>

          val json = Json.parse(validBonusPaymentMinimumFieldsJson)
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "transactionType" -> (json \ "transactionType").as[String],
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Float].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Float].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Float].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Float].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Float].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String]
            )
          ))(any())
        }
      }

    }

    "audit bonusPaymentNotRequested" when {

      "given an error response from the service layer" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentErrorResponse(404, DesFailureResponse("LIFE_EVENT_NOT_FOUND", ""))))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { res =>

          val json = Json.parse(validBonusPaymentMinimumFieldsJson)
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "transactionType" -> (json \ "transactionType").as[String],
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Float].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Float].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Float].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Float].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Float].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "LIFE_EVENT_NOT_FOUND"
            )
          ))(any())
        }
      }

      "the bonus claim reason is life event and no life event id is provided" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        val invalidJson = validBonusPaymentJson.replace("\"lifeEventID\": \"1234567891\",", "")

        doSyncRequest(invalidJson) { res =>
          reset(mockService) // removes the thenThrow

          val json = Json.parse(invalidJson)
          val htb = json \ "htbTransfer"
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "transactionType" -> (json \ "transactionType").as[String],
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "htbTransferInForPeriod" -> (htb \ "htbTransferInForPeriod").as[Float].toString,
              "htbTransferTotalYTD" -> (htb \ "htbTransferTotalYTD").as[Float].toString,
              "newSubsForPeriod" -> (inboundPayments \ "newSubsForPeriod").as[Float].toString,
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Float].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Float].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Float].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Float].toString,
              "bonusPaidYTD" -> (bonuses \ "bonusPaidYTD").as[Float].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Float].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "LIFE_EVENT_NOT_PROVIDED"
            )
          ))(any())
        }
      }

      "an error occurs" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { res =>
          reset(mockService) // removes the thenThrow

          val json = Json.parse(validBonusPaymentMinimumFieldsJson)
          val inboundPayments = json \ "inboundPayments"
          val bonuses = json \ "bonuses"

          verify(mockAuditService).audit(
            auditType = MatcherEquals("bonusPaymentNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/transactions"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "transactionType" -> (json \ "transactionType").as[String],
              "periodStartDate" -> (json \ "periodStartDate").as[String],
              "periodEndDate" -> (json \ "periodEndDate").as[String],
              "newSubsYTD" -> (inboundPayments \ "newSubsYTD").as[Float].toString,
              "totalSubsForPeriod" -> (inboundPayments \ "totalSubsForPeriod").as[Float].toString,
              "totalSubsYTD" -> (inboundPayments \ "totalSubsYTD").as[Float].toString,
              "bonusDueForPeriod" -> (bonuses \ "bonusDueForPeriod").as[Float].toString,
              "totalBonusDueYTD" -> (bonuses \ "totalBonusDueYTD").as[Float].toString,
              "claimReason" -> (bonuses \ "claimReason").as[String],
              "reasonNotRequested" -> "INTERNAL_SERVER_ERROR"
            )
          ))(any())
        }
      }

    }

  }

  def doRequest(jsonString: String)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.requestBonusPayment(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncRequest(jsonString: String)(callback: (Result) =>  Unit): Unit = {
    val res = await(SUT.requestBonusPayment(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  val mockService: BonusPaymentService = mock[BonusPaymentService]
  val mockAuditService: AuditService = mock[AuditService]
  val SUT = new BonusPaymentController {
    override val service: BonusPaymentService = mockService
    override val auditService: AuditService = mockAuditService
  }
}
