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

package unit.connectors

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import play.api.test.Helpers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class DesConnectorSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  val statusCodeSuccess = 200
  val statusCodeServiceUnavailable = 503
  val rdsCodeInvestorNotFound = 63214
  val rdsCodeAccountAlreadyExists = 63219
  val statusCodeCreated = 201
  val statusCodeForbidden = 403


  "Create Lisa Investor endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = statusCodeSuccess, responseJson = None)))

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeSuccess,
            None
          ))
        }
      }
    }

    "Return no DesCreateInvestorResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeServiceUnavailable,
            None
          ))
        }
      }
    }

    "Return any empty DesCreateInvestorResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesCreateInvestorResponse(None, None))
          ))
        }
      }
    }

    "Return a populated DesCreateInvestorResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse(s"""{"rdsCode":$rdsCodeInvestorNotFound, "investorId": "AB123456"}"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesCreateInvestorResponse(rdsCode = Some(rdsCodeInvestorNotFound), investorId = Some("AB123456")))
          ))
        }
      }
    }

  }

  "Create Lisa Account endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = statusCodeSuccess, responseJson = None)))

        doCreateAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            None
          ))
        }
      }
    }

    "Return no DesAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doCreateAccountRequest { response =>
          response must be((
            statusCodeServiceUnavailable,
            None
          ))
        }
      }
    }

    "Return any empty DesAccountResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doCreateAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesAccountResponse(None, None))
          ))
        }
      }
    }

    "Return a populated DesAccountResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse(s"""{"rdsCode":$rdsCodeAccountAlreadyExists, "accountId": "AB123456"}"""))
              )
            )
          )

        doCreateAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesAccountResponse(rdsCode = Some(rdsCodeAccountAlreadyExists), accountId = Some("AB123456")))
          ))
        }
      }
    }

  }

  "Transfer Lisa Account endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = statusCodeSuccess, responseJson = None)))

        doTransferAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            None
          ))
        }
      }
    }

    "Return no DesAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doTransferAccountRequest { response =>
          response must be((
            statusCodeServiceUnavailable,
            None
          ))
        }
      }
    }

    "Return any empty DesAccountResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doTransferAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesAccountResponse(None, None))
          ))
        }
      }
    }

    "Return a populated DesAccountResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse(s"""{"rdsCode":$rdsCodeAccountAlreadyExists, "accountId": "AB123456"}"""))
              )
            )
          )

        doTransferAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesAccountResponse(rdsCode = Some(rdsCodeAccountAlreadyExists), accountId = Some("AB123456")))
          ))
        }
      }
    }

  }

  "Close Lisa Account endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = statusCodeSuccess, responseJson = None)))

        doCloseAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            None
          ))
        }
      }
    }

    "Return no DesAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doCloseAccountRequest { response =>
          response must be((
            statusCodeServiceUnavailable,
            None
          ))
        }
      }
    }

    "Return any empty DesAccountResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doCloseAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesAccountResponse(None, None))
          ))
        }
      }
    }

    "Return a populated DesAccountResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse(s"""{"rdsCode": null, "accountId": "AB123456"}"""))
              )
            )
          )

        doCloseAccountRequest { response =>
          response must be((
            statusCodeSuccess,
            Some(DesAccountResponse(rdsCode = None, accountId = Some("AB123456")))
          ))
        }
      }
    }

  }

  "Report Life Event endpoint" must {

    "Return an failure response" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeServiceUnavailable,
                responseJson = None
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "Return any empty LifeEventResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeSuccess,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "Return a populated DesSuccessResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeCreated,
                responseJson = Some(Json.parse(s"""{"lifeEventID": "87654321"}"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesLifeEventResponse("87654321"))
        }
      }
    }

    "Return a DesFailureResponse" when {
      "Status is 201 and Json is invalid" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeCreated,
                responseJson = Some(Json.parse(s"""{"lifeEvent": "87654321"}"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Server Error"))
        }

      }
    }

    "Return a populated DesFailureResponse" when {
      "A LIFE_EVENT_INAPPROPRIATE failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = statusCodeForbidden,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_INAPPROPRIATE","reason": "The life event conflicts with previous life event reported."}"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesFailureResponse("LIFE_EVENT_INAPPROPRIATE","The life event conflicts with previous life event reported."))
        }
      }
    }

  }

  "Request Bonus Payment endpoint" must {

    "Return an failure response" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "Return a populated DesTransactionResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"transactionID": "87654321"}"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be(DesTransactionResponse("87654321"))
        }
      }
    }

    "Return a default DesFailureResponse" when {
      "Status is 201 and Json is invalid" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"transaction": "87654321"}"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Server Error"))
        }

      }
    }

    "Return a populated DesFailureResponse" when {
      "A LIFE_EVENT_DOES_NOT_EXIST failure is returned" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = NOT_FOUND,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventID does not match with HMRC’s records."}"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be(DesFailureResponse("LIFE_EVENT_DOES_NOT_EXIST", "The lifeEventID does not match with HMRC’s records."))
        }
      }
    }

  }

  private def doCreateInvestorRequest(callback: ((Int, Option[DesCreateInvestorResponse])) => Unit) = {
    val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doCreateAccountRequest(callback: ((Int, Option[DesAccountResponse])) => Unit) = {
    val request = CreateLisaAccountCreationRequest("1234567890", "Z019283", "9876543210", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doTransferAccountRequest(callback: ((Int, Option[DesAccountResponse])) => Unit) = {
    val transferAccount = AccountTransfer("1234", "1234", new DateTime("2000-01-01"))
    val request = CreateLisaAccountTransferRequest("1234567890", "Z019283", "9876543210", new DateTime("2000-01-01"), transferAccount)
    val response = Await.result(SUT.transferAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doCloseAccountRequest(callback: ((Int, Option[DesAccountResponse])) => Unit) = {
    val request = CloseLisaAccountRequest("Voided", new DateTime("2000-01-01"))
    val response = Await.result(SUT.closeAccount("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  private def doReportLifeEventRequest(callback: (DesResponse) => Unit) = {
    val request = ReportLifeEventRequest("1234567890","Z543210","LISA Investor Terminal Ill Health",new DateTime("2000-01-01"))
    val response = Await.result(SUT.reportLifeEvent("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  private def doRequestBonusPaymentRequest(callback: (DesResponse) => Unit) = {
    val request = RequestBonusPaymentRequest(
      lifeEventID = "1234567891",
      periodStartDate = new DateTime("2016-05-22"),
      periodEndDate = new DateTime("2017-05-22"),
      transactionType = "Bonus",
      htbTransfer = HelpToBuyTransfer(0f, 0f),
      inboundPayments = InboundPayments(4000f, 4000f, 4000f, 4000f),
      bonuses = Bonuses(1000f, 1000f, None, "Life Event")
    )

    val response = Await.result(SUT.requestBonusPayment("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  val mockHttpPost = mock[HttpPost]
  implicit val hc = HeaderCarrier()

  object SUT extends DesConnector {
    override val httpPost = mockHttpPost
  }
}
