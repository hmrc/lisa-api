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
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpGet, HttpPost, HttpResponse}
import play.api.test.Helpers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.Source

class DesConnectorSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "Create Lisa Investor endpoint" must {

    "Return a populated DesCreateInvestorResponse" when {

      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"investorID": "AB123456"}"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((
            CREATED,
            DesCreateInvestorResponse(investorID = "AB123456")
          ))
        }
      }

    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {

        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((INTERNAL_SERVER_ERROR, DesFailureResponse()))
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse("""[1,2,3]"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((INTERNAL_SERVER_ERROR, DesFailureResponse()))
        }
      }
    }

    "return an investor already exists response" when {

      "the investor already exists response is returned" in {
        val investorID = "1234567890"
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CONFLICT,
                responseJson = Some(Json.parse(s"""{"investorID": "$investorID"}"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((CONFLICT, DesCreateInvestorResponse(investorID)))
        }
      }

    }

    "return a specific DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = FORBIDDEN,
                responseJson = Some(Json.parse(s"""{"code": "INVESTOR_NOT_FOUND","reason": "The investor details given do not match with HMRC’s records."}"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be((FORBIDDEN, DesFailureResponse("INVESTOR_NOT_FOUND", "The investor details given do not match with HMRC’s records.")))
        }
      }

    }

  }

  "Create Account endpoint" must {

    "return a populated success response" when {

      "DES returns 201 created" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = None
              )
            )
          )

        doCreateAccountRequest { response =>
          response mustBe DesAccountResponse("9876543210")
        }
      }

    }

    "return a generic failure response" when {

      "the DES response is not 201 created and has no json body" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doCreateAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is not 201 created and has a json body that is not in the correct format" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = Some(Json.parse(s"""{"problem": "service unavailable"}"""))
              )
            )
          )

        doCreateAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a type-appropriate failure response" when {

      "a specific failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = FORBIDDEN,
                responseJson = Some(Json.parse(s"""{"code": "INVESTOR_NOT_FOUND", "reason": "The investorId given does not match with HMRC’s records."}"""))
              )
            )
          )

        doCreateAccountRequest { response =>
          response mustBe DesFailureResponse("INVESTOR_NOT_FOUND", "The investorId given does not match with HMRC’s records.")
        }
      }

    }

  }

  "Transfer Account endpoint" must {

    "return a populated success response" when {

      "DES returns 201 created" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = None
              )
            )
          )

        doTransferAccountRequest { response =>
          response mustBe DesAccountResponse("9876543210")
        }
      }

    }

    "return a generic failure response" when {

      "the DES response is not 201 created and has no json body" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doTransferAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is not 201 created and has a json body that is not in the correct format" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = Some(Json.parse(s"""{"problem": "service unavailable"}"""))
              )
            )
          )

        doTransferAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a type-appropriate failure response" when {

      "a specific failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = FORBIDDEN,
                responseJson = Some(Json.parse(s"""{"code": "INVESTOR_NOT_FOUND", "reason": "The investorId given does not match with HMRC’s records."}"""))
              )
            )
          )

        doTransferAccountRequest { response =>
          response mustBe DesFailureResponse("INVESTOR_NOT_FOUND", "The investorId given does not match with HMRC’s records.")
        }
      }

    }

  }

  "Close Lisa Account endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = OK, responseJson = None)))

        doCloseAccountRequest { response =>
          response must be(DesEmptySuccessResponse)
        }
      }
    }

    "Return no DesAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doCloseAccountRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }
  }

  "Update First Subscription date endpoint" must {

    "Return a populated DesUpdateSubscriptionSuccessResponse" when {

      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[UpdateSubscriptionRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(s"""{"code": "UPDATED_AND_ACCOUNT_VOIDED", "message": "LISA Account firstSubscriptionDate has been updated successfully"}"""))
              )
            )
          )

        updateFirstSubscriptionDateRequest { response =>
          response must be((
            DesUpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_VOIDED", "LISA Account firstSubscriptionDate has been updated successfully" )
          ))
        }
      }
    }

    "Return an failure response" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[UpdateSubscriptionRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        updateFirstSubscriptionDateRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "Return a DesFailureResponse" when {
      "Status is 201 and Json is invalid" in {
        when(mockHttpPost.POST[UpdateSubscriptionRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"code": "UPDATED_AND_ACCOUNT_VOIDED", "message": "LISA Account firstSubscriptionDate has been updated successfully"}"""))
              )
            )
          )

        updateFirstSubscriptionDateRequest { response =>
          response must be(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Server Error"))
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
                responseStatus = SERVICE_UNAVAILABLE,
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
                responseStatus = OK,
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
                responseStatus = CREATED,
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
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"lifeEvent": "87654321"}"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesFailureResponse("INTERNAL_SERVER_ERROR","Internal Server Error"))
        }

      }
    }

    "Return a populated DesLifeEventExistResponse" when {
      "A LIFE_EVENT_ALREADY_EXISTS failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CONFLICT,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_ALREADY_EXISTS", "reason": "The investor’s life event has already been reported.", "lifeEventID": "1234567891"}"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesLifeEventExistResponse("LIFE_EVENT_ALREADY_EXISTS", "The investor’s life event has already been reported.", "1234567891"))
        }
      }
    }

    "Return a populated DesFailureResponse" when {
      "A LIFE_EVENT_INAPPROPRIATE failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = FORBIDDEN,
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

    "return a populated DesTransactionResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"transactionID": "87654321","message": "On Time"}"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be((CREATED, DesTransactionResponse("87654321","On Time")))
        }
      }
    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {
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
          response must be((INTERNAL_SERVER_ERROR, DesFailureResponse()))
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
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
          response must be((INTERNAL_SERVER_ERROR, DesFailureResponse()))
        }
      }
    }

    "return a specific DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = NOT_FOUND,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response must be((NOT_FOUND, DesFailureResponse("LIFE_EVENT_DOES_NOT_EXIST", "The lifeEventId does not match with HMRC’s records.")))
        }
      }

    }

  }

  "Retrieve Life Event endpoint" must {

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(
                  """{
                    | "code": "ERROR_CODE",
                    | "reason" : "ERROR MESSAGE"
                  }""".stripMargin))
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response must be(DesFailureResponse("ERROR_CODE", "ERROR MESSAGE"))
        }
      }
      "the DES response has no json body" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response must be(DesFailureResponse())
        }
      }
      "the DES response is invalid" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(
                  """
                    |{
                    |  "lifeEventId" : "1234567890",
                    |  "eventType" : "UNKNOWN"
                    |}
                  """.stripMargin))
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "return a success response" when {
      "the DES response is a life event" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(
                  """
                    |{
                    |  "lifeEventID" : "1234567890",
                    |  "eventType" : "LISA Investor Terminal Ill Health",
                    |  "eventDate" : "2017-04-06"
                    |}
                  """.stripMargin))
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response must be(DesLifeEventRetrievalResponse("1234567890", "LISA Investor Terminal Ill Health", new DateTime("2017-04-06")))
        }
      }
    }

  }

  "Retrieve Bonus Payment endpoint" must {

    "return a specific DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CONFLICT,
                responseJson = Some(Json.parse(
                  """{
                    | "code": "ERROR_CODE",
                    | "reason" : "ERROR MESSAGE"
                  }""".stripMargin))
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }

      "the response has no json body" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = INTERNAL_SERVER_ERROR,
                responseJson = None
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }


    "return a success response" when {

      "DES returns successfully" in {

        when(mockHttpGet.GET[HttpResponse](any())(any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(validBonusPaymentJson))
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe DesGetBonusPaymentResponse(
            lifeEventId = Some("1234567891"),
            periodStartDate = new DateTime("2017-04-06"),
            periodEndDate = new DateTime("2017-05-05"),
            htbTransfer = Some(HelpToBuyTransfer(0f, 10f)),
            inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
            bonuses = Bonuses(1000f, 1000f, Some(1000f), "Life Event")
          )
        }

      }

    }

  }

  val validBonusPaymentJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment.json")).mkString

  private def doCreateInvestorRequest(callback: ((Int,DesResponse)) => Unit) = {
    val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doCreateAccountRequest(callback: (DesResponse) => Unit) = {
    val request = CreateLisaAccountCreationRequest("1234567890",  "9876543210", new DateTime("2000-01-01"))
    val response = Await.result(SUT.createAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doTransferAccountRequest(callback: (DesResponse) => Unit) = {
    val transferAccount = AccountTransfer("1234", "1234", new DateTime("2000-01-01"))
    val request = CreateLisaAccountTransferRequest("1234567890",  "9876543210", new DateTime("2000-01-01"), transferAccount)
    val response = Await.result(SUT.transferAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  private def doCloseAccountRequest(callback: (DesResponse) => Unit) = {
    val request = CloseLisaAccountRequest("All funds withdrawn", new DateTime("2000-01-01"))
    val response = Await.result(SUT.closeAccount("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }


  private def updateFirstSubscriptionDateRequest(callback: (DesResponse) => Unit) = {
    val request = UpdateSubscriptionRequest(new DateTime("2000-01-01"))
    val response = Await.result(SUT.updateFirstSubDate("Z019283", "123456789", request), Duration.Inf)

    callback(response)
  }

  private def doReportLifeEventRequest(callback: (DesResponse) => Unit) = {
    val request = ReportLifeEventRequest("LISA Investor Terminal Ill Health",new DateTime("2000-01-01"))
    val response = Await.result(SUT.reportLifeEvent("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  private def doRetrieveLifeEventRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.getLifeEvent("Z123456", "ABC12345", "123456"), Duration.Inf)

    callback(response)
  }

  private def doRequestBonusPaymentRequest(callback: ((Int, DesResponse)) => Unit) = {
    val request = RequestBonusPaymentRequest(
      lifeEventId = Some("1234567891"),
      periodStartDate = new DateTime("2017-04-06"),
      periodEndDate = new DateTime("2017-05-05"),
      htbTransfer = Some(HelpToBuyTransfer(0f, 0f)),
      inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
      bonuses = Bonuses(1000f, 1000f, None, "Life Event")
    )

    val response = Await.result(SUT.requestBonusPayment("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  private def doRetrieveBonusPaymentRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.getBonusPayment("Z123456", "ABC12345", "123456"), Duration.Inf)

    callback(response)
  }

  val mockHttpPost = mock[HttpPost]
  val mockHttpGet = mock[HttpGet]

  implicit val hc = HeaderCarrier()

  object SUT extends DesConnector {
    override val httpPost = mockHttpPost
    override val httpGet = mockHttpGet
  }
}
