/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesCreateInvestorResponse, DesFailureResponse, DesUnavailableResponse}
import uk.gov.hmrc.lisaapi.services.InvestorService

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import uk.gov.hmrc.http.HeaderCarrier

class InvestorServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "InvestorService" must {

    "return a Success Response" when {
      "given a success response from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful(CreateLisaInvestorSuccessResponse(investorId = investorId))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorSuccessResponse(investorId)
        }
      }
    }

    "return an Investor Already Exists Response" when {

      "given a already exists response from the DES connector" in {

        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(Future.successful(CreateLisaInvestorAlreadyExistsResponse(investorId)))

        doRequest{response =>
          response mustBe CreateLisaInvestorAlreadyExistsResponse(investorId)
        }
      }

    }

    "return an Investor Not Found response" when {

      "given a investor not found response from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful(DesFailureResponse("INVESTOR_NOT_FOUND"))
          )

        doRequest{response =>
          response mustBe CreateLisaInvestorInvestorNotFoundResponse
        }
      }

    }

    "return a Service Unavailable Response" when {
      "given any other response from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(Future.successful(DesUnavailableResponse))

        doRequest{response =>
          response mustBe CreateLisaInvestorServiceUnavailableResponse
        }
      }
    }

    "return an Error Response" when {
      "given any other response from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful(DesFailureResponse("INVALID_PAYLOAD", "Submission has not passed validation."))
          )

        doRequest{response =>
          response mustBe CreateLisaInvestorErrorResponse
        }
      }
    }

  }

  private def doRequest(callback: (CreateLisaInvestorResponse) => Unit) = {
    val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createInvestor("Z019283", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val investorId = "1234567890"
  val mockDesConnector = mock[DesConnector]
  object SUT extends InvestorService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
