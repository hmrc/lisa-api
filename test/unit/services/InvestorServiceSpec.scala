/*
 * Copyright 2021 HM Revenue & Customs
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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesUnavailableResponse}
import uk.gov.hmrc.lisaapi.services.InvestorService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class InvestorServiceSpec extends ServiceTestFixture {

  val investorService: InvestorService = new InvestorService(mockDesConnector)


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
    val response = Await.result(investorService.createInvestor("Z019283", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val investorId = "1234567890"
}
