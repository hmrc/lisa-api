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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesLifeEventExistResponse, DesLifeEventResponse, DesLifeEventRetrievalResponse}
import uk.gov.hmrc.lisaapi.services.LifeEventService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import uk.gov.hmrc.http.HeaderCarrier

class LifeEventServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "Report life event" must {

    "return a Success response" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesLifeEventResponse("AB123456")))

        doRequest{response => response mustBe ReportLifeEventSuccessResponse("AB123456")}
      }
    }

    "return a Inappropriate Life Event response" when {
      "given DesFailureResponse and status 403" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_INAPPROPRIATE","The life Event was inappropriate")))
        doRequest(response => response mustBe ReportLifeEventInappropriateResponse)
      }
    }

    "return a Already Exists response" when {
      "given DesFailureResponse and status 409" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_ALREADY_EXISTS","The life Event Already Exists")))
        doRequest(response => response mustBe ReportLifeEventAlreadyExistsResponse)
      }
    }

    "return a Not Found response" when {
      "given DesFailureReponse and status 404" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND","The accountID given does not match with HMRC’s records")))
        doRequest(response => response mustBe ReportLifeEventAccountNotFoundResponse)
      }
    }

    "return a Internal Server Error response" when {
      "When INTERNAL_SERVER_ERROR sent" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Error")))
        doRequest(response => response mustBe ReportLifeEventErrorResponse)
      }

      "When Invalid Code Sent" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVALID","Invalid Code")))
        doRequest(response => response mustBe ReportLifeEventErrorResponse)
      }
    }

  }

  private def doRequest(callback: (ReportLifeEventResponse) => Unit): Unit = {
    val request = ReportLifeEventRequest("LISA Investor Terminal Ill Health", new DateTime("2017-04-06"))
    val response = Await.result(SUT.reportLifeEvent("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends LifeEventService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
