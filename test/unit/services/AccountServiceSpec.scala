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
import uk.gov.hmrc.lisaapi.models.des.{DesCreateAccountResponse, DesCreateInvestorResponse}
import uk.gov.hmrc.lisaapi.services.{AccountService, InvestorService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class AccountServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  val httpStatusOk = 200
  val httpStatusCreated = 201
  val unknownRdsCode = 99999

  "Create / Transfer Account" must {

    "return a Success Response" when {

      "given no rds code and an account id" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(None, accountId = Some("AB123456")))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountSuccessResponse("AB123456")
        }
      }

    }

    "return a Error Response" when {

      "given a status code other than 200" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusCreated,
              None
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }

      "given no data" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              None
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }

      "given empty data" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(rdsCode = None, accountId = None))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }

      "given an rds code which is not recognised" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(rdsCode = Some(unknownRdsCode)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountErrorResponse
        }
      }
    }

    "return the type-appropriate response" when {

      "given the RDS code for a Investor Not Found Response" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(rdsCode = Some(SUT.INVESTOR_NOT_FOUND)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountInvestorNotFoundResponse
        }
      }

      "given the RDS code for a Investor Not Eligible Response" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(rdsCode = Some(SUT.INVESTOR_NOT_ELIGIBLE)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountInvestorNotEligibleResponse
        }
      }

      "given the RDS code for a Investor Compliance Failed Response" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(rdsCode = Some(SUT.INVESTOR_COMPLIANCE_FAILED)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountInvestorComplianceCheckFailedResponse
        }
      }

      "given the RDS code for a Investor Previous Account Does Not Exist Response" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              200,
              Some(DesCreateAccountResponse(rdsCode = Some(SUT.INVESTOR_PREVIOUS_ACCOUNT_DOES_NOT_EXIST)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse
        }
      }

      "given the RDS code for a Investor Account Already Exists Response" in {
        when(mockDesConnector.createAccount(any(), any())(any()))
          .thenReturn(
            Future.successful((
              httpStatusOk,
              Some(DesCreateAccountResponse(rdsCode = Some(SUT.INVESTOR_ACCOUNT_ALREADY_EXISTS)))
            ))
          )

        doRequest { response =>
          response mustBe CreateLisaAccountAlreadyExistsResponse
        }
      }

    }

  }

  "Close Account" must {

    "return true" when {

      "given any request" in {
        val request = CloseLisaAccountRequest("Voided", new DateTime("2000-01-01"))
        val response = Await.result(SUT.closeAccount("Z019283", "ABC12345", request)(HeaderCarrier()), Duration.Inf)

        response mustBe true
      }

    }

  }

  private def doRequest(callback: (CreateLisaAccountResponse) => Unit) = {
    val request = CreateLisaAccountCreationRequest("1234567890", "Z019283", "9876543210", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createAccount("Z019283", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends AccountService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
