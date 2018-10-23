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
import uk.gov.hmrc.lisaapi.models
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

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseSuccessResponse("AB123456")}
      }
    }

    "return account closed" when {
      "given a account closed response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseAccountClosedResponse}
      }
    }

    "return account cancelled" when {
      "given a account cancelled response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseAccountCancelledResponse}
      }
    }

    "return account void" when {
      "given a account void response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseAccountVoidResponse}
      }
    }

    "return account not found" when {
      "given a account void response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseAccountNotFoundResponse}
      }
    }

    "return life event already exists" when {
      "given a life event already exists response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_ALREADY_EXISTS","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseLifeEventAlreadyExistsResponse}
      }
    }

    "return already superseded" when {
      "given a already superseded response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseLifeEventAlreadySupersededResponse}
      }
    }

    "return mismatch error" when {
      "given a mismatch response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_LIFE_EVENT_MISMATCH","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseMismatchResponse}
      }
    }

    "return not open long enough error" when {
      "given a not open long enough response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseAccountNotOpenLongEnoughResponse}
      }
    }

    "return other purchase on record error" when {
      "given a other purchase on record response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD","")))

        doFundReleaseRequest{ response => response mustBe PropertyPurchaseOtherPurchaseOnRecordResponse}
      }
    }

    "return internal server error" when {
      "given a INTERNAL_SERVER_ERROR error code from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Error")))
        doFundReleaseRequest(response => response mustBe PropertyPurchaseErrorResponse)
      }

      "given a invalid error code response from the DES connector" in {
        when(mockDesConnector.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVALID","Invalid Code")))
        doFundReleaseRequest(response => response mustBe PropertyPurchaseErrorResponse)
      }
    }

  }

  "Request a purchase extension" must {

    "return a success response" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesLifeEventResponse("AB123456")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseSuccessResponse("AB123456")}
      }
    }

    "return account closed" when {
      "given a account closed response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseAccountClosedResponse}
      }
    }

    "return account cancelled" when {
      "given a account cancelled response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseAccountCancelledResponse}
      }
    }

    "return account void" when {
      "given a account void response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_VOID","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseAccountVoidResponse}
      }
    }

    "return account not found" when {
      "given a account void response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseAccountNotFoundResponse}
      }
    }

    "return life event already exists" when {
      "given a life event already exists response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("LIFE_EVENT_ALREADY_EXISTS","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseLifeEventAlreadyExistsResponse}
      }
    }

    "return already superseded" when {
      "given a already superseded response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseLifeEventAlreadySupersededResponse}
      }
    }

    "return mismatch error" when {
      "given a mismatch response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("SUPERSEDING_LIFE_EVENT_MISMATCH","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseMismatchResponse}
      }
    }

    "return extension one already approved error" when {
      "given a extension one already approved response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseExtensionOneAlreadyApprovedResponse}
      }
    }

    "return extension two already approved error" when {
      "given a extension two already approved response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseExtensionTwoAlreadyApprovedResponse}
      }
    }

    "return extension one not yet approved error" when {
      "given a extension one not yet approved response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseExtensionOneNotYetApprovedResponse}
      }
    }

    "return fund release not found error" when {
      "given a fund release not found response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseFundReleaseNotFoundResponse}
      }
    }

    "return fund release superseded error" when {
      "given a fund release not found response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED","")))

        doExtensionRequest{ response => response mustBe PropertyPurchaseFundReleaseSupersededResponse}
      }
    }

    "return internal server error" when {
      "given a INTERNAL_SERVER_ERROR error code from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Error")))
        doExtensionRequest(response => response mustBe PropertyPurchaseErrorResponse)
      }

      "given a invalid error code response from the DES connector" in {
        when(mockDesConnector.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(DesFailureResponse("INVALID","Invalid Code")))
        doExtensionRequest(response => response mustBe PropertyPurchaseErrorResponse)
      }
    }

  }

  private def doFundReleaseRequest(callback: (PropertyPurchaseResponse) => Unit): Unit = {
    val request = InitialFundReleaseRequest(new DateTime("2018-01-01"), 10000, "CR12345", FundReleasePropertyDetails("1", "AA1 1AA"))
    val response = Await.result(SUT.requestFundRelease("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  private def doExtensionRequest(callback: (PropertyPurchaseResponse) => Unit): Unit = {
    val request = RequestStandardPurchaseExtension("1234567890", new DateTime("2018-01-01"), "Extension one")
    val response = Await.result(SUT.requestPurchaseExtension("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends PropertyPurchaseService {
    override val desConnector: DesConnector = mockDesConnector
  }
}