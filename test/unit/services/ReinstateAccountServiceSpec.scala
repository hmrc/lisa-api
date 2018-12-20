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
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.lisaapi.services.{AccountService, ReinstateAccountService}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

// scalastyle:off multiple.string.literals
class ReinstateAccountServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {


  "Reinstate Account" must {

    "return a Success Response" when {

      "given no rds code and an account id" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              DesReinstateAccountSuccessResponse("code", "reason")
            ))
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountSuccessResponse ("code", "reason")
        }
      }

    }

    "return the type-appropriate error response" when {

      "given a DesUnavailableResponse" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountServiceUnavailableResponse
        }

      }

      "given failureResponse for a Account Already Closed Response" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CLOSED")
            )
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountAlreadyClosedResponse
        }
      }

      "given failureResponse for a Account already cancelled" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_CANCELLED")
            )
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountAlreadyCancelledResponse
        }
      }

      "given failureResponse for a Account already opened" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_ACCOUNT_ALREADY_OPEN")
            )
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountAlreadyOpenResponse
        }
      }

      "given failureResponse for a Compliance check failure" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful(
              DesFailureResponse("INVESTOR_COMPLIANCE_CHECK_FAILED")
            )
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountInvestorComplianceCheckFailedResponse
        }
      }


      "given failureResponse for a Account Not Found Response" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful(DesFailureResponse("INVESTOR_ACCOUNTID_NOT_FOUND"))
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountNotFoundResponse
        }
      }


      "given failureResponse for any other error" in {
        when(mockDesConnector.reinstateAccount(any(), any())(any()))
          .thenReturn(
            Future.successful(DesFailureResponse("ERROR1234"))
          )

        doReinstateRequest { response =>
          response mustBe ReinstateLisaAccountErrorResponse
        }
      }
    }

  }



  private def doReinstateRequest(callback: (ReinstateLisaAccountResponse) => Unit) = {
    val response = Await.result(SUT.reinstateAccountService(testLMRN, "A123456")(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val testLMRN = "Z019283"

  val mockDesConnector: DesConnector = mock[DesConnector]

  object SUT extends ReinstateAccountService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
