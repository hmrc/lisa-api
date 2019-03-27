/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import org.joda.time.DateTime
import org.mockito.Matchers.{eq => MatcherEquals, _}
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsJson, ControllerComponents, PlayBodyParsers, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers, Injecting}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.controllers.{AnnualReturnController, ErrorBadRequest, ErrorBadRequestAccountId, ErrorBadRequestLmrn, ErrorForbidden, ErrorInternalServerError, ErrorServiceUnavailable, ErrorValidation}
import uk.gov.hmrc.lisaapi.metrics.LisaMetrics
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AnnualReturnControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach
  with LisaConstants
  with Injecting {

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockValidator)
    reset(mockService)

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockValidator.validate(any())).thenReturn(Nil)
    when(mockAppContext.endpointIsDisabled(any())).thenReturn(false)
  }

  "Submit annual return" must {

    "return 201 created" when {
      "the life event service returns a ReportLifeEventSuccessResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

        doRequest(){ res =>
          status(res) mustBe CREATED
          contentAsJson(res) mustBe Json.obj(
            "data" -> Json.obj(
              "lifeEventId" -> "1928374",
              "message" -> "Life event created"
            ),
            "success" -> true,
            "status" -> 201
          )
        }
      }
      "the life event service returns a ReportLifeEventSuccessResponse for a superseded life event" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

        doRequest(payload = supersedeJson){ res =>
          status(res) mustBe CREATED
          contentAsJson(res) mustBe Json.obj(
            "data" -> Json.obj(
              "lifeEventId" -> "1928374",
              "message" -> "Life event superseded"
            ),
            "success" -> true,
            "status" -> 201
          )
        }
      }
    }

    "return 400 bad request" when {
      "given a invalid lmrn in the url" in {
        doRequest(lmrn = "123456") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "given a invalid accountId in the url" in {
        doRequest(accountId = "1234567890!") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
      "given a invalid json payload" in {
        doRequest(payload = Json.obj()) { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)
          val badRequest = ErrorBadRequest(Nil)

          (json \ "code").as[String] mustBe badRequest.errorCode
          (json \ "message").as[String] mustBe badRequest.message
        }
      }
    }

    "return 403 forbidden" when {
      "given a json payload which does not meet the business rules" in {
        val errors = List(ErrorValidation(DATE_ERROR, "The taxYear cannot be before 2017", Some("/taxYear")))
        when(mockValidator.validate(any())).thenReturn(errors)

        doRequest() { res =>
          status(res) mustBe FORBIDDEN

          val json = contentAsJson(res)
          val forbidden = ErrorForbidden(Nil)

          json mustBe Json.obj(
            "code" -> forbidden.errorCode,
            "message" -> forbidden.message,
            "errors" -> Json.arr(
              Json.obj(
                "code" -> DATE_ERROR,
                "message" -> "The taxYear cannot be before 2017",
                "path" -> "/taxYear"
              )
            )
          )
        }
      }
      "the life event service returns a ReportLifeEventAccountVoidResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAccountVoidResponse))

        doRequest(){ res =>
          status(res) mustBe FORBIDDEN
          contentAsJson(res) mustBe Json.obj(
            "code" -> "INVESTOR_ACCOUNT_ALREADY_VOID",
            "message" -> "The LISA account is already void"
          )
        }
      }
      "the life event service returns a ReportLifeEventAccountCancelledResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAccountCancelledResponse))

        doRequest(){ res =>
          status(res) mustBe FORBIDDEN
          contentAsJson(res) mustBe Json.obj(
            "code" -> "INVESTOR_ACCOUNT_ALREADY_CANCELLED",
            "message" -> "The LISA account is already cancelled"
          )
        }
      }
      "the life event service returns a ReportLifeEventMismatchResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventMismatchResponse))

        doRequest(){ res =>
          status(res) mustBe FORBIDDEN
          contentAsJson(res) mustBe Json.obj(
            "code" -> "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR",
            "message" -> "originalLifeEventId and the originalEventDate do not match the information in the original request"
          )
        }
      }
    }

    "return 404 not found" when {
      "the life event service returns a ReportLifeEventAccountNotFoundResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))

        doRequest(){ res =>
          status(res) mustBe NOT_FOUND
          contentAsJson(res) mustBe Json.obj(
            "code" -> "INVESTOR_ACCOUNTID_NOT_FOUND",
            "message" -> "The accountId does not match HMRC’s records"
          )
        }
      }
    }

    "return 406 not acceptable" when {
      "the accept header is for v1.0 of the api" in {
        doRequest(acceptHeader = acceptHeaderV1){ res =>
          status(res) mustBe NOT_ACCEPTABLE
        }
      }
    }

    "return 409 conflict" when {
      "the life event service returns a ReportLifeEventAlreadyExistsResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("123")))

        doRequest(){ res =>
          status(res) mustBe CONFLICT
          contentAsJson(res) mustBe Json.obj(
            "code" -> "LIFE_EVENT_ALREADY_EXISTS",
            "message" -> "The investor’s life event has already been reported",
            "lifeEventId" -> "123"
          )
        }
      }
      "the life event service returns a ReportLifeEventAlreadySupersededResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAlreadySupersededResponse("123")))

        doRequest(){ res =>
          status(res) mustBe CONFLICT
          contentAsJson(res) mustBe Json.obj(
            "code" -> "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED",
            "message" -> "This life event has already been superseded",
            "lifeEventId" -> "123"
          )
        }
      }
    }

    "return 500 internal server error" when {
      "the life event service returns a error" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        doRequest() { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }
      "the life event service returns a unexpected response" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventFundReleaseNotFoundResponse))

        doRequest() { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(res) mustBe Json.toJson(ErrorInternalServerError)
        }
      }
    }

    "return 503 service unavailable" when {
      "the life event service returns a ReportLifeEventServiceUnavailableResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventServiceUnavailableResponse))

        doRequest() { res =>
          status(res) mustBe SERVICE_UNAVAILABLE
          contentAsJson(res) mustBe ErrorServiceUnavailable.asJson
        }
      }
    }

    "audit lifeEventRequested" when {
      "the life event service returns a ReportLifeEventSuccessResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

        val payload = supersedeJson

        doRequest(payload = payload) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("lifeEventRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/events/annual-returns"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "eventType" -> "Statutory Submission",
              "eventDate" -> (payload \ "eventDate").as[String],
              "lisaManagerName" -> (payload \ "lisaManagerName").as[String],
              "taxYear" -> (payload \ "taxYear").as[Int].toString,
              "marketValueCash" -> (payload \ "marketValueCash").as[Int].toString,
              "marketValueStocksAndShares" -> (payload \ "marketValueStocksAndShares").as[Int].toString,
              "annualSubsCash" -> (payload \ "annualSubsCash").as[Int].toString,
              "annualSubsStocksAndShares" -> (payload \ "annualSubsStocksAndShares").as[Int].toString,
              "originalLifeEventId" -> (payload \ "supersede" \ "originalLifeEventId").as[String],
              "originalEventDate" -> (payload \ "supersede" \ "originalEventDate").as[String]
            ))
          )(any())
        }
      }
    }

    "audit lifeEventNotRequested" when {
      "given a json payload which does not meet the business rules" in {
        val errors = List(ErrorValidation(DATE_ERROR, "The taxYear cannot be before 2017", Some("/taxYear")))
        when(mockValidator.validate(any())).thenReturn(errors)

        doRequest() { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("lifeEventNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/events/annual-returns"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "eventType" -> "Statutory Submission",
              "eventDate" -> (json \ "eventDate").as[String],
              "lisaManagerName" -> (json \ "lisaManagerName").as[String],
              "taxYear" -> (json \ "taxYear").as[Int].toString,
              "marketValueCash" -> (json \ "marketValueCash").as[Int].toString,
              "marketValueStocksAndShares" -> (json \ "marketValueStocksAndShares").as[Int].toString,
              "annualSubsCash" -> (json \ "annualSubsCash").as[Int].toString,
              "annualSubsStocksAndShares" -> (json \ "annualSubsStocksAndShares").as[Int].toString,
              "reasonNotRequested" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
      "the life event service returns a failure response" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))

        doRequest() { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("lifeEventNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/events/annual-returns"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "eventType" -> "Statutory Submission",
              "eventDate" -> (json \ "eventDate").as[String],
              "lisaManagerName" -> (json \ "lisaManagerName").as[String],
              "taxYear" -> (json \ "taxYear").as[Int].toString,
              "marketValueCash" -> (json \ "marketValueCash").as[Int].toString,
              "marketValueStocksAndShares" -> (json \ "marketValueStocksAndShares").as[Int].toString,
              "annualSubsCash" -> (json \ "annualSubsCash").as[Int].toString,
              "annualSubsStocksAndShares" -> (json \ "annualSubsStocksAndShares").as[Int].toString,
              "reasonNotRequested" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            ))
          )(any())
        }
      }
      "the life event service returns a error" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        doRequest() { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = MatcherEquals("lifeEventNotRequested"),
            path = MatcherEquals(s"/manager/$lisaManager/accounts/$accountId/events/annual-returns"),
            auditData = MatcherEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId" -> accountId,
              "eventType" -> "Statutory Submission",
              "eventDate" -> (json \ "eventDate").as[String],
              "lisaManagerName" -> (json \ "lisaManagerName").as[String],
              "taxYear" -> (json \ "taxYear").as[Int].toString,
              "marketValueCash" -> (json \ "marketValueCash").as[Int].toString,
              "marketValueStocksAndShares" -> (json \ "marketValueStocksAndShares").as[Int].toString,
              "annualSubsCash" -> (json \ "annualSubsCash").as[Int].toString,
              "annualSubsStocksAndShares" -> (json \ "annualSubsStocksAndShares").as[Int].toString,
              "reasonNotRequested" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
    }

  }

  private val acceptHeaderV1 = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  private val acceptHeaderV2 = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  private val lisaManager = "Z123456"
  private val accountId = "1234567890"
  private val json = Json.obj(
    "eventDate" -> "2018-04-05",
    "lisaManagerName" -> "ISA Manager 1",
    "taxYear" -> 2018,
    "marketValueCash" -> 0,
    "marketValueStocksAndShares" -> 56,
    "annualSubsCash" -> 0,
    "annualSubsStocksAndShares" -> 55
  )
  private val supersedeJson = json ++ Json.obj(
    "supersede" -> Json.obj(
      "originalLifeEventId" -> "1234567890",
      "originalEventDate" -> "2018-01-01"
    )
  )

  private def doRequest(lmrn: String = lisaManager, accountId: String = accountId, acceptHeader: (String, String) = acceptHeaderV2, payload: JsValue = json)
                       (callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.POST, "/")
    val res = SUT.submitReturn(lmrn, accountId).apply(req.withHeaders(acceptHeader).withBody(AnyContentAsJson(payload)))

    callback(res)
  }

  private val mockService: LifeEventService = mock[LifeEventService]
  private val mockAuditService: AuditService = mock[AuditService]
  private val mockAuthCon: AuthConnector = mock[AuthConnector]
  private val mockValidator: AnnualReturnValidator = mock[AnnualReturnValidator]
  val mockAppContext: AppContext = mock[AppContext]
  val mockLisaMetrics: LisaMetrics = mock[LisaMetrics]
  val mockControllerComponents = inject[ControllerComponents]
  val mockParser = inject[PlayBodyParsers]

  private val SUT = new AnnualReturnController(mockAuthCon, mockAppContext, mockService, mockAuditService, mockValidator, mockLisaMetrics, mockControllerComponents, mockParser) {
    override lazy val v2endpointsEnabled = true
  }

}
