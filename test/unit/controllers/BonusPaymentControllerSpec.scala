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

import org.joda.time.DateTime
import org.mockito.Matchers.{eq => MatcherEquals, _}
import org.mockito.Mockito._
import org.scalatest._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{BonusPaymentController, ErrorAccountNotFound, ErrorBadRequestLmrn, ErrorTransactionNotFound}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusPaymentService}
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class BonusPaymentControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach {

  case object TestBonusPaymentResponse extends RequestBonusPaymentResponse

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"
  val transactionId = "1234567890"
  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString
  val validBonusPaymentMinimumFieldsJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.min.json")).mkString
  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    when(mockAuthCon.authorise[Option[String]](any(),any())(any())).thenReturn(Future(Some("1234")))
  }

  "the POST bonus payment endpoint" must {

    "return with status 201 created" when {

      "given a Success Response from the service layer" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374","On Time")))

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
        doRequest(validBonusPaymentJson.replace(""""lifeEventId": "1234567891",""", "")) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("LIFE_EVENT_NOT_PROVIDED")
          (contentAsJson(res) \ "message").as[String] mustBe ("lifeEventId is required when the claimReason is \"Life Event\"")
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

      "when the json request fails business validation" in {
        val validBonusPayment = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(1), newSubsYTD = 0, totalSubsForPeriod = 0)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 1, htbTransferTotalYTD = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))

        doRequest(Json.toJson(request).toString()) { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"

          val errors = (json \ "errors").as[List[Map[String, String]]]

          errors.size mustBe 3
          errors(0)("path") mustBe "/inboundPayments/newSubsYTD"
          errors(1)("path") mustBe "/htbTransfer/htbTransferTotalYTD"
          errors(2)("path") mustBe "/inboundPayments/totalSubsForPeriod"
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
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374","Bonus transaction created")))

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
        }
      }

      "given a success response from the service layer and no optional fields" in {
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(RequestBonusPaymentSuccessResponse("1928374", "Bonus transaction created - Late Notification")))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { res =>

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
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        val invalidJson = validBonusPaymentJson.replace("\"lifeEventId\": \"1234567891\",", "")

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
              "reasonNotRequested" -> "LIFE_EVENT_NOT_PROVIDED"
            )
            ))(any())
        }
      }

      "the request fails business rule validation" in {
        val validBonusPayment = Json.parse(validBonusPaymentJson).as[RequestBonusPaymentRequest]

        val ibp = validBonusPayment.inboundPayments.copy(newSubsForPeriod = Some(1), newSubsYTD = 0, totalSubsForPeriod = 0)
        val htb = validBonusPayment.htbTransfer.get.copy(htbTransferInForPeriod = 1, htbTransferTotalYTD = 0)
        val request = validBonusPayment.copy(inboundPayments = ibp, htbTransfer = Some(htb))
        val json = Json.toJson(request)

        reset(mockService)

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
        when(mockService.requestBonusPayment(any(), any(), any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        doSyncRequest(validBonusPaymentMinimumFieldsJson) { _ =>
          reset(mockService) // removes the thenThrow

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

    "return 200 success response" in {
      when(mockService.getBonusPayment(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusPaymentSuccessResponse(Some("1234567891"),
        new DateTime("2017-04-06"),
        new DateTime("2017-05-05"),
        transactionType = "Bonus",
        Some(HelpToBuyTransfer(0f, 10f)),
        InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
        Bonuses(1000f, 1000f, Some(1000f), "Life Event"))))

      doGetBonusPaymentTransactionRequest(res => {
        status(res) mustBe OK
        contentAsJson(res) mustBe Json.toJson (GetBonusPaymentSuccessResponse(Some("1234567891"),
          new DateTime("2017-04-06"),
          new DateTime("2017-05-05"),
          transactionType = "Bonus",
          Some(HelpToBuyTransfer(0f, 10f)),
          InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
          Bonuses(1000f, 1000f, Some(1000f), "Life Event")))
      })
    }

    "return 400 when an invalid lmrn is being sent" in {
      when(mockService.getBonusPayment(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusPaymentLmrnDoesNotExistResponse))

      doGetBonusPaymentTransactionRequest { res =>
        status(res) mustBe (BAD_REQUEST)
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
        (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
      }
    }
    "return 404 status invalid lisa account (investor id not found)" in {
      when(mockService.getBonusPayment(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusPaymentInvestorNotFoundResponse))
      doGetBonusPaymentTransactionRequest(res => {
        status(res) mustBe (NOT_FOUND)
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
        (json \ "message").as[String] mustBe ErrorAccountNotFound.message
      })
    }

    "return 404 transcation not found" in {
      when(mockService.getBonusPayment(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusPaymentTransactionNotFoundResponse))
      doGetBonusPaymentTransactionRequest(res => {
        status(res) mustBe (NOT_FOUND)
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe ErrorTransactionNotFound.errorCode
        (json \ "message").as[String] mustBe ErrorTransactionNotFound.message
      })
    }
    "return an internal server error response" in {
      when(mockService.getBonusPayment(any(), any(), any())(any())).thenReturn(Future.successful(GetBonusPaymentErrorResponse))
      doGetBonusPaymentTransactionRequest(res => {
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      })
    }
  }

  def doRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.requestBonusPayment(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncRequest(jsonString: String)(callback: (Result) =>  Unit): Unit = {
    val res = await(SUT.requestBonusPayment(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }


  def doGetBonusPaymentTransactionRequest(callback: (Future[Result]) => Unit) {
    val res = SUT.getBonusPayment(lisaManager, accountId, transactionId).apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
    callback(res)
  }

  val mockService: BonusPaymentService = mock[BonusPaymentService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon :LisaAuthConnector = mock[LisaAuthConnector]
  val SUT = new BonusPaymentController {
    override val service: BonusPaymentService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
  }
}
