/*
 * Copyright 2023 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers.{eq => matchersEquals, _}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountAlreadyCancelled, ErrorAccountAlreadyClosed, ErrorAccountAlreadyVoided, ErrorBadRequestAccountId, ErrorBadRequestLmrn, LifeEventController}
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LifeEventControllerSpec extends ControllerTestFixture {

  val lifeEventController: LifeEventController = new LifeEventController(
    mockAuthConnector,
    mockAppContext,
    mockLifeEventService,
    mockAuditService,
    mockLisaMetrics,
    mockControllerComponents,
    mockParser
  ) {
    override lazy val v2endpointsEnabled = true
  }

  val acceptHeaderV1: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val acceptHeaderV2: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val lisaManager                      = "Z019283"
  val accountId                        = "ABC/12345"
  val validDate                        = "2017-04-06"
  val invalidDate                      = "2017-04-05"

  val reportLifeEventJson: String =
    s"""
      |{
      |  "eventType" : "LISA Investor Terminal Ill Health",
      |  "eventDate" : "$validDate"
      |}
    """.stripMargin

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  "Report Life Event" should {

    "audit lifeEventReported" when {
      "the request has been successful" in {
        when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doReportLifeEventRequest(reportLifeEventJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(
              Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountID"                  -> accountId,
                "eventType"                  -> "LISA Investor Terminal Ill Health",
                "eventDate"                  -> validDate
              )
            )
          )(any())
        }
      }
    }

    "audit lifeEventNotReported" when {
      "the json fails date validation" in {
        doReportLifeEventRequest(reportLifeEventJson.replace(validDate, invalidDate)) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(
              Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountID"                  -> accountId,
                "eventType"                  -> "LISA Investor Terminal Ill Health",
                "eventDate"                  -> invalidDate,
                "reasonNotReported"          -> "FORBIDDEN"
              )
            )
          )(any())
        }
      }
      "the request results in a ReportLifeEventInappropriateResponse" in {
        when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventInappropriateResponse))
        doReportLifeEventRequest(reportLifeEventJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(
              Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountID"                  -> accountId,
                "eventType"                  -> "LISA Investor Terminal Ill Health",
                "eventDate"                  -> validDate,
                "reasonNotReported"          -> "LIFE_EVENT_INAPPROPRIATE"
              )
            )
          )(any())
        }
      }
      "the request results in a ReportLifeEventAlreadyExistsResponse" in {
        when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("123")))
        doReportLifeEventRequest(reportLifeEventJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(
              Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountID"                  -> accountId,
                "eventType"                  -> "LISA Investor Terminal Ill Health",
                "eventDate"                  -> validDate,
                "reasonNotReported"          -> "LIFE_EVENT_ALREADY_EXISTS"
              )
            )
          )(any())
        }
      }
      "the request results in a ReportLifeEventAccountNotFoundResponse" in {
        when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
        doReportLifeEventRequest(reportLifeEventJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(
              Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountID"                  -> accountId,
                "eventType"                  -> "LISA Investor Terminal Ill Health",
                "eventDate"                  -> validDate,
                "reasonNotReported"          -> "INVESTOR_ACCOUNTID_NOT_FOUND"
              )
            )
          )(any())
        }
      }
      "the request results in a ReportLifeEventErrorResponse" in {
        when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventErrorResponse))

        doReportLifeEventRequest(reportLifeEventJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("lifeEventNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events"),
            auditData = matchersEquals(
              Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountID"                  -> accountId,
                "eventType"                  -> "LISA Investor Terminal Ill Health",
                "eventDate"                  -> validDate,
                "reasonNotReported"          -> "INTERNAL_SERVER_ERROR"
              )
            )
          )(any())
        }
      }
    }

    "return with 201 created" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe CREATED
        (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
      }
    }

    "return with 400 bad request and a code of BAD_REQUEST" when {
      "given an invalid event type" in {
        val invalidJson = reportLifeEventJson.replace("LISA Investor Terminal Ill Health", "Invalid Event Type")

        doReportLifeEventRequest(invalidJson) { res =>
          status(res) mustBe BAD_REQUEST
          (contentAsJson(res) \ "code").as[String] mustBe "BAD_REQUEST"
        }
      }
      "given a future eventDate" in {
        val invalidJson = reportLifeEventJson.replace(s"$validDate", DateTime.now.plusDays(1).toString("yyyy-MM-dd"))

        doReportLifeEventRequest(invalidJson) { res =>
          status(res) mustBe BAD_REQUEST
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
        when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

        doReportLifeEventRequest(reportLifeEventJson.replace(validDate, "2017-04-05")) { res =>
          status(res) mustBe FORBIDDEN
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The eventDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/eventDate"
        }
      }
    }

    "return with 403 forbidden and a code of LIFE_EVENT_INAPPROPRIATE" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventInappropriateResponse))
      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_INAPPROPRIATE"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID in version 1" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventAccountClosedOrVoidResponse))
      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID"
        (contentAsJson(res) \ "message")
          .as[String] mustBe "This LISA account has already been closed or been made void by HMRC"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED in version 2" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventAccountClosedResponse))
      doReportLifeEventRequest(reportLifeEventJson, acceptHeader = acceptHeaderV2) { res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyClosed.errorCode
        (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyClosed.message
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CANCELLED in version 2" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventAccountCancelledResponse))
      doReportLifeEventRequest(reportLifeEventJson, acceptHeader = acceptHeaderV2) { res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyCancelled.errorCode
        (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyCancelled.message
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID in version 2" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventAccountVoidResponse))
      doReportLifeEventRequest(reportLifeEventJson, acceptHeader = acceptHeaderV2) { res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountAlreadyVoided.errorCode
        (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountAlreadyVoided.message
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
      }
    }

    "return with 409 conflict and a code of LIFE_EVENT_ALREADY_EXISTS" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("123")))
      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe CONFLICT
        val json = contentAsJson(res)
        (json \ "code").as[String] mustBe "LIFE_EVENT_ALREADY_EXISTS"
        (json \ "lifeEventId").as[String] mustBe "123"
      }
    }

    "return with 500 internal server error when an unexpected response is returned" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventFundReleaseNotFoundResponse))
      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      }
    }

    "return with 503 service unavailable when a service unavailable response is returned" in {
      when(mockLifeEventService.reportLifeEvent(any(), any(), any())(any()))
        .thenReturn(Future.successful(ReportLifeEventServiceUnavailableResponse))

      doReportLifeEventRequest(reportLifeEventJson) { res =>
        status(res) mustBe SERVICE_UNAVAILABLE
        (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
      }
    }

  }

  def doReportLifeEventRequest(
    jsonString: String,
    lmrn: String = lisaManager,
    accId: String = accountId,
    acceptHeader: (String, String) = acceptHeaderV1
  )(callback: Future[Result] => Unit): Unit = {
    val req = FakeRequest(Helpers.PUT, "/")
    val res = lifeEventController
      .reportLisaLifeEvent(lmrn, accId)
      .apply(req.withHeaders(acceptHeader).withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }
}
