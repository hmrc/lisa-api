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
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesCreateInvestorResponse, DesFailureResponse}
import uk.gov.hmrc.lisaapi.services.InvestorService

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import uk.gov.hmrc.http.HeaderCarrier

class InvestorServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "InvestorService" must {

    "return a Success Response" when {

      "given a investor id from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              201,

              DesCreateInvestorResponse(investorID = "AB123456")
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorSuccessResponse("AB123456")
        }
      }

    }

    "return an Error Response" when {
      "given an error response from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((500, DesFailureResponse("code1", "reason1"))
          )
          )

        doRequest{response =>
          response mustBe CreateLisaInvestorErrorResponse(500, DesFailureResponse("code1", "reason1"))
        }

      }
    }

    "return an Investor Already Exists Response" when {

      "given a 409 status and CreateInvestor response from DES" in {
        val investorID = "1234567890"

        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((409, DesCreateInvestorResponse(investorID))
            )
          )

        doRequest{response =>
          response mustBe CreateLisaInvestorAlreadyExistsResponse(investorID)
        }
      }

    }

  }

  private def doRequest(callback: (CreateLisaInvestorResponse) => Unit) = {
    val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createInvestor("Z019283", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends InvestorService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
