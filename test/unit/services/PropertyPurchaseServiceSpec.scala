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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesLifeEventResponse}
import uk.gov.hmrc.lisaapi.services.PropertyPurchaseService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class PropertyPurchaseServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "Request a fund release" must {

    "return a success response" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesLifeEventResponse("AB123456")))

        doRequest{response => response mustBe RequestFundReleaseSuccessResponse("AB123456")}
      }
    }

    "return account closed" when {
      "given a account closed response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","")))

        doRequest{response => response mustBe RequestFundReleaseAccountClosedResponse}
      }
    }

    "return account cancelled" when {
      "given a account cancelled response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED","")))

        doRequest{response => response mustBe RequestFundReleaseAccountCancelledResponse}
      }
    }

    "return account void" when {
      "given a account void response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID","")))

        doRequest{response => response mustBe RequestFundReleaseAccountVoidResponse}
      }
    }

    "return account not found" when {
      "given a account void response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND","")))

        doRequest{response => response mustBe RequestFundReleaseAccountNotFoundResponse}
      }
    }

    "return life event already exists" when {
      "given a life event already exists response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_ALREADY_EXISTS","")))

        doRequest{response => response mustBe RequestFundReleaseLifeEventAlreadyExistsResponse}
      }
    }

    "return already superseded" when {
      "given a already superseded response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED","")))

        doRequest{response => response mustBe RequestFundReleaseLifeEventAlreadySupersededResponse}
      }
    }

    "return mismatch error" when {
      "given a mismatch response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_LIFE_EVENT_MISMATCH","")))

        doRequest{response => response mustBe RequestFundReleaseMismatchResponse}
      }
    }

    "return not open long enough error" when {
      "given a not open long enough response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH","")))

        doRequest{response => response mustBe RequestFundReleaseAccountNotOpenLongEnoughResponse}
      }
    }

    "return other purchase on record error" when {
      "given a other purchase on record response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD","")))

        doRequest{response => response mustBe RequestFundReleaseOtherPurchaseOnRecordResponse}
      }
    }

    "return a Internal Server Error response" when {
      "given a INTERNAL_SERVER_ERROR error code from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Error")))
        doRequest(response => response mustBe RequestFundReleaseErrorResponse)
      }

      "given a invalid error code response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVALID","Invalid Code")))
        doRequest(response => response mustBe RequestFundReleaseErrorResponse)
      }
    }

  }

  private def doRequest(callback: (RequestFundReleaseResponse) => Unit): Unit = {
    val request = InitialFundReleaseRequest(new DateTime("2018-01-01"), 10000, "CR12345", FundReleasePropertyDetails("1", "AA1 1AA"))
    val response = Await.result(SUT.requestFundRelease("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends PropertyPurchaseService {
    override val desConnector: DesConnector = mockDesConnector
  }
}