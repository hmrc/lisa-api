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
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesUnavailableResponse}
import uk.gov.hmrc.lisaapi.services.BonusOrWithdrawalService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class BonusOrWithdrawalServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "GET bonus or withdrawal" must {

    "return a success response" when {
      "given a successful response from the DES connector" in {
        val desResponse = GetWithdrawalResponse(
          periodStartDate = new DateTime("2018-01-06"),
          periodEndDate = new DateTime("2018-02-05"),
          automaticRecoveryAmount = Some(0),
          withdrawalAmount = 100,
          withdrawalChargeAmount = 25,
          withdrawalChargeAmountYtd = 0,
          fundsDeductedDuringWithdrawal = false,
          withdrawalReason = "Regular withdrawal",
          supersededBy = None,
          supersede = None,
          paymentStatus = "Collected",
          creationDate = new DateTime("2018-02-10")
        )

        when(mockDesConnector.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(desResponse))

        doRequest { response =>
          response mustBe desResponse
        }
      }
    }

    "return a transaction not found response" when {
      "given the code TRANSACTION_ID_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("TRANSACTION_ID_NOT_FOUND", "xxxx")))

        doRequest { response =>
          response mustBe GetBonusOrWithdrawalTransactionNotFoundResponse
        }
      }
    }

    "return a account not found response" when {
      "given the code INVESTOR_ACCOUNTID_NOT_FOUND from the DES connector" in {
        when(mockDesConnector.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND", "xxxx")))

        doRequest { response =>
          response mustBe GetBonusOrWithdrawalInvestorNotFoundResponse
        }
      }
    }

    "return a service unavailable response" when {
      "given a DesUnavailableResponse from the DES connector" in {
        when(mockDesConnector.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(DesUnavailableResponse))

        doRequest { response =>
          response mustBe GetBonusOrWithdrawalServiceUnavailableResponse
        }
      }
    }

    "return a generic error response" when {
      "given any other error code from the DES connector" in {
        when(mockDesConnector.getBonusOrWithdrawal(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("SOMETHING_ELSE", "xxxxx")))

        doRequest { response =>
          response mustBe GetBonusOrWithdrawalErrorResponse
        }
      }
    }

  }

  private def doRequest(callback: (GetBonusOrWithdrawalResponse) => Unit) = {
    val response = Await.result(SUT.getBonusOrWithdrawal("Z019283", "192837", "789")(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends BonusOrWithdrawalService {
    override val desConnector: DesConnector = mockDesConnector
  }

}
