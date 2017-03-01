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
import uk.gov.hmrc.lisaapi.models.des.DesCreateInvestorResponse
import uk.gov.hmrc.lisaapi.services.InvestorService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class InvestorServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "InvestorService" must {

    "return a Success Response" when {

      "given a investor id from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateInvestorResponse(investorId = Some("AB123456")))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorSuccessResponse("AB123456")
        }
      }

    }

    "return a Not Found Response" when {

      "given an RDS code of 63214 from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateInvestorResponse(rdsCode = Some(63214)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorNotFoundResponse
        }
      }

    }

    "return a Already Exists Response" when {

      "given an RDS code of 63215 and an investor ID from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateInvestorResponse(rdsCode = Some(63215), investorId = Some("AB1234")))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorAlreadyExistsResponse("AB1234")
        }
      }

    }

    "return a Error Response" when {

      "given no data from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              None
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorErrorResponse
        }
      }

      "given an empty data object from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateInvestorResponse())
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorErrorResponse
        }
      }

      "given a status code other than 200" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              201,
              Some(DesCreateInvestorResponse(investorId = Some("AB123456")))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorErrorResponse
        }
      }

      "given an RDS code other than the ones for Not Found and Already Exists" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateInvestorResponse(rdsCode = Some(63216)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaInvestorErrorResponse
        }
      }

      "given an RDS code for Already Exists, but no investor ID" in {
        when(mockDesConnector.createInvestor(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateInvestorResponse(rdsCode = Some(63215), investorId = None))
            ))
          )

        doRequest { response =>
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

  val mockDesConnector = mock[DesConnector]
  object SUT extends InvestorService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
