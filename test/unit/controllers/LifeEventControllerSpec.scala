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
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.LifeEventController
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.LifeEventService

import scala.concurrent.Future

/**
  * Created by mark on 20/03/17.
  */
case object ReportTest extends ReportLifeEventResponse

class LifeEventControllerSpec  extends PlaySpec with MockitoSugar with OneAppPerSuite {


  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"

  val reportLifeEventJson =
    """
      |{
      |  "eventType" : "House Purchase",
      |  "eventDate" : "2001-01-01"
      |}
    """.stripMargin



  "The Life Event Controller" should {

    "return with status 201 created" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventSuccessResponse("1928374"))))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (CREATED)
        (contentAsJson(res) \"data" \ "lifeEventId").as[String] mustBe ("1928374")
      }
    }

    "return with status 400 bad request and a code of BAD_REQUEST" in {
      val invalidJson =    """
                             |{
                             |  "accountId" : "1234567890",
                             |  "lisaManagerReferenceNumber" : "Z543210",
                             |  "eventType" : "LISA Wanted to Come Out",
                             |  "eventDate" : "2001-01-06"
                             |}
                           """.stripMargin

      doReportLifeEventRequest(invalidJson) { res =>
        status(res) mustBe (BAD_REQUEST)
        (contentAsJson(res) \ "code").as[String] mustBe ("BAD_REQUEST")
      }
    }

    "return with 403 forbidden and a code of LIFE_EVENT_INAPPROPRIATE" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventInappropriateResponse)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (FORBIDDEN)
        (contentAsJson(res) \"code").as[String] mustBe ("LIFE_EVENT_INAPPROPRIATE")
      }
    }

    "return with 404 Notfound and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventAccountNotFoundResponse)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (NOT_FOUND)
        (contentAsJson(res) \"code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
      }
    }

    "return with 409 Conflict and a code of LIFE_EVENT_ALREADY_EXISTS" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventAlreadyExistsResponse)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (CONFLICT)
        (contentAsJson(res) \"code").as[String] mustBe ("LIFE_EVENT_ALREADY_EXISTS")
      }
    }

    "return with InternalServer Error when the Wrong event is returned" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportTest)))
      doReportLifeEventRequest(reportLifeEventJson){res =>
        status(res) mustBe (INTERNAL_SERVER_ERROR)
      }
    }

    "return with BadRequest when invalid future date for LISA Investor Terminal Ill Health" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportTest)))

      val reportLifeEventJsonFutureDateDeath =
        """
          |{
          |  "eventType" : "LISA Investor Terminal Ill Health",
          |  "eventDate" : "2017-04-06"
          |}
        """.stripMargin
      val futureDate = DateTime.now().plusDays(1).toString("yyyy-MM-dd")
      val req = reportLifeEventJsonFutureDateDeath.replace("2017-04-06", futureDate)
      doReportLifeEventRequest(req){res =>
        status(res) mustBe (BAD_REQUEST)
        (contentAsJson(res) \"code").as[String] mustBe ("BAD_REQUEST")
      }

    }

    "return with BadRequest when invalid future date for LISA Investor Death" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportTest)))

      val reportLifeEventJsonFutureDateDeath =
        """
          |{
          |  "eventType" : "LISA Investor Death",
          |  "eventDate" : "2017-04-06"
          |}
        """.stripMargin

      val futureDate = DateTime.now().plusDays(1).toString("yyyy-MM-dd")
      val req = reportLifeEventJsonFutureDateDeath.replace("2017-04-06", futureDate)
      doReportLifeEventRequest(req){res =>
        status(res) mustBe (BAD_REQUEST)
        (contentAsJson(res) \"code").as[String] mustBe ("BAD_REQUEST")
      }

    }
  }

  "return with Created when valid date for LISA Investor Terminal Ill Health" in {
    when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventSuccessResponse("1928374"))))

    val reportLifeEventJsonFutureDateDeath =
      """
        |{
        |  "eventType" : "LISA Investor Terminal Ill Health",
        |  "eventDate" : "2001-01-01"
        |}
      """.stripMargin
    doReportLifeEventRequest(reportLifeEventJsonFutureDateDeath){res =>
      status(res) mustBe (CREATED)
      (contentAsJson(res) \"data" \ "lifeEventId").as[String] mustBe ("1928374")
    }

  }

  "return with Created when valid date for LISA Investor Death" in {
    when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((ReportLifeEventSuccessResponse("1928374"))))

    val reportLifeEventJsonFutureDateDeath =
      """
        |{
        |  "eventType" : "LISA Investor Death",
        |  "eventDate" : "2001-01-01"
        |}
      """.stripMargin
    doReportLifeEventRequest(reportLifeEventJsonFutureDateDeath){res =>
      status(res) mustBe (CREATED)
      (contentAsJson(res) \"data" \ "lifeEventId").as[String] mustBe ("1928374")
    }

  }

  def doReportLifeEventRequest(jsonString: String)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.reportLisaLifeEvent(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }


  val mockService = mock[LifeEventService]
  val SUT = new LifeEventController {
    override val service: LifeEventService = mockService
  }

}
