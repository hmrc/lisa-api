/*
 * Copyright 2022 HM Revenue & Customs
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

import helpers.ServiceTestFixture
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.services.UpdateSubscriptionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class UpdateSubscriptionServiceSpec extends ServiceTestFixture {

  val updateSubscriptionService: UpdateSubscriptionService = new UpdateSubscriptionService(mockDesConnector)

  "Update subscription event" must {

    "return a Success response" when {
      "given a success response with code SUCCESS from the DES connector" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesUpdateSubscriptionSuccessResponse("SUCCESS","message")))

        doRequest{response => response mustBe UpdateSubscriptionSuccessResponse("UPDATED",
          "Successfully updated the firstSubscriptionDate for the LISA account")}
      }
      "given a success response with code INVESTOR_ACCOUNT_NOW_VOID from the DES connector" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesUpdateSubscriptionSuccessResponse("INVESTOR_ACCOUNT_NOW_VOID","message")))

        doRequest{response => response mustBe UpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_VOID",
          "Successfully updated the firstSubscriptionDate for the LISA account and changed the account status to void because the investor has another account with an earlier firstSubscriptionDate")}
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
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","The LISA account is already closed")))
        doRequest(response => response mustBe UpdateSubscriptionAccountClosedResponse)
      }
    }

    "return a Forbidden account cancelled response" when {
      "given DesFailureReponse and status 403 for cancelled account" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED","The LISA account is already cancelled")))
        doRequest(response => response mustBe UpdateSubscriptionAccountCancelledResponse)
      }
    }

    "return a Forbidden account voided response" when {
      "given DesFailureReponse and status 403" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID","The LISA account is already voided")))
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

    "return a Service Unavailable response" when {
      "a DesUnavailableResponse is returned" in {
        when(mockDesConnector.updateFirstSubDate(any(), any(),any())(any())).
          thenReturn(Future.successful(DesUnavailableResponse))

        doRequest(response => response mustBe UpdateSubscriptionServiceUnavailableResponse)
      }
    }

  }

  private def doRequest(callback: (UpdateSubscriptionResponse) => Unit): Unit = {
    val request = UpdateSubscriptionRequest( new DateTime("2017-04-06"))
    val response = Await.result(updateSubscriptionService.updateSubscription("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }
}
