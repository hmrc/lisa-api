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

package unit.services

import helpers.ServiceTestFixture
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountNotFound, ErrorInternalServerError, ErrorLifeEventIdNotFound, ErrorResponse, ErrorServiceUnavailable}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.services.LifeEventService

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class LifeEventServiceSpec extends ServiceTestFixture {

  val lifeEventService: LifeEventService = new LifeEventService(mockDesConnector)

  "Report life event" must {

    "return ReportLifeEventSuccessResponse" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesLifeEventResponse("AB123456")))

        doPostRequest(response => response mustBe ReportLifeEventSuccessResponse("AB123456"))
      }
    }

    "return ReportLifeEventAlreadyExistsResponse" when {
      "the error code is LIFE_EVENT_ALREADY_EXISTS" in {
        val response = DesFailureResponse(
          "LIFE_EVENT_ALREADY_EXISTS",
          "The investor’s life event id 9999999979 has already been reported."
        )

        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(response))

        doPostRequest(response => response mustBe ReportLifeEventAlreadyExistsResponse("9999999979"))
      }
    }

    "return ReportLifeEventAlreadySupersededResponse" when {
      "the error code is SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" in {
        val response = DesFailureResponse(
          "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED",
          "The life event id 9999999989 has already been superseded."
        )

        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(response))

        doPostRequest(response => response mustBe ReportLifeEventAlreadySupersededResponse("9999999989"))
      }
    }

    "return ReportLifeEventInappropriateResponse" when {
      "the error code is LIFE_EVENT_INAPPROPRIATE" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(
          Future.successful(DesFailureResponse("LIFE_EVENT_INAPPROPRIATE", "The life Event was inappropriate"))
        )
        doPostRequest(response => response mustBe ReportLifeEventInappropriateResponse)
      }
    }

    "return ReportLifeEventAccountClosedOrVoidResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID", "")))
        doPostRequest(response => response mustBe ReportLifeEventAccountClosedOrVoidResponse)
      }
    }

    "return ReportLifeEventAccountClosedResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED", "")))
        doPostRequest(response => response mustBe ReportLifeEventAccountClosedResponse)
      }
    }

    "return ReportLifeEventAccountVoidResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_VOID" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID", "")))
        doPostRequest(response => response mustBe ReportLifeEventAccountVoidResponse)
      }
    }

    "return ReportLifeEventAccountCancelledResponse" when {
      "the error code is INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED", "")))
        doPostRequest(response => response mustBe ReportLifeEventAccountCancelledResponse)
      }
    }

    "return ReportLifeEventAccountNotFoundResponse" when {
      "the error code is INVESTOR_ACCOUNTID_NOT_FOUND" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND", "The accountID given does not match with HMRC’s records")
          )
        )
        doPostRequest(response => response mustBe ReportLifeEventAccountNotFoundResponse)
      }
    }

    "return ReportLifeEventMismatchResponse" when {
      "the error code is SUPERSEDING_LIFE_EVENT_MISMATCH" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_LIFE_EVENT_MISMATCH", "")))
        doPostRequest(response => response mustBe ReportLifeEventMismatchResponse)
      }
    }

    "return ReportLifeEventAccountNotOpenLongEnoughResponse" when {
      "the error code is COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH", "")))
        doPostRequest(response => response mustBe ReportLifeEventAccountNotOpenLongEnoughResponse)
      }
    }

    "return ReportLifeEventOtherPurchaseOnRecordResponse" when {
      "the error code is COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD", "")))
        doPostRequest(response => response mustBe ReportLifeEventOtherPurchaseOnRecordResponse)
      }
    }

    "return ReportLifeEventFundReleaseSupersededResponse" when {
      "the error code is FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesFailureResponse(
              "FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED",
              "The fund release life event id 9999999992 in the request has been superseded."
            )
          )
        )
        doPostRequest(response => response mustBe ReportLifeEventFundReleaseSupersededResponse("9999999992"))
      }
    }

    "return ReportLifeEventFundReleaseNotFoundResponse" when {
      "the error code is FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND", "")))
        doPostRequest(response => response mustBe ReportLifeEventFundReleaseNotFoundResponse)
      }
    }

    "return ReportLifeEventExtensionOneAlreadyApprovedResponse" when {
      "the error code is PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesFailureResponse(
              "PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED",
              "Extension 1 life event 9999999999 has already been recorded for this account."
            )
          )
        )
        doPostRequest(response => response mustBe ReportLifeEventExtensionOneAlreadyApprovedResponse("9999999999"))
      }
    }

    "return ReportLifeEventExtensionTwoAlreadyApprovedResponse" when {
      "the error code is PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any())).thenReturn(
          Future.successful(
            DesFailureResponse(
              "PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED",
              "Extension 2 life event 9999999991 has already been recorded for this account."
            )
          )
        )
        doPostRequest(response => response mustBe ReportLifeEventExtensionTwoAlreadyApprovedResponse("9999999991"))
      }
    }

    "return ReportLifeEventExtensionOneNotYetApprovedResponse" when {
      "the error code is PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED", "")))
        doPostRequest(response => response mustBe ReportLifeEventExtensionOneNotYetApprovedResponse)
      }
    }

    "return ReportLifeEventServiceUnavailableResponse" when {
      "a DesUnavailableResponse is received" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))
        doPostRequest(response => response mustBe ReportLifeEventServiceUnavailableResponse)
      }
    }

    "return ReportLifeEventErrorResponse" when {
      "the error code doesn't match any of the previous values" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("XXX")))
        doPostRequest(response => response mustBe ReportLifeEventErrorResponse)
      }
    }

    "return ReportLifeEventInvalidPayload" when {
      "the DES error code returned as INVALID_PAYLOAD" in {
        when(mockDesConnector.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(DesFailureResponse("INVALID_PAYLOAD", "JZ")))
        doPostRequest(response => response mustBe ReportLifeEventInvalidPayload)
      }
    }
  }

  "Get life event" must {

    "return a Left with a appropriate ErrorResponse" when {

      "a INVESTOR_ACCOUNT_ID_NOT_FOUND error code is returned" in {
        when(mockDesConnector.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(DesFailureResponse("INVESTOR_ACCOUNT_ID_NOT_FOUND"))))

        doGetRequest(_ mustBe Left(ErrorAccountNotFound))
      }

      "a LIFE_EVENT_ID_NOT_FOUND error code is returned" in {
        when(mockDesConnector.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(DesFailureResponse("LIFE_EVENT_ID_NOT_FOUND"))))

        doGetRequest(_ mustBe Left(ErrorLifeEventIdNotFound))
      }

      "a DesUnavailableResponse is returned" in {
        when(mockDesConnector.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(DesUnavailableResponse)))

        doGetRequest(_ mustBe Left(ErrorServiceUnavailable))
      }

      "any other error code is returned" in {
        when(mockDesConnector.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(DesFailureResponse("ERROR"))))

        doGetRequest(_ mustBe Left(ErrorInternalServerError))
      }

    }

    "return a Right with a Seq GetLifeEventItem" when {

      "a Seq GetLifeEventItem is returned" in {
        val success = List(GetLifeEventItem("12345", "STATUTORY_SUBMISSION", LocalDate.parse("2018-01-01")))

        when(mockDesConnector.getLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(Right(success)))
        doGetRequest {
          _ mustBe Right(success)
        }
      }
    }

  }

  private def doPostRequest(callback: ReportLifeEventResponse => Unit): Unit = {
    val request  = ReportLifeEventRequest("LISA Investor Terminal Ill Health", LocalDate.parse("2017-04-06"))
    val response =
      Await.result(lifeEventService.reportLifeEvent("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  private def doGetRequest(callback: Either[ErrorResponse, Seq[GetLifeEventItem]] => Unit): Unit = {
    val response =
      Await.result(lifeEventService.getLifeEvent("Z019283", "192837", "5581145645")(HeaderCarrier()), Duration.Inf)

    callback(response)
  }
}
