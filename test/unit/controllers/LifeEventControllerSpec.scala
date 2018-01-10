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
import org.mockito.Matchers.{eq => matchersEquals, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{ErrorBadRequestLmrn, LifeEventController}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

case object ReportTest extends ReportLifeEventResponse

class LifeEventControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"
  val eventId = "1234567890"

  implicit val hc:HeaderCarrier = HeaderCarrier()

  val reportLifeEventJson =
    """
      |{
      |  "eventType" : "LISA Investor Terminal Ill Health",
      |  "eventDate" : "2017-01-01"
      |}
    """.stripMargin

  before {
    reset(mockAuditService)
    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  "Report Life Event" should {

    "audit lifeEventReported" when {
      "the request has been successful" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventSuccessResponse("1928374"))))
        doReportLifeEventRequest(reportLifeEventJson){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> "2017-01-01"
            ))
          )(any())
        }
      }
    }

    "audit lifeEventNotReported" when {
      "the request results in a ReportLifeEventInappropriateResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventInappropriateResponse))
        doReportLifeEventRequest(reportLifeEventJson){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> "2017-01-01",
              "reasonNotReported" -> "LIFE_EVENT_INAPPROPRIATE"
            ))
          )(any())
        }
      }
      "the request results in a ReportLifeEventAlreadyExistsResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("1234567890")))
        doReportLifeEventRequest(reportLifeEventJson){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> "2017-01-01",
              "reasonNotReported" -> "LIFE_EVENT_ALREADY_EXISTS"
            ))
          )(any())
        }
      }
      "the request results in a ReportLifeEventAccountNotFoundResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
        doReportLifeEventRequest(reportLifeEventJson){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> "2017-01-01",
              "reasonNotReported" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            ))
          )(any())
        }
      }
      "the request results in a ReportLifeEventErrorResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any()))
          .thenReturn(Future.successful(ReportLifeEventErrorResponse))

        doReportLifeEventRequest(reportLifeEventJson){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> "2017-01-01",
              "reasonNotReported" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
    }

    "return with 201 created" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventSuccessResponse("1928374"))))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (CREATED)
        (contentAsJson(res) \"data" \ "lifeEventId").as[String] mustBe ("1928374")
      }
    }

    "return with 400 bad request and a code of BAD_REQUEST" when {
      "given an invalid event type" in {
        val invalidJson = reportLifeEventJson.replace("LISA Investor Terminal Ill Health", "Invalid Event Type")

        doReportLifeEventRequest(invalidJson) { res =>
          status(res) mustBe (BAD_REQUEST)
          (contentAsJson(res) \ "code").as[String] mustBe "BAD_REQUEST"
        }
      }
      "given a future eventDate" in {
        val invalidJson = reportLifeEventJson.replace("2017-01-01", DateTime.now.plusDays(1).toString("yyyy-MM-dd"))

        doReportLifeEventRequest(invalidJson) { res =>
          status(res) mustBe (BAD_REQUEST)
          (contentAsJson(res) \ "code").as[String] mustBe "BAD_REQUEST"
        }
      }
      "given an invalid lmrn in the url" in {
        doReportLifeEventRequest(reportLifeEventJson, "Z111") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
    }

    "return with 403 forbidden and a code of LIFE_EVENT_INAPPROPRIATE" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventInappropriateResponse)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (FORBIDDEN)
        (contentAsJson(res) \"code").as[String] mustBe ("LIFE_EVENT_INAPPROPRIATE")
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventAccountClosedResponse)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (FORBIDDEN)
        (contentAsJson(res) \"code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID")
        (contentAsJson(res) \"message").as[String] mustBe ("This LISA account has already been closed or been made void by HMRC")
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventAccountNotFoundResponse)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (NOT_FOUND)
        (contentAsJson(res) \"code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
      }
    }

    "return with 409 conflict and a code of LIFE_EVENT_ALREADY_EXISTS" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("1234567890")))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (CONFLICT)
        (contentAsJson(res) \"code").as[String] mustBe ("LIFE_EVENT_ALREADY_EXISTS")
        (contentAsJson(res) \"lifeEventId").as[String] mustBe ("1234567890")
      }
    }

    "return with 500 internal server error when the wrong event is returned" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportTest))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      }
    }

  }

  "Get Life Event" should {

    "return with 200 ok" in {
      val successResponse = RequestLifeEventSuccessResponse(eventId, "LISA Investor Terminal Ill Health", new DateTime("2000-01-01"))
      when(mockService.getLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(successResponse))
      doGetLifeEventRequest{ res =>
        status(res) mustBe OK
        val json = contentAsJson(res)

        (json \ "lifeEventId").as[String] mustBe eventId
        (json \ "eventType").as[String] mustBe "LISA Investor Terminal Ill Health"
        (json \ "eventDate").as[String] mustBe "2000-01-01"
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.getLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
      doGetLifeEventRequest{ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
      }
    }

    "return with 404 not found and a code of LIFE_EVENT_NOT_FOUND" in {
      when(mockService.getLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventIdNotFoundResponse))
      doGetLifeEventRequest{ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_NOT_FOUND"
      }
    }

    "return with 500 internal server error when the wrong event is returned" in {
      when(mockService.getLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportTest))
      doGetLifeEventRequest{ res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      }
    }

  }

  def doReportLifeEventRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.PUT, "/")
    val res = SUT.reportLisaLifeEvent(lmrn, accountId).apply(req.withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doGetLifeEventRequest(callback: (Future[Result]) => Unit): Unit = {
    val res = SUT.getLifeEvent(lisaManager, accountId, eventId).apply(FakeRequest().withHeaders(acceptHeader))

    callback(res)
  }

  val mockService: LifeEventService = mock[LifeEventService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon :LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new LifeEventController {
    override val service: LifeEventService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
  }

}
