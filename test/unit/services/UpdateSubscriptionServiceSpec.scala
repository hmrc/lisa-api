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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.services.{LifeEventService, UpdateSubscriptionService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UpdateSubscriptionServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "Update subscription event" must {

    "return a Success response" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesUpdateSubscriptionSuccessResponse("code","message")))

        doRequest{response => response mustBe UpdateSubscriptionSuccessResponse("code", "message")}
      }
    }


    "return a Not Found response" when {
      "given DesFailureReponse and status 404" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND","The accountID given does not match with HMRC’s records")))
        doRequest(response => response mustBe UpdateSubscriptionAccountNotFoundResponse)
      }
    }

    "return a Forbidden account closed response" when {
      "given DesFailureReponse and status 403" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","The LISA account is already closed.")))
        doRequest(response => response mustBe UpdateSubscriptionAccountClosedResponse)
      }
    }

    "return a Forbidden account voided response" when {
      "given DesFailureReponse and status 403" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID","The LISA account is already voided.")))
        doRequest(response => response mustBe UpdateSubscriptionAccountVoidedResponse)
      }
    }


    "return a Internal Server Error response" when {
      "When INTERNAL_SERVER_ERROR sent" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Error")))
        doRequest(response => response mustBe UpdateSubscriptionErrorResponse)
      }

      "When Invalid Code Sent" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVALID","Invalid Code")))
        doRequest(response => response mustBe UpdateSubscriptionErrorResponse)
      }
    }

  }



  private def doRequest(callback: (UpdateSubscriptionResponse) => Unit): Unit = {
    val request = UpdateSubscriptionRequest( new DateTime("2017-04-06"))
    val response = Await.result(SUT.updateSubscription("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends UpdateSubscriptionService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
