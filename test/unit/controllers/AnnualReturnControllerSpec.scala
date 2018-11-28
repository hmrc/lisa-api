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

import org.mockito.Matchers.any
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{AnyContentAsEmpty, AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{AnnualReturnController, ErrorBadRequest, ErrorBadRequestAccountId, ErrorBadRequestLmrn, ErrorForbidden, ErrorInternalServerError, ErrorValidation}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AnnualReturnControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter
  with LisaConstants {

  val acceptHeaderV1 = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val acceptHeaderV2 = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")

  before {
    reset(mockAuditService)
    reset(mockValidator)

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockValidator.validate(any())).thenReturn(Nil)
  }

  "Submit annual return" must {

    // TODO: Auditing

    "return 201 created" when {
      "a success response is received from des" in {
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
      "a success response is received from des for a superseded life event" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

        val payload = json ++ Json.obj(
          "supersede" -> Json.obj(
            "originalLifeEventId" -> "1234567890",
            "originalEventDate" -> "2018-01-01"
          )
        )

        doRequest(payload = payload){ res =>
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
        val errors = List(ErrorValidation(
          DATE_ERROR,
          "The taxYear cannot be before 2017",
          Some("/taxYear")
        ))
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
      "a ReportLifeEventAccountVoidResponse is returned from DES" in {
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
      "a ReportLifeEventAccountCancelledResponse is returned from DES" in {
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
      "a ReportLifeEventMismatchResponse is returned from DES" in {
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
      "a ReportLifeEventAlreadySupersededResponse is returned from DES" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAlreadySupersededResponse))

        doRequest(){ res =>
          status(res) mustBe FORBIDDEN
          contentAsJson(res) mustBe Json.obj(
            "code" -> "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED",
            "message" -> "This life event has already been superseded"
          )
        }
      }
    }

    "return 404 not found" when {
      "a ReportLifeEventAccountNotFoundResponse is returned from DES" in {
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
      "a ReportLifeEventAlreadyExistsResponse is returned from DES" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse))

        doRequest(){ res =>
          status(res) mustBe CONFLICT
          contentAsJson(res) mustBe Json.obj(
            "code" -> "LIFE_EVENT_ALREADY_EXISTS",
            "message" -> "The investor’s life event has already been reported"
          )
        }
      }
    }

    "return 500 internal server error" when {
      "the life event service returns an error" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.failed(new RuntimeException("Test")))

        doRequest() { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }
      "the life event service returns an unexpected response" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).
          thenReturn(Future.successful(ReportLifeEventFundReleaseNotFoundResponse))

        doRequest() { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          contentAsJson(res) mustBe Json.toJson(ErrorInternalServerError)
        }
      }
    }

  }

  val json = Json.obj(
    "eventDate" -> "2018-04-05",
    "isaManagerName" -> "ISA Manager 1",
    "taxYear" -> 2018,
    "marketValueCash" -> 0,
    "marketValueStocksAndShares" -> 56,
    "annualSubsCash" -> 0,
    "annualSubsStocksAndShares" -> 55
  )

  private def doRequest(lmrn: String = "Z123456", accountId: String = "1234567890", acceptHeader: (String, String) = acceptHeaderV2, payload: JsValue = json)
                       (callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.POST, "/")
    val res = SUT.submitReturn(lmrn, accountId).apply(req.withHeaders(acceptHeader).withBody(AnyContentAsJson(payload)))

    callback(res)
  }

  val mockService: LifeEventService = mock[LifeEventService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val mockValidator: AnnualReturnValidator = mock[AnnualReturnValidator]

  val SUT = new AnnualReturnController {
    override val authConnector = mockAuthCon
    override lazy val v2endpointsEnabled = true
    override val service: LifeEventService = mockService
    override val auditService: AuditService = mockAuditService
    override val validator: AnnualReturnValidator = mockValidator
  }

}
