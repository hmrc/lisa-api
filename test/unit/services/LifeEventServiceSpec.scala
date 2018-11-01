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

    "return ReportLifeEventSuccessResponse" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesLifeEventResponse("AB123456")))

        doRequest{response => response mustBe ReportLifeEventSuccessResponse("AB123456")}
      }
    }

    "return ReportLifeEventInappropriateResponse" when {
      "the error code is LIFE_EVENT_INAPPROPRIATE" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_INAPPROPRIATE","The life Event was inappropriate")))
        doRequest(response => response mustBe ReportLifeEventInappropriateResponse)
      }
    }

    "return ReportLifeEventAccountClosedOrVoidResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID","")))
        doRequest(response => response mustBe ReportLifeEventAccountClosedOrVoidResponse)
      }
    }

    "return ReportLifeEventAccountClosedResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","")))
        doRequest(response => response mustBe ReportLifeEventAccountClosedResponse)
      }
    }

    "return ReportLifeEventAccountVoidResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_VOID" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID","")))
        doRequest(response => response mustBe ReportLifeEventAccountVoidResponse)
      }
    }

    "return ReportLifeEventAccountCancelledResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED","")))
        doRequest(response => response mustBe ReportLifeEventAccountCancelledResponse)
      }
    }

    "return ReportLifeEventAlreadyExistsResponse" when {
      "the error code is LIFE_EVENT_ALREADY_EXISTS" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_ALREADY_EXISTS","The life Event Already Exists")))
        doRequest(response => response mustBe ReportLifeEventAlreadyExistsResponse)
      }
    }

    "return ReportLifeEventAccountNotFoundResponse" when {
      "the error code is INVESTOR_ACCOUNTID_NOT_FOUND" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND","The accountID given does not match with HMRC’s records")))
        doRequest(response => response mustBe ReportLifeEventAccountNotFoundResponse)
      }
    }

    /* ********************************************* */

    "return ReportLifeEventAlreadySupersededResponse" when {
      "the error code is SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED","")))
        doRequest(response => response mustBe ReportLifeEventAlreadySupersededResponse)
      }
    }

    "return ReportLifeEventMismatchResponse" when {
      "the error code is SUPERSEDING_LIFE_EVENT_MISMATCH" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_LIFE_EVENT_MISMATCH","")))
        doRequest(response => response mustBe ReportLifeEventMismatchResponse)
      }
    }

    "return ReportLifeEventAccountNotOpenLongEnoughResponse" when {
      "the error code is COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH","")))
        doRequest(response => response mustBe ReportLifeEventAccountNotOpenLongEnoughResponse)
      }
    }

    "return ReportLifeEventOtherPurchaseOnRecordResponse" when {
      "the error code is COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD","")))
        doRequest(response => response mustBe ReportLifeEventOtherPurchaseOnRecordResponse)
      }
    }

    "return ReportLifeEventFundReleaseSupersededResponse" when {
      "the error code is FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED","")))
        doRequest(response => response mustBe ReportLifeEventFundReleaseSupersededResponse)
      }
    }

    "return ReportLifeEventFundReleaseNotFoundResponse" when {
      "the error code is FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND","")))
        doRequest(response => response mustBe ReportLifeEventFundReleaseNotFoundResponse)
      }
    }

    "return ReportLifeEventExtensionOneAlreadyApprovedResponse" when {
      "the error code is PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED","")))
        doRequest(response => response mustBe ReportLifeEventExtensionOneAlreadyApprovedResponse)
      }
    }

    "return ReportLifeEventExtensionTwoAlreadyApprovedResponse" when {
      "the error code is PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED","")))
        doRequest(response => response mustBe ReportLifeEventExtensionTwoAlreadyApprovedResponse)
      }
    }

    "return ReportLifeEventExtensionOneNotYetApprovedResponse" when {
      "the error code is PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED","")))
        doRequest(response => response mustBe ReportLifeEventExtensionOneNotYetApprovedResponse)
      }
    }


    /* ********************************************* */

    "return ReportLifeEventErrorResponse" when {
      "the error code doesn't match any of the previous values" in {
        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("XXX","Any other error condition")))
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
