/*
 * Copyright 2023 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import java.time.LocalDate
import scala.concurrent.Future

class DesConnectorSpec extends DesConnectorTestHelper with BeforeAndAfterEach {
  lazy val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]



  override def beforeEach(): Unit = {
    reset(mockHttp)
    when(mockAppContext.desUrl).thenReturn("http://localhost:8883")
    when(mockHttp.get(any())(any())).thenReturn(mockRequestBuilder)
    when(mockHttp.post(any())(any())).thenReturn(mockRequestBuilder)
    when(mockHttp.put(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
  }

  "Create Lisa Investor endpoint" must {
    "return a populated CreateLisaInvestorSuccessResponse" when {
      "The DES response has a json body that is in the correct format" in {

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                s"""{"investorID": "1234567890"}""",
                responseHeader
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(
            CreateLisaInvestorSuccessResponse("1234567890")
          )
        }
      }

      "The DES response has a correct JSON body and multiple types" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                s"""{"investorID": "1234567890"}""",
                Map("Content-Type" -> List("test", "application/json"))
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(
            CreateLisaInvestorSuccessResponse("1234567890")
          )
        }
      }
    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """[1,2,3]"""
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "a 409 is returned with invalid JSON" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                """{"invalid": json""",
                responseHeader
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "a 200 is returned with invalid JSON" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{"invalid": json""",
                responseHeader
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "return a populated CreateLisaInvestorAlreadyExistsResponse" when {
      "the investor already exists response is returned" in {
        val investorID = "1234567890"
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                s"""{"investorID": "$investorID"}""",
                responseHeader
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(CreateLisaInvestorAlreadyExistsResponse(investorID))
        }
      }
    }

    "return a specific DesFailureResponse" when {
      "a specific failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                FORBIDDEN,
                s"""{"code": "INVESTOR_NOT_FOUND","reason": "The investor details given do not match with HMRC’s records."}""",
                responseHeader
              )
            )
          )
        doCreateInvestorRequest { response =>
          response must be(
            DesFailureResponse("INVESTOR_NOT_FOUND", "The investor details given do not match with HMRC’s records.")
          )
        }
      }
    }

    "return a DesUnavailableResponse" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )
        doCreateInvestorRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }

    }

    "return a DesBadRequestResponse" when {
      "a 400 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                ""
              )
            )
          )
        doCreateInvestorRequest { response =>
          response mustBe DesBadRequestResponse
        }
      }
    }
  }

  "Create Account endpoint" must {
    "return a populated success response" when {
      "DES returns 201 created" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                ""
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
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )
        doCreateAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is not 201 created and has a json body that is not in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                s"""{"problem": "service unavailable"}""",
                responseHeader
              )
            )
          )
        doCreateAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a DesUnavailableResponse" when {

      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doCreateAccountRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }

    }

    "return a DesBadRequestResponse" when {
      "a 400 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                ""
              )
            )
          )
        doCreateAccountRequest { response =>
          response mustBe DesBadRequestResponse
        }
      }
    }

    "return a type-appropriate failure response" when {
      "a specific failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                FORBIDDEN,
                s"""{"code": "INVESTOR_NOT_FOUND", "reason": "The investorId given does not match with HMRC’s records."}""",
                responseHeader
              )
            )
          )
        doCreateAccountRequest { response =>
          response mustBe DesFailureResponse(
            "INVESTOR_NOT_FOUND",
            "The investorId given does not match with HMRC’s records."
          )
        }
      }
    }
  }

  "Transfer Account endpoint" must {
    "return a populated success response" when {
      "DES returns 201 created" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                ""
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
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )
        doTransferAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is not 201 created and has a json body that is not in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                s"""{"problem": "service unavailable"}""",
                responseHeader
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
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                FORBIDDEN,
                s"""{"code": "INVESTOR_NOT_FOUND", "reason": "The investorId given does not match with HMRC’s records."}""",
                responseHeader
              )
            )
          )
        doTransferAccountRequest { response =>
          response mustBe DesFailureResponse(
            "INVESTOR_NOT_FOUND",
            "The investorId given does not match with HMRC’s records."
          )
        }
      }
    }

    "return a DesUnavailableResponse" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )
        doTransferAccountRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }
  }

  "Close Lisa Account endpoint" must {
    "return a DesEmptySuccessResponse" when {
      "DES returns 200 ok" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(Future.successful(HttpResponse(OK, "")))

        doCloseAccountRequest { response =>
          response mustBe DesEmptySuccessResponse
        }
      }
    }

    "return a DesUnavailableResponse" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )
        doCloseAccountRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }

    "return a DesBadRequestResponse" when {
      "a 400 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                ""
              )
            )
          )
        doCloseAccountRequest { response =>
          response mustBe DesBadRequestResponse
        }
      }
    }

    "return a DesFailureResponse" when {
      "any other response is received" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )
        doCloseAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

  }

  "Reinstate Lisa Account endpoint" must {

    "return a populated success response" when {
      "DES returns 200 ok" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                s"""{"code": "SUCCESS", "reason": "Account successfully reinstated"}""",
                responseHeader
              )
            )
          )

        doReinstateAccountRequest { response =>
          response mustBe DesReinstateAccountSuccessResponse("SUCCESS", "Account successfully reinstated")
        }
      }
    }

    "return a generic failure response" when {
      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )

        doReinstateAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a DesUnavailableResponse" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doReinstateAccountRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }

    "return a DesBadRequestResponse" when {
      "a 400 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                ""
              )
            )
          )
        doReinstateAccountRequest { response =>
          response mustBe DesBadRequestResponse
        }
      }
    }
  }

  "Update First Subscription date endpoint" must {
    "return a populated DesUpdateSubscriptionSuccessResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                s"""{"code": "INVESTOR_ACCOUNT_NOW_VOID", "reason": "Date of first Subscription updated successfully, but as a result of the date change the account has subsequently been voided"}""",
                responseHeader
              )
            )
          )

        updateFirstSubscriptionDateRequest { response =>
          response mustBe DesUpdateSubscriptionSuccessResponse(
            "INVESTOR_ACCOUNT_NOW_VOID",
            "Date of first Subscription updated successfully, but as a result of the date change the account has subsequently been voided"
          )
        }
      }
    }

    "return a failure response" when {

      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )

        updateFirstSubscriptionDateRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "status is 201 and json is invalid" in {
      when(mockRequestBuilder.execute[HttpResponse](any(),any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              CREATED,
              s"""{"code": "UPDATED_AND_ACCOUNT_VOIDED", "message": "LISA Account firstSubscriptionDate has been updated successfully"}""",
              responseHeader
            )
          )
        )

      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesFailureResponse()
      }
    }

  }

  "return a DesUnavailableResponse" when {

    "a 503 response is returned" in {
      when(mockRequestBuilder.execute[HttpResponse](any(),any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              SERVICE_UNAVAILABLE,
              ""
            )
          )
        )

      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

  }

  "return a DesBadRequestResponse" when {
    "a 400 response is returned" in {
      when(mockRequestBuilder.execute[HttpResponse](any(),any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              BAD_REQUEST,
              ""
            )
          )
        )
      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }
  }

  "Report Life Event endpoint" must {
    "return a populated DesSuccessResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                s"""{"lifeEventID": "87654321"}""",
                responseHeader
              )
            )
          )
        doReportLifeEventRequest { response =>
          response mustBe DesLifeEventResponse("87654321")
        }
      }

    }

    "return a DesUnavailableResponse" when {

      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doReportLifeEventRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }

    }

    "return a generic DesFailureResponse" when {

      "the response json is invalid" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                s"""{"lifeEvent": "87654321"}""",
                responseHeader
              )
            )
          )

        doReportLifeEventRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                ""
              )
            )
          )

        doReportLifeEventRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a populated DesFailureResponse" when {

      "a LIFE_EVENT_INAPPROPRIATE failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                FORBIDDEN,
                s"""{"code": "LIFE_EVENT_INAPPROPRIATE","reason": "The life event conflicts with previous life event reported."}""",
                responseHeader
              )
            )
          )

        doReportLifeEventRequest { response =>
          response mustBe DesFailureResponse(
            "LIFE_EVENT_INAPPROPRIATE",
            "The life event conflicts with previous life event reported."
          )
        }
      }

    }

  }

  "Retrieve Life Event endpoint" must {

    "return a Left of DesUnavailableResponse" when {

      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesUnavailableResponse)
        }
      }

    }

    "return a Left of DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                """{
                  | "code": "ERROR_CODE",
                  | "reason" : "ERROR MESSAGE"
                  }""".stripMargin
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse("ERROR_CODE", "ERROR MESSAGE"))
        }
      }

      "the response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                INTERNAL_SERVER_ERROR,
                ""
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse())
        }
      }

      "the response is badly formed" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                INTERNAL_SERVER_ERROR,
                """{"test": "test"}""",
                responseHeader
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse())
        }
      }

    }

    "return a Right of Seq GetLifeEventItem" when {

      "DES returns successfully" in {

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """[{
                  "lifeEventId": "1234567890",
                  "lifeEventType": "STATUTORY_SUBMISSION",
                  "lifeEventDate": "2018-04-05"
                  }]
                """
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Right(
            List(
              GetLifeEventItem(
                lifeEventId = "1234567890",
                eventType = "Statutory Submission",
                eventDate = LocalDate.parse("2018-04-05")
              )
            )
          )
        }

      }

    }

  }

  "Request Bonus Payment endpoint" must {

    "return a populated DesTransactionResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                s"""{"transactionID": "87654321","message": "On Time"}""",
                responseHeader
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesTransactionResponse("87654321", Some("On Time"))
        }
      }
    }

    "return a populated DesTransactionExistResponse" when {
      "the DES response returns a 409 with a json body that is in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                s"""{"code": "x", "reason": "xx", "transactionID": "87654321"}""",
                responseHeader
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesTransactionExistResponse(code = "x", reason = "xx", transactionID = "87654321")
        }
      }
    }

    "return the default DesFailureResponse" when {

      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                """[1,2,3]"""
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a specific DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                NOT_FOUND,
                s"""{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}""",
                responseHeader
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse(
            "LIFE_EVENT_DOES_NOT_EXIST",
            "The lifeEventId does not match with HMRC’s records."
          )
        }
      }

    }

    "return a DesUnavailableResponse" when {

      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }

    }

    "return a DesFailureResponse" when {

      "a gateway timeout is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.failed(
              UpstreamErrorResponse("Timeout", GATEWAY_TIMEOUT, GATEWAY_TIMEOUT)
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse("Timeout", "Timeout")
        }
      }

    }

    "return a DesUnavailableResponse" when {

      "a 499 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.failed(
              UpstreamErrorResponse("CLIENT CLOSED REQUEST", 499, 499)
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }

    }

    "return a DesBadRequestResponse" when {
      "a 400 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                ""
              )
            )
          )
        doRequestBonusPaymentRequest { response =>
          response mustBe DesBadRequestResponse
        }
      }
    }
  }

  "Retrieve Bonus Payment endpoint" must {
    "return a DesUnavailableResponse" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )
        doRetrieveBonusPaymentRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }

    }

    "return a DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                """{
                  | "code": "ERROR_CODE",
                  | "reason" : "ERROR MESSAGE"
                  }""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }

      "the response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                INTERNAL_SERVER_ERROR,
                ""
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a GetBonusResponse" when {

      "DES returns successfully" in {

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                validBonusPaymentResponseJson,
                responseHeader
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe GetBonusResponse(
            lifeEventId = Some("1234567891"),
            periodStartDate = LocalDate.parse("2017-04-06"),
            periodEndDate = LocalDate.parse("2017-05-05"),
            htbTransfer = Some(HelpToBuyTransfer(0, 10)),
            inboundPayments = InboundPayments(Some(4000), 4000, 4000, 4000),
            bonuses = Bonuses(1000, 1000, Some(1000), "Life Event"),
            creationDate = LocalDate.parse("2017-05-05"),
            paymentStatus = "Paid",
            supersededBy = None,
            supersede = None
          )
        }

      }

    }

  }

  "Retrieve Transaction endpoint" must {

    "return a unavailable response" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  | "code": "ERROR_CODE",
                  | "reason" : "ERROR MESSAGE"
                  }""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }
      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                ""
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
      "the DES response is invalid" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  | "status": "Due"
                  }""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response" when {
      "the DES response is a valid collected Pending transaction" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  |    "paymentStatus": "PENDING",
                  |    "paymentDate": "2000-01-01",
                  |    "paymentReference": "002630000994",
                  |    "paymentAmount": 2.00
                  |}""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPending(
            paymentDueDate = LocalDate.parse("2000-01-01"),
            paymentReference = Some("002630000994"),
            paymentAmount = Some(2.0)
          )
        }
      }
      "the DES response is a valid paid Pending transaction" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  |    "paymentStatus": "PENDING",
                  |    "paymentDate": "2000-01-01"
                  |}""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPending(
            paymentDueDate = LocalDate.parse("2000-01-01"),
            paymentReference = None,
            paymentAmount = None
          )
        }
      }
      "the DES response is a valid Paid transaction" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  |    "paymentStatus": "PAID",
                  |    "paymentDate": "2000-01-01",
                  |    "paymentReference": "002630000993",
                  |    "paymentAmount": 1.00
                  |}""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPaid(
            paymentDate = LocalDate.parse("2000-01-01"),
            paymentReference = "002630000993",
            paymentAmount = 1.0
          )
        }
      }
    }

  }

  "Retrieve Bulk Payment endpoint" must {

    "return a unavailable response" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  | "code": "ERROR_CODE",
                  | "reason" : "ERROR MESSAGE"
                  }""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }
      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                ""
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
      "the DES response is missing a processingDate" in {
        val responseString = "{}"

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response" when {
      "the DES response is the appropriate json response" in {
        val responseString =
          """{
            |    "processingDate": "2017-03-07T09:30:00.000Z",
            |    "idNumber": "Z5555",
            |    "financialTransactions": [
            |      {
            |        "clearedAmount": -1000,
            |        "items": [
            |          {
            |            "clearingSAPDocument": "ABC123456789",
            |            "clearingDate": "2017-06-01"
            |          }
            |        ]
            |      },
            |      {
            |        "outstandingAmount": -1500.55,
            |        "items": [
            |          {
            |            "dueDate": "2017-07-01"
            |          }
            |        ]
            |      }
            |    ]
            |}""".stripMargin

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentSuccessResponse(
            lisaManagerReferenceNumber = "Z5555",
            payments = List(
              BulkPaymentPaid(
                paymentDate = Some(LocalDate.parse("2017-06-01")),
                paymentReference = Some("ABC123456789"),
                paymentAmount = 1000.00
              ),
              BulkPaymentPending(dueDate = Some(LocalDate.parse("2017-07-01")), paymentAmount = 1500.55)
            )
          )
        }
      }
    }

    "return a not found response" when {
      "the DES response has no financial transactions field" in {
        val responseString =
          """{
            | "processingDate": "2017-03-07T09:30:00.000Z",
            | "idNumber": "Z1234"
            |}""".stripMargin

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the DES response has no id number field" in {
        val responseString =
          """{
            | "processingDate": "2017-03-07T09:30:00.000Z",
            | "financialTransactions": []
            |}""".stripMargin

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the DES response has no id number or financial transactions field" in {
        val responseString =
          """{
            | "processingDate": "2017-03-07T09:30:00.000Z"
            |}""".stripMargin

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
    }

  }

  "Retrieve Account endpoint" must {

    "return a unavailable response" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                """{
                  | "code": "ERROR_CODE",
                  | "reason" : "ERROR MESSAGE"
                  }""".stripMargin,
                responseHeader
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }
      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                ""
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
      "the DES response is missing required fields" in {
        val responseString = "{}"

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response" when {
      "the DES response is the appropriate json response" in {
        val responseString =
          """{
            |  "investorId": "1234567890",
            |  "status": "OPEN",
            |  "creationDate": "2016-01-01",
            |  "creationReason": "REINSTATED",
            |  "hmrcClosureDate": "2016-02-01",
            |  "accountClosureReason": "TRANSFERRED_OUT",
            |  "transferInDate": "2016-03-01",
            |  "transferOutDate": "2016-04-01",
            |  "xferredFromAccountId": "123abc789ABC34567890",
            |  "xferredFromLmrn": "Z123453",
            |  "lisaManagerClosureDate": "2016-05-01",
            |  "subscriptionStatus": "AVAILABLE",
            |  "firstSubscriptionDate": "2016-01-06"
            |}""".stripMargin

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe GetLisaAccountSuccessResponse(
            accountId = "123456",
            investorId = "1234567890",
            creationReason = "Reinstated",
            firstSubscriptionDate = LocalDate.parse("2016-01-06"),
            accountStatus = "OPEN",
            subscriptionStatus = "AVAILABLE",
            accountClosureReason = Some("Transferred out"),
            closureDate = Some(LocalDate.parse("2016-05-01")),
            transferAccount = Some(
              GetLisaAccountTransferAccount(
                transferredFromAccountId = "123abc789ABC34567890",
                transferredFromLMRN = "Z123453",
                transferInDate = LocalDate.parse("2016-03-01")
              )
            )
          )
        }
      }
    }

    "return a subscriptionStatus of AVAILABLE" when {
      "there is no subscriptionStatus in the json response from DES" in {
        val responseString =
          """{
            |  "investorId": "1234567890",
            |  "status": "OPEN",
            |  "creationDate": "2016-01-01",
            |  "creationReason": "REINSTATED",
            |  "hmrcClosureDate": "2016-02-01",
            |  "accountClosureReason": "TRANSFERRED_OUT",
            |  "transferInDate": "2016-03-01",
            |  "transferOutDate": "2016-04-01",
            |  "xferredFromAccountId": "123abc789ABC34567890",
            |  "xferredFromLmrn": "Z123453",
            |  "lisaManagerClosureDate": "2016-05-01",
            |  "firstSubscriptionDate": "2016-01-06"
            |}""".stripMargin

        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                OK,
                responseString,
                responseHeader
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe GetLisaAccountSuccessResponse(
            accountId = "123456",
            investorId = "1234567890",
            creationReason = "Reinstated",
            firstSubscriptionDate = LocalDate.parse("2016-01-06"),
            accountStatus = "OPEN",
            subscriptionStatus = "AVAILABLE",
            accountClosureReason = Some("Transferred out"),
            closureDate = Some(LocalDate.parse("2016-05-01")),
            transferAccount = Some(
              GetLisaAccountTransferAccount(
                transferredFromAccountId = "123abc789ABC34567890",
                transferredFromLMRN = "Z123453",
                transferInDate = LocalDate.parse("2016-03-01")
              )
            )
          )
        }
      }
    }
  }

  "Report withdrawal endpoint" must {
    "uses the des writes when posting data" in {
      when(mockRequestBuilder.execute[HttpResponse](any(),any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              CREATED,
              s"""{"transactionID": "87654321","message": "On Time"}""",
              responseHeader
            )
          )
        )
      doReportWithdrawalRequest { response =>
        verify(mockHttp).post(any())(any())
      }
    }

    "return a populated DesTransactionResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                s"""{"transactionID": "87654321","message": "On Time"}""",
                responseHeader
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesTransactionResponse("87654321", Some("On Time"))
        }
      }

    }

    "return a populated DesTransactionExistResponse" when {
      "the DES response has status CONFLICT" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CONFLICT,
                s"""{"code": "WITHDRAWAL_CHARGE_ALREADY_EXISTS","reason": "A withdrawal charge with these details has already been requested for this investor","investorTransactionID":"2345678901"}""",
                responseHeader
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesWithdrawalChargeAlreadyExistsResponse(
            "WITHDRAWAL_CHARGE_ALREADY_EXISTS",
            "A withdrawal charge with these details has already been requested for this investor",
            "2345678901"
          )
        }
      }

      "the DES response has status FORBIDDEN and a transactionID value in the json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                FORBIDDEN,
                s"""{"code": "SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED","reason": "This withdrawal charge has already been superseded","supersededTransactionByID": "2345678901"}""",
                responseHeader
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesWithdrawalChargeAlreadySupersededResponse(
            "SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED",
            "This withdrawal charge has already been superseded",
            "2345678901"
          )
        }
      }
    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                GATEWAY_TIMEOUT,
                ""
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                """[1,2,3]"""
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a html body instead of JSON format" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                CREATED,
                """<!DOCTYPE html>"""
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

    }

    "return a specific DesFailureResponse" when {
      "a specific failure is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                NOT_FOUND,
                s"""{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}""",
                responseHeader
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesFailureResponse(
            "LIFE_EVENT_DOES_NOT_EXIST",
            "The lifeEventId does not match with HMRC’s records."
          )
        }
      }

    }

    "return a DesUnavailableResponse" when {
      "a 503 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))

          .thenReturn(
            Future.successful(
              HttpResponse(
                SERVICE_UNAVAILABLE,
                ""
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesUnavailableResponse
        }
      }
    }

    "return a DesBadRequestResponse" when {
      "a 400 is returned" in {
        when(mockRequestBuilder.execute[HttpResponse](any(),any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                BAD_REQUEST,
                ""
              )
            )
          )
        doReportWithdrawalRequest { response =>
          response mustBe DesBadRequestResponse
        }
      }
    }
  }
}
