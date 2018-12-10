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

package unit.connectors

import org.joda.time.DateTime
import org.mockito.Matchers.{eq => matchersEquals, _}
import org.mockito.Mockito.{verify, when}
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import play.api.test.Helpers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.io.Source
import uk.gov.hmrc.http._
import uk.gov.hmrc.lisaapi.models

class DesConnectorSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "Create Lisa Investor endpoint" must {

    "return a populated CreateLisaInvestorSuccessResponse" when {

      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"investorID": "1234567890"}"""))
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

        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse("""[1,2,3]"""))
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
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CONFLICT,
                responseJson = Some(Json.parse(s"""{"investorID": "$investorID"}"""))
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
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = FORBIDDEN,
                responseJson = Some(Json.parse(s"""{"code": "INVESTOR_NOT_FOUND","reason": "The investor details given do not match with HMRC’s records."}"""))
              )
            )
          )

        doCreateInvestorRequest { response =>
          response must be(DesFailureResponse("INVESTOR_NOT_FOUND", "The investor details given do not match with HMRC’s records."))
        }
      }

    }

  }

  "Create Account endpoint" must {

    "return a populated success response" when {

      "DES returns 201 created" in {
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[CreateLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = OK, responseJson = None)))

        doCloseAccountRequest { response =>
          response must be(DesEmptySuccessResponse)
        }
      }
    }

    "Return no DesAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CloseLisaAccountRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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

  "Reinstate Lisa Account endpoint" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPut.PUT[JsValue, HttpResponse](any(), any())(any(),any(), any(), any()))
          .thenReturn (Future.successful(HttpResponse(responseStatus = OK,
            responseJson =  Some(Json.parse(s"""{"code": "SUCCESS", "reason": "Account successfully reinstated"}""")))))

        doReinstateAccountRequest { response =>
          response must be(DesReinstateAccountSuccessResponse("SUCCESS", "Account successfully reinstated"))
        }
      }
    }

    "Return no DesAccountResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPut.PUT[JsValue,HttpResponse](any(),any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doReinstateAccountRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }
  }

  "Update First Subscription date endpoint" must {

    "Return a populated DesUpdateSubscriptionSuccessResponse" when {

      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPut.PUT[UpdateSubscriptionRequest, HttpResponse](any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(s"""{"code": "INVESTOR_ACCOUNT_NOW_VOID", "reason": "Date of first Subscription updated successfully, but as a result of the date change the account has subsequently been voided"}"""))
              )
            )
          )

        updateFirstSubscriptionDateRequest { response =>
          response must be((
            DesUpdateSubscriptionSuccessResponse("INVESTOR_ACCOUNT_NOW_VOID", "Date of first Subscription updated successfully, but as a result of the date change the account has subsequently been voided" )
          ))
        }
      }
    }

    "Return an failure response" when {
      "The DES response has no json body" in {
        when(mockHttpPut.PUT[UpdateSubscriptionRequest, HttpResponse](any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[UpdateSubscriptionRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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

    "Return a populated DesFailureResponse" when {
      "A LIFE_EVENT_INAPPROPRIATE failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
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

      "A LIFE_EVENT_ALREADY_EXISTS failure is returned" in {
        when(mockHttpPost.POST[ReportLifeEventRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CONFLICT,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_ALREADY_EXISTS", "reason": "The investor’s life event has already been reported."}"""))
              )
            )
          )

        doReportLifeEventRequest { response =>
          response must be(DesFailureResponse("LIFE_EVENT_ALREADY_EXISTS", "The investor’s life event has already been reported."))
        }
      }
    }

  }

  "Retrieve Life Event endpoint" must {

    "return a Left of DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
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

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse("ERROR_CODE", "ERROR MESSAGE"))
        }
      }

      "the response has no json body" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = INTERNAL_SERVER_ERROR,
                responseJson = None
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse())
        }
      }

      "the response is badly formed" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = INTERNAL_SERVER_ERROR,
                responseJson = Some(Json.obj("test" -> "test"))
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse())
        }
      }

    }

    "return a Right of ReportLifeEventRequestBase" when {

      "DES returns successfully" in {

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.arr(Json.obj(
                  "lifeEventType" -> "STATUTORY_SUBMISSION",
                  "lifeEventDate" -> "2018-04-05",
                  "isaManagerName" -> "ISA Manager",
                  "annualSubsCash" -> 0,
                  "annualSubsStocksAndShares" -> 55,
                  "marketValueCash" -> 0,
                  "marketValueStocksAndShares" -> 65,
                  "taxYear" -> "2018",
                  "supersededLifeEventId" -> "1234567890",
                  "supersededLifeEventDate" -> "2018-04-04",
                  "lifeEventSupersededById" -> "1234567891"
                )))
              )
            )
          )

        doRetrieveLifeEventRequest { response =>
          response mustBe Right(List(AnnualReturn(
            eventDate = new DateTime("2018-04-05"),
            lisaManagerName = "ISA Manager",
            taxYear = 2018,
            marketValueCash = 0,
            marketValueStocksAndShares = 65,
            annualSubsCash = 0,
            annualSubsStocksAndShares = 55,
            supersede = Some(AnnualReturnSupersede(
              originalEventDate = new DateTime("2018-04-04"),
              originalLifeEventId = "1234567890"
            )),
            supersededBy = Some("1234567891")
          )))
        }

      }

    }

  }

  "Request Bonus Payment endpoint" must {

    "return a populated DesTransactionResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"transactionID": "87654321","message": "On Time"}"""))
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
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CONFLICT,
                responseJson = Some(Json.parse(s"""{"code": "x", "reason": "xx", "transactionID": "87654321"}"""))
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
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse("""[1,2,3]"""))
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
        when(mockHttpPost.POST[RequestBonusPaymentRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = NOT_FOUND,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}"""))
              )
            )
          )

        doRequestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse("LIFE_EVENT_DOES_NOT_EXIST", "The lifeEventId does not match with HMRC’s records.")
        }
      }

    }

  }

  "Retrieve Bonus Payment endpoint" must {

    "return a DesFailureResponse" when {

      "a specific failure is returned" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
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
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
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

    "return a GetBonusResponse" when {

      "DES returns successfully" in {

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(validBonusPaymentResponseJson))
              )
            )
          )

        doRetrieveBonusPaymentRequest { response =>
          response mustBe GetBonusResponse(
            lifeEventId = Some("1234567891"),
            periodStartDate = new DateTime("2017-04-06"),
            periodEndDate = new DateTime("2017-05-05"),
            htbTransfer = Some(HelpToBuyTransfer(0, 10)),
            inboundPayments = InboundPayments(Some(4000), 4000, 4000, 4000),
            bonuses = Bonuses(1000, 1000, Some(1000), "Life Event"),
            creationDate = new DateTime("2017-05-05"),
            paymentStatus = "Paid",
            supersededBy = None,
            supersede = None
          )
        }

      }

    }

  }

  "Retrieve Transaction endpoint" must {

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
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

        doRetrieveTransactionRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }
      "the DES response has no json body" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = None
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
      "the DES response is invalid" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse(
                  """{
                    | "status": "Due"
                  }""".stripMargin))
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response" when {
      "the DES response is a valid Pending transaction" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse("""{
                                                 |    "paymentStatus": "PENDING",
                                                 |    "paymentDueDate": "2000-01-01"
                                                 |}""".stripMargin))
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPending(
            paymentDueDate = new DateTime("2000-01-01")
          )
        }
      }
      "the DES response is a valid Paid transaction" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(Json.parse("""{
                                                 |    "paymentStatus": "PAID",
                                                 |    "paymentDate": "2000-01-01",
                                                 |    "paymentReference": "002630000993",
                                                 |    "paymentAmount": 1.00
                                                 |}""".stripMargin))
              )
            )
          )

        doRetrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPaid(
            paymentDate = new DateTime("2000-01-01"),
            paymentReference = "002630000993",
            paymentAmount = 1.0
          )
        }
      }
    }

  }

  "Retrieve Bulk Payment endpoint" must {

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
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

        doRetrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }
      "the DES response has no json body" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = None
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
      "the DES response is missing a processingDate" in {
        val responseJson = Json.parse("{}")

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
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
        val responseJson = Json.parse("""{
                                        |    "processingDate": "2017-03-07T09:30:00.000Z",
                                        |    "idNumber": "Z5555",
                                        |    "financialTransactions": [
                                        |      {
                                        |        "clearedAmount": -1000,
                                        |        "sapDocumentNumber": "ABC123456789",
                                        |        "items": [
                                        |          {
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
                                        |}""".stripMargin)

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentSuccessResponse(
            lisaManagerReferenceNumber = "Z5555",
            payments = List(
              BulkPaymentPaid(paymentDate = new DateTime("2017-06-01"), paymentReference = "ABC123456789", paymentAmount = 1000.00),
              BulkPaymentPending(dueDate = new DateTime("2017-07-01"), paymentAmount = 1500.55)
            )
          )
        }
      }
    }

    "return a not found response" when {
      "the DES response has no financial transactions field" in {
        val responseJson = Json.parse(
          """{
            | "processingDate": "2017-03-07T09:30:00.000Z",
            | "idNumber": "Z1234"
            |}""".stripMargin)

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the DES response has no id number field" in {
        val responseJson = Json.parse(
          """{
            | "processingDate": "2017-03-07T09:30:00.000Z",
            | "financialTransactions": []
            |}""".stripMargin)

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
              )
            )
          )

        doRetrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the DES response has no id number or financial transactions field" in {
        val responseJson = Json.parse(
          """{
            | "processingDate": "2017-03-07T09:30:00.000Z"
            |}""".stripMargin)

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
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

    "return a failure response" when {
      "the DES response is a failure response" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
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

        doRetrieveAccountRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }
      "the DES response has no json body" in {
        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = None
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
      "the DES response is missing required fields" in {
        val responseJson = Json.parse("{}")

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
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
        val responseJson = Json.parse("""{
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
                                          |}""".stripMargin)

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe GetLisaAccountSuccessResponse(
            accountId = "123456",
            investorId = "1234567890",
            creationReason = "Reinstated",
            firstSubscriptionDate = new DateTime("2016-01-06"),
            accountStatus = "OPEN",
            subscriptionStatus = "AVAILABLE",
            accountClosureReason = Some("Transferred out"),
            closureDate = Some(new DateTime("2016-05-01")),
            transferAccount = Some(GetLisaAccountTransferAccount(
              transferredFromAccountId = "123abc789ABC34567890",
              transferredFromLMRN = "Z123453",
              transferInDate = new DateTime("2016-03-01")
            ))
          )
        }
      }
    }

    "return a subscriptionStatus of AVAILABLE" when {
      "there is no subscriptionStatus in the json response from DES" in {
        val responseJson = Json.parse("""{
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
                                        |}""".stripMargin)

        when(mockHttpGet.GET[HttpResponse](any())(any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = OK,
                responseJson = Some(responseJson)
              )
            )
          )

        doRetrieveAccountRequest { response =>
          response mustBe GetLisaAccountSuccessResponse(
            accountId = "123456",
            investorId = "1234567890",
            creationReason = "Reinstated",
            firstSubscriptionDate = new DateTime("2016-01-06"),
            accountStatus = "OPEN",
            subscriptionStatus = "AVAILABLE",
            accountClosureReason = Some("Transferred out"),
            closureDate = Some(new DateTime("2016-05-01")),
            transferAccount = Some(GetLisaAccountTransferAccount(
              transferredFromAccountId = "123abc789ABC34567890",
              transferredFromLMRN = "Z123453",
              transferInDate = new DateTime("2016-03-01")
            ))
          )
        }
      }
    }

  }

  "Report withdrawal endpoint" must {

    "uses the des writes when posting data" in {
      when(mockHttpPost.POST[ReportWithdrawalChargeRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
        .thenReturn(
          Future.successful(
            HttpResponse(
              responseStatus = CREATED,
              responseJson = Some(Json.parse(s"""{"transactionID": "87654321","message": "On Time"}"""))
            )
          )
        )

      doReportWithdrawalRequest { response =>
        verify(mockHttpPost).POST(any(), any(), any())(matchersEquals(ReportWithdrawalChargeRequest.desReportWithdrawalChargeWrites), any(), any(), any())
      }
    }

    "return a populated DesTransactionResponse" when {
      "the DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[ReportWithdrawalChargeRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse(s"""{"transactionID": "87654321","message": "On Time"}"""))
              )
            )
          )

        doReportWithdrawalRequest { response =>
          response mustBe DesTransactionResponse("87654321", Some("On Time"))
        }
      }
    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {
        when(mockHttpPost.POST[ReportWithdrawalChargeRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = SERVICE_UNAVAILABLE,
                responseJson = None
              )
            )
          )

        doReportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[ReportWithdrawalChargeRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = CREATED,
                responseJson = Some(Json.parse("""[1,2,3]"""))
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
        when(mockHttpPost.POST[ReportWithdrawalChargeRequest, HttpResponse](any(), any(), any())(any(), any(), any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse(
                responseStatus = NOT_FOUND,
                responseJson = Some(Json.parse(s"""{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}"""))
              )
            )
          )

        doReportWithdrawalRequest { response =>
          response mustBe DesFailureResponse("LIFE_EVENT_DOES_NOT_EXIST", "The lifeEventId does not match with HMRC’s records.")
        }
      }

    }

  }

  val validBonusPaymentResponseJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment-response.json")).mkString

  private def doCreateInvestorRequest(callback: (DesResponse) => Unit) = {
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

  private def doReinstateAccountRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.reinstateAccount("Z123456", "ABC12345"), Duration.Inf)

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

  private def doRetrieveLifeEventRequest(callback: (Either[DesResponse, Seq[ReportLifeEventRequestBase]]) => Unit) = {
    val response = Await.result(SUT.getLifeEvent("Z123456", "ABC12345", "1234567890"), Duration.Inf)

    callback(response)
  }

  private def doRequestBonusPaymentRequest(callback: (DesResponse) => Unit) = {
    val request = RequestBonusPaymentRequest(
      lifeEventId = Some("1234567891"),
      periodStartDate = new DateTime("2017-04-06"),
      periodEndDate = new DateTime("2017-05-05"),
      htbTransfer = Some(HelpToBuyTransfer(0, 0)),
      inboundPayments = InboundPayments(Some(4000), 4000, 4000, 4000),
      bonuses = Bonuses(1000, 1000, None, "Life Event")
    )

    val response = Await.result(SUT.requestBonusPayment("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  private def doRetrieveBonusPaymentRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.getBonusOrWithdrawal("Z123456", "ABC12345", "123456"), Duration.Inf)

    callback(response)
  }

  private def doRetrieveTransactionRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.getTransaction("Z123456", "ABC12345", "123456"), Duration.Inf)

    callback(response)
  }

  private def doRetrieveBulkPaymentRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.getBulkPayment("Z123456", new DateTime("2018-01-01"), new DateTime("2018-01-01")), Duration.Inf)

    callback(response)
  }

  private def doRetrieveAccountRequest(callback: (DesResponse) => Unit) = {
    val response = Await.result(SUT.getAccountInformation("Z123456", "123456"), Duration.Inf)

    callback(response)
  }

  private def doReportWithdrawalRequest(callback: (DesResponse) => Unit) = {
    val request = SupersededWithdrawalChargeRequest(
      Some(250.00),
      new DateTime("2017-12-06"),
      new DateTime("2018-01-05"),
      1000.00,
      250.00,
      500.00,
      true,
      WithdrawalIncrease(
        "2345678901",
        250.00,
        250.00
      )
    )

    val response = Await.result(SUT.reportWithdrawalCharge("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  val mockHttpPost = mock[HttpPost]
  val mockHttpGet = mock[HttpGet]
  val mockHttpPut = mock[HttpPut]

  implicit val hc = HeaderCarrier()

  object SUT extends DesConnector {
    override val httpPost = mockHttpPost
    override val httpGet = mockHttpGet
    override val httpPut = mockHttpPut
  }
}
