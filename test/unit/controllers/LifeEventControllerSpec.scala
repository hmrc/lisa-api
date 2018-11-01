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
import uk.gov.hmrc.lisaapi.controllers.{ErrorBadRequestAccountId, ErrorBadRequestLmrn, LifeEventController}
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
  val accountId = "ABC/12345"
  val eventId = "1234567890"
  val validDate = "2017-04-06"
  val invalidDate = "2017-04-05"

  implicit val hc:HeaderCarrier = HeaderCarrier()

  val reportLifeEventJson =
    s"""
      |{
      |  "eventType" : "LISA Investor Terminal Ill Health",
      |  "eventDate" : "$validDate"
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
              "eventDate" -> validDate
            ))
          )(any())
        }
      }
    }

    "audit lifeEventNotReported" when {
      "the json fails date validation" in {
        doReportLifeEventRequest(reportLifeEventJson.replace(validDate, invalidDate)){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> invalidDate,
              "reasonNotReported" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
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
              "eventDate" -> validDate,
              "reasonNotReported" -> "LIFE_EVENT_INAPPROPRIATE"
            ))
          )(any())
        }
      }
      "the request results in a ReportLifeEventAlreadyExistsResponse" in {
        when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse))
        doReportLifeEventRequest(reportLifeEventJson){res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventType" -> "LISA Investor Terminal Ill Health",
              "eventDate" -> validDate,
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
              "eventDate" -> validDate,
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
              "eventDate" -> validDate,
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
        val invalidJson = reportLifeEventJson.replace(s"$validDate", DateTime.now.plusDays(1).toString("yyyy-MM-dd"))

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
      "given an invalid accountId in the url" in {
        doReportLifeEventRequest(reportLifeEventJson, accId = "1=2!") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }

    "return with 403 forbidden and a code of FORBIDDEN" when {
      "given a eventDate prior to 6 April 2017" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful((ReportLifeEventSuccessResponse("1928374"))))

        doReportLifeEventRequest(reportLifeEventJson.replace(validDate, "2017-04-05")) { res =>
          status(res) mustBe (FORBIDDEN)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The eventDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/eventDate"
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
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventAccountClosedOrVoidResponse)))
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
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (CONFLICT)
        (contentAsJson(res) \"code").as[String] mustBe ("LIFE_EVENT_ALREADY_EXISTS")
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

  def doReportLifeEventRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.PUT, "/")
    val res = SUT.reportLisaLifeEvent(lmrn, accId).apply(req.withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService: LifeEventService = mock[LifeEventService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new LifeEventController {
    override val service: LifeEventService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
  }

}
