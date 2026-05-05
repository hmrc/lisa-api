/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}
import uk.gov.hmrc.lisaapi.models.*
import uk.gov.hmrc.lisaapi.models.des.*

import java.time.LocalDate
import java.util.UUID

class DesConnectorSpec extends DesConnectorTestHelper {

  val managerPath = "/lifetime-isa/manager"

  private val createInvestorUrl  = s"$managerPath/Z019283/investors"
  private val createAccountUrl   = s"$managerPath/Z019283/accounts"
  private val transferAccountUrl = s"$managerPath/Z019283/accounts"
  private val updateFirstSubUrl  = s"$managerPath/Z019283/accounts/123456789"

  private val closeAccountUrl      = s"$managerPath/Z123456/accounts/ABC12345/close-account"
  private val reinstateAccountUrl  = s"$managerPath/Z123456/accounts/ABC12345/reinstate"
  private val reportLifeEventUrl   = s"$managerPath/Z123456/accounts/ABC12345/life-event"
  private val getLifeEventUrl      = s"$managerPath/Z123456/accounts/ABC12345/life-events/1234567890"
  private val requestBonusUrl      = s"$managerPath/Z123456/accounts/ABC12345/bonus-claim"
  private val getBonusOrWithdrawal = s"$managerPath/Z123456/accounts/ABC12345/transaction/123456"

  private val getTransactionUrl =
    s"$managerPath/Z123456/accounts/ABC12345/transaction/123456/bonusChargeDetails"

  private val withdrawalUrl = s"$managerPath/Z123456/accounts/ABC12345/withdrawal"
  private val getAccountUrl = s"$managerPath/Z123456/accounts/123456"

  private val bulkPaymentUrl =
    "/enterprise/financial-data/ZISA/Z123456/LISA?dateFrom=2018-01-01&dateTo=2018-01-01&onlyOpenItems=false"

  "Create Lisa Investor endpoint" must {
    "return a populated CreateLisaInvestorSuccessResponse when The DES response has a json body that is in the correct format" in {
      stubForPost(createInvestorUrl, CREATED, """{"investorID": "1234567890"}""")
      createInvestorRequest { response =>
        response must be(CreateLisaInvestorSuccessResponse("1234567890"))
      }
    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {
        stubForPost(createInvestorUrl, OK, "")
        createInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        stubForPost(createInvestorUrl, OK, """[1,2,3]""")
        createInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }

      "a 409 is returned with JSON that cannot be parsed as expected response" in {
        stubForPost(createInvestorUrl, CONFLICT, """{"unexpectedField": "value", "notInvestorID": "123"}""")
        createInvestorRequest { response =>
          response must be(DesFailureResponse())
        }
      }
    }

    "return a populated CreateLisaInvestorAlreadyExistsResponse when the investor already exists response is returned" in {
      val investorID = "1234567890"
      stubForPost(createInvestorUrl, CONFLICT, s"""{"investorID": "$investorID"}""")
      createInvestorRequest { response =>
        response must be(CreateLisaInvestorAlreadyExistsResponse(investorID))
      }
    }

    "return a specific DesFailureResponse when a specific failure is returned" in {
      stubForPost(
        createInvestorUrl,
        FORBIDDEN,
        """{"code": "INVESTOR_NOT_FOUND","reason": "The investor details given do not match with HMRC’s records."}"""
      )
      createInvestorRequest { response =>
        response must be(
          DesFailureResponse("INVESTOR_NOT_FOUND", "The investor details given do not match with HMRC’s records.")
        )
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPost(createInvestorUrl, SERVICE_UNAVAILABLE, "")
      createInvestorRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPost(createInvestorUrl, BAD_REQUEST, "")
      createInvestorRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }
  }

  "Create Account endpoint" must {
    "return a populated success response when DES returns 201 created" in {
      stubForPost(createAccountUrl, CREATED, "")
      createAccountRequest { response =>
        response mustBe DesAccountResponse("9876543210")
      }
    }

    "return a generic failure response" when {
      "the DES response is not 201 created and has no json body" in {
        stubForPost(createAccountUrl, OK, "")
        createAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is not 201 created and has a json body that is not in the correct format" in {
        stubForPost(createAccountUrl, GATEWAY_TIMEOUT, """{"problem": "service unavailable"}""")
        createAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPost(createAccountUrl, SERVICE_UNAVAILABLE, "")
      createAccountRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPost(createAccountUrl, BAD_REQUEST, "")
      createAccountRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }

    "return a type-appropriate failure response when a specific failure is returned" in {
      stubForPost(
        createAccountUrl,
        FORBIDDEN,
        """{"code": "INVESTOR_NOT_FOUND", "reason": "The investorId given does not match with HMRC’s records."}"""
      )

      createAccountRequest { response =>
        response mustBe DesFailureResponse(
          "INVESTOR_NOT_FOUND",
          "The investorId given does not match with HMRC’s records."
        )
      }
    }
  }

  "Transfer Account endpoint" must {
    "return a populated success response when DES returns 201 created" in {
      stubForPost(transferAccountUrl, CREATED, "")
      transferAccountRequest { response =>
        response mustBe DesAccountResponse("9876543210")
      }
    }

    "return a generic failure response" when {
      "the DES response is not 201 created and has no json body" in {
        stubForPost(transferAccountUrl, OK, "")
        transferAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is not 201 created and has a json body that is not in the correct format" in {
        stubForPost(transferAccountUrl, GATEWAY_TIMEOUT, """{"problem": "service unavailable"}""")
        transferAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a type-appropriate failure response when a specific failure is returned" in {
      stubForPost(
        transferAccountUrl,
        FORBIDDEN,
        """{"code": "INVESTOR_NOT_FOUND", "reason": "The investorId given does not match with HMRC’s records."}"""
      )
      transferAccountRequest { response =>
        response mustBe DesFailureResponse(
          "INVESTOR_NOT_FOUND",
          "The investorId given does not match with HMRC’s records."
        )
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPost(transferAccountUrl, SERVICE_UNAVAILABLE, "")
      transferAccountRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPost(transferAccountUrl, BAD_REQUEST, "")
      transferAccountRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }

  }

  "Close Lisa Account endpoint" must {
    "return a DesEmptySuccessResponse when DES returns 200 ok" in {
      stubForPost(closeAccountUrl, OK, "")
      closeAccountRequest { response =>
        response mustBe DesEmptySuccessResponse
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPost(closeAccountUrl, SERVICE_UNAVAILABLE, "")
      closeAccountRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPost(closeAccountUrl, BAD_REQUEST, "")
      closeAccountRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }

    "return a DesFailureResponse when any other response is received" in {
      stubForPost(closeAccountUrl, GATEWAY_TIMEOUT, "")
      closeAccountRequest { response =>
        response mustBe DesFailureResponse()
      }
    }
  }

  "Reinstate Lisa Account endpoint" must {
    "return a populated success response when DES returns 200 ok" in {
      stubForPut(reinstateAccountUrl, OK, """{"code": "SUCCESS", "reason": "Account successfully reinstated"}""")
      reinstateAccountRequest { response =>
        response mustBe DesReinstateAccountSuccessResponse("SUCCESS", "Account successfully reinstated")
      }
    }

    "return a generic failure response when a 200 is returned, but the DES response has no json body" in {
      stubForPut(reinstateAccountUrl, OK, "")
      reinstateAccountRequest { response =>
        response mustBe DesFailureResponse()
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPut(reinstateAccountUrl, SERVICE_UNAVAILABLE, "")
      reinstateAccountRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPut(reinstateAccountUrl, BAD_REQUEST, "")
      reinstateAccountRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }

    "return a DesFailureResponse when a 500 is returned (not 200/400/503)" in {
      stubForPut(reinstateAccountUrl, INTERNAL_SERVER_ERROR, "")
      reinstateAccountRequest { response =>
        response mustBe DesFailureResponse()
      }
    }

  }

  "Update First Subscription date endpoint" must {
    "return a populated DesUpdateSubscriptionSuccessResponse when the DES response has a json body that is in the correct format" in {
      stubForPut(
        updateFirstSubUrl,
        OK,
        """{"code": "INVESTOR_ACCOUNT_NOW_VOID", "reason": "Date of first Subscription updated successfully, but as a result of the date change the account has subsequently been voided"}"""
      )
      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesUpdateSubscriptionSuccessResponse(
          "INVESTOR_ACCOUNT_NOW_VOID",
          "Date of first Subscription updated successfully, but as a result of the date change the account has subsequently been voided"
        )
      }
    }

    "return a failure response" when {
      "the DES response has no json body" in {
        stubForPut(updateFirstSubUrl, OK, "")
        updateFirstSubscriptionDateRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "status is 201 and json is invalid" in {
        stubForPut(
          updateFirstSubUrl,
          CREATED,
          """{"code": "UPDATED_AND_ACCOUNT_VOIDED", "message": "LISA Account firstSubscriptionDate has been updated successfully"}"""
        )
        updateFirstSubscriptionDateRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a DesUnavailableResponse when a 503 response is returned" in {
      stubForPut(updateFirstSubUrl, SERVICE_UNAVAILABLE, "")
      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 response is returned" in {
      stubForPut(updateFirstSubUrl, BAD_REQUEST, "")
      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }

    "return a DesTransactionExistResponse when a 409 response is returned with json in the correct format" in {
      stubForPut(
        updateFirstSubUrl,
        CONFLICT,
        """{"code": "x", "reason": "xx", "transactionID": "87654321"}"""
      )
      updateFirstSubscriptionDateRequest { response =>
        response mustBe DesTransactionExistResponse(code = "x", reason = "xx", transactionID = "87654321")
      }
    }

  }

  "Report Life Event endpoint" must {
    "return a populated DesSuccessResponse when the DES response has a json body that is in the correct format" in {
      stubForPost(reportLifeEventUrl, CREATED, """{"lifeEventID": "87654321"}""")
      reportLifeEventRequest { response =>
        response mustBe DesLifeEventResponse("87654321")
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPost(reportLifeEventUrl, SERVICE_UNAVAILABLE, "")
      reportLifeEventRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a generic DesFailureResponse" when {
      "the response json is invalid" in {
        stubForPost(reportLifeEventUrl, CREATED, """{"lifeEvent": "87654321"}""")
        reportLifeEventRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the response has no json body" in {
        stubForPost(reportLifeEventUrl, CREATED, "")
        reportLifeEventRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a populated DesFailureResponse when a LIFE_EVENT_INAPPROPRIATE failure is returned" in {
      stubForPost(
        reportLifeEventUrl,
        FORBIDDEN,
        """{"code": "LIFE_EVENT_INAPPROPRIATE","reason": "The life event conflicts with previous life event reported."}"""
      )
      reportLifeEventRequest { response =>
        response mustBe DesFailureResponse(
          "LIFE_EVENT_INAPPROPRIATE",
          "The life event conflicts with previous life event reported."
        )
      }
    }
  }

  "Retrieve Life Event endpoint" must {
    "return a Left of DesUnavailableResponse when a 503 is returned" in {
      stubForGet(getLifeEventUrl, SERVICE_UNAVAILABLE, "")
      retrieveLifeEventRequest { response =>
        response mustBe Left(DesUnavailableResponse)
      }
    }

    "return a Left of DesFailureResponse" when {
      "a specific failure is returned" in {
        stubForGet(
          getLifeEventUrl,
          CONFLICT,
          """{ "code": "ERROR_CODE", "reason" : "ERROR MESSAGE" }""".stripMargin
        )

        retrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse("ERROR_CODE", "ERROR MESSAGE"))
        }
      }

      "the response has no json body" in {
        stubForGet(getLifeEventUrl, OK, "")
        retrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse())
        }
      }

      "the response is badly formed" in {
        stubForGet(getLifeEventUrl, OK, """{"test": "test"}""")
        retrieveLifeEventRequest { response =>
          response mustBe Left(DesFailureResponse())
        }
      }
    }

    "return a Right of Seq GetLifeEventItem when DES returns successfully" in {
      stubForGet(
        getLifeEventUrl,
        OK,
        """[{ "lifeEventId": "1234567890", "lifeEventType": "STATUTORY_SUBMISSION", "lifeEventDate": "2018-04-05" }]"""
      )

      retrieveLifeEventRequest { response =>
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

  "Request Bonus Payment endpoint" must {
    "return a populated DesTransactionResponse when the DES response has a json body that is in the correct format" in {
      stubForPost(requestBonusUrl, CREATED, """{"transactionID": "87654321","message": "On Time"}""")
      requestBonusPaymentRequest { response =>
        response mustBe DesTransactionResponse("87654321", Some("On Time"))
      }
    }

    "return a populated DesTransactionExistResponse when the DES response returns a 409 with a json body that is in the correct format" in {
      stubForPost(requestBonusUrl, CONFLICT, """{"code": "x", "reason": "xx", "transactionID": "87654321"}""")
      requestBonusPaymentRequest { response =>
        response mustBe DesTransactionExistResponse(code = "x", reason = "xx", transactionID = "87654321")
      }
    }

    "return the default DesFailureResponse" when {
      "the DES response has no json body" in {
        stubForPost(requestBonusUrl, OK, "")
        requestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        stubForPost(requestBonusUrl, CREATED, """[1,2,3]""")
        requestBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a specific DesFailureResponse when a specific failure is returned" in {
      stubForPost(
        requestBonusUrl,
        NOT_FOUND,
        """{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}"""
      )
      requestBonusPaymentRequest { response =>
        response mustBe DesFailureResponse(
          "LIFE_EVENT_DOES_NOT_EXIST",
          "The lifeEventId does not match with HMRC’s records."
        )
      }
    }

    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForPost(requestBonusUrl, SERVICE_UNAVAILABLE, "")
      requestBonusPaymentRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesFailureResponse when the connection fails" in {
      server.stubFor(
        post(urlEqualTo(requestBonusUrl))
          .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK))
      )
      requestBonusPaymentRequest { response =>
        response mustBe DesFailureResponse()
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPost(requestBonusUrl, BAD_REQUEST, "")
      requestBonusPaymentRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }
  }

  "Retrieve Bonus Payment endpoint" must {
    "return a DesUnavailableResponse when a 503 is returned" in {
      stubForGet(getBonusOrWithdrawal, SERVICE_UNAVAILABLE, "")
      retrieveBonusPaymentRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesFailureResponse" when {
      "a specific failure is returned" in {
        stubForGet(
          getBonusOrWithdrawal,
          CONFLICT,
          """{
            | "code": "ERROR_CODE",
            | "reason" : "ERROR MESSAGE"
            |}""".stripMargin
        )

        retrieveBonusPaymentRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }

      "the response has no json body" in {
        stubForGet(getBonusOrWithdrawal, OK, "")
        retrieveBonusPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a GetBonusResponse when DES returns successfully" in {
      stubForGet(getBonusOrWithdrawal, OK, validBonusPaymentResponseJson)
      retrieveBonusPaymentRequest { response =>
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

  "Retrieve Transaction endpoint" must {
    "return a unavailable response when a 503 is returned" in {
      stubForGet(getTransactionUrl, SERVICE_UNAVAILABLE, "")
      retrieveTransactionRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a failure response" when {
      "the DES response is a failure response" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{ "code": "ERROR_CODE", "reason" : "ERROR MESSAGE" }""".stripMargin
        )

        retrieveTransactionRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }

      "the DES response has no json body" in {
        stubForGet(getTransactionUrl, OK, "")
        retrieveTransactionRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is invalid" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{  "status": "Due" }""".stripMargin
        )
        retrieveTransactionRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response" when {
      "the DES response is a valid collected Pending transaction" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{
            | "paymentStatus": "PENDING",
            | "paymentDate": "2000-01-01",
            | "paymentReference": "002630000994",
            | "paymentAmount": 2.00
            |}""".stripMargin
        )

        retrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPending(
            paymentDueDate = LocalDate.parse("2000-01-01"),
            paymentReference = Some("002630000994"),
            paymentAmount = Some(2.0)
          )
        }
      }

      "the DES response is a valid paid Pending transaction" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{ "paymentStatus": "PENDING", "paymentDate": "2000-01-01" }""".stripMargin
        )

        retrieveTransactionRequest { response =>
          response mustBe DesGetTransactionPending(
            paymentDueDate = LocalDate.parse("2000-01-01"),
            paymentReference = None,
            paymentAmount = None
          )
        }
      }

      "the DES response is a valid Paid transaction" in {
        stubForGet(
          getTransactionUrl,
          OK,
          """{
            | "paymentStatus": "PAID",
            | "paymentDate": "2000-01-01",
            | "paymentReference": "002630000993",
            | "paymentAmount": 1.00
            |}""".stripMargin
        )

        retrieveTransactionRequest { response =>
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

    "return an unavailable response when a 503 is returned" in {
      stubForGet(bulkPaymentUrl, SERVICE_UNAVAILABLE, "")
      retrieveBulkPaymentRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a failure response" when {
      "the DES response is a failure response" in {
        stubForGet(
          bulkPaymentUrl,
          OK,
          """{ "code": "ERROR_CODE",  "reason" : "ERROR MESSAGE" }""".stripMargin
        )
        retrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }

      "the DES response has no json body" in {
        stubForGet(bulkPaymentUrl, OK, "")
        retrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is missing a processingDate" in {
        stubForGet(bulkPaymentUrl, OK, "{}")
        retrieveBulkPaymentRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response when the DES response has the expected json response" in {
      val responseString =
        """{
            | "processingDate": "2017-03-07T09:30:00.000Z",
            | "idNumber": "Z5555",
            | "financialTransactions": [
            |   {
            |     "clearedAmount": -1000,
            |     "items": [
            |       {
            |         "clearingSAPDocument": "ABC123456789",
            |         "clearingDate": "2017-06-01"
            |       }
            |     ]
            |   },
            |   {
            |     "outstandingAmount": -1500.55,
            |     "items": [
            |       {
            |         "dueDate": "2017-07-01"
            |       }
            |     ]
            |   }
            | ]
            |}""".stripMargin

      stubForGet(bulkPaymentUrl, OK, responseString)

      retrieveBulkPaymentRequest { response =>
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

    "return a not found response" when {
      "the DES response has no financial transactions field" in {
        val responseString =
          """{ "processingDate": "2017-03-07T09:30:00.000Z", "idNumber": "Z1234" }""".stripMargin

        stubForGet(bulkPaymentUrl, OK, responseString)
        retrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }

      "the DES response has no id number field" in {
        val responseString =
          """{ "processingDate": "2017-03-07T09:30:00.000Z", "financialTransactions": [] }""".stripMargin

        stubForGet(bulkPaymentUrl, OK, responseString)

        retrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }

      "the DES response has no id number or financial transactions field" in {
        val responseString =
          """{ "processingDate": "2017-03-07T09:30:00.000Z" }""".stripMargin

        stubForGet(bulkPaymentUrl, OK, responseString)

        retrieveBulkPaymentRequest { response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
    }
  }

  "Retrieve Account endpoint" must {
    "return a unavailable response when a 503 is returned" in {
      stubForGet(getAccountUrl, SERVICE_UNAVAILABLE, "")
      retrieveAccountRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a failure response" when {
      "the DES response is a failure response" in {
        stubForGet(
          getAccountUrl,
          OK,
          """{ "code": "ERROR_CODE", "reason" : "ERROR MESSAGE" }""".stripMargin
        )

        retrieveAccountRequest { response =>
          response mustBe DesFailureResponse("ERROR_CODE", "ERROR MESSAGE")
        }
      }

      "the DES response has no json body" in {
        stubForGet(getAccountUrl, OK, "")
        retrieveAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response is missing required fields" in {
        stubForGet(getAccountUrl, OK, "{}")
        retrieveAccountRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a success response when the DES response is the appropriate json response" in {
      val responseString =
        """{
            | "investorId": "1234567890",
            | "status": "OPEN",
            | "creationDate": "2016-01-01",
            | "creationReason": "REINSTATED",
            | "hmrcClosureDate": "2016-02-01",
            | "accountClosureReason": "TRANSFERRED_OUT",
            | "transferInDate": "2016-03-01",
            | "transferOutDate": "2016-04-01",
            | "xferredFromAccountId": "123abc789ABC34567890",
            | "xferredFromLmrn": "Z123453",
            | "lisaManagerClosureDate": "2016-05-01",
            | "subscriptionStatus": "AVAILABLE",
            | "firstSubscriptionDate": "2016-01-06"
            |}""".stripMargin

      stubForGet(getAccountUrl, OK, responseString)

      retrieveAccountRequest { response =>
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

    "return a subscriptionStatus of AVAILABLE when there is no subscriptionStatus in the json response from DES" in {
      val responseString =
        """{
            | "investorId": "1234567890",
            | "status": "OPEN",
            | "creationDate": "2016-01-01",
            | "creationReason": "REINSTATED",
            | "hmrcClosureDate": "2016-02-01",
            | "accountClosureReason": "TRANSFERRED_OUT",
            | "transferInDate": "2016-03-01",
            | "transferOutDate": "2016-04-01",
            | "xferredFromAccountId": "123abc789ABC34567890",
            | "xferredFromLmrn": "Z123453",
            | "lisaManagerClosureDate": "2016-05-01",
            | "firstSubscriptionDate": "2016-01-06"
            |}""".stripMargin

      stubForGet(getAccountUrl, OK, responseString)

      retrieveAccountRequest { response =>
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

  "Report withdrawal endpoint" must {
    "post to the withdrawal endpoint" in {
      stubForPost(withdrawalUrl, CREATED, """{"transactionID": "87654321","message": "On Time"}""")
      reportWithdrawalRequest { _ =>
        server.verify(postRequestedFor(urlEqualTo(withdrawalUrl)))
      }
    }

    "return a populated DesTransactionResponse when the DES response has a json body that is in the correct format" in {
      stubForPost(withdrawalUrl, CREATED, """{"transactionID": "87654321","message": "On Time"}""")
      reportWithdrawalRequest { response =>
        response mustBe DesTransactionResponse("87654321", Some("On Time"))
      }
    }

    "return a populated DesTransactionExistResponse" when {
      "the DES response has status CONFLICT" in {
        stubForPost(
          withdrawalUrl,
          CONFLICT,
          """{"code": "WITHDRAWAL_CHARGE_ALREADY_EXISTS","reason": "A withdrawal charge with these details has already been requested for this investor","investorTransactionID":"2345678901"}"""
        )

        reportWithdrawalRequest { response =>
          response mustBe DesWithdrawalChargeAlreadyExistsResponse(
            "WITHDRAWAL_CHARGE_ALREADY_EXISTS",
            "A withdrawal charge with these details has already been requested for this investor",
            "2345678901"
          )
        }
      }

      "the DES response has status FORBIDDEN and a transactionID value in the json body" in {
        stubForPost(
          withdrawalUrl,
          FORBIDDEN,
          """{"code": "SUPERSEDED_TRANSACTION_ID_ALREADY_SUPERSEDED","reason": "This withdrawal charge has already been superseded","supersededTransactionByID": "2345678901"}"""
        )

        reportWithdrawalRequest { response =>
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
        stubForPost(withdrawalUrl, OK, "")
        reportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a json body that is in an incorrect format" in {
        stubForPost(withdrawalUrl, CREATED, """[1,2,3]""")
        reportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }

      "the DES response has a html body instead of JSON format" in {
        server.stubFor(
          post(urlEqualTo(withdrawalUrl))
            .willReturn(aResponse().withStatus(CREATED).withBody("<!DOCTYPE html>"))
        )

        reportWithdrawalRequest { response =>
          response mustBe DesFailureResponse()
        }
      }
    }

    "return a specific DesFailureResponse when a specific failure is returned" in {
      stubForPost(
        withdrawalUrl,
        NOT_FOUND,
        """{"code": "LIFE_EVENT_DOES_NOT_EXIST","reason": "The lifeEventId does not match with HMRC’s records."}"""
      )

      reportWithdrawalRequest { response =>
        response mustBe DesFailureResponse(
          "LIFE_EVENT_DOES_NOT_EXIST",
          "The lifeEventId does not match with HMRC’s records."
        )
      }
    }

    "return a DesUnavailableResponse when a 503 is returned when a 503 is returned" in {
      stubForPost(withdrawalUrl, SERVICE_UNAVAILABLE, "")
      reportWithdrawalRequest { response =>
        response mustBe DesUnavailableResponse
      }
    }

    "return a DesBadRequestResponse when a 400 is returned" in {
      stubForPost(withdrawalUrl, BAD_REQUEST, "")
      reportWithdrawalRequest { response =>
        response mustBe DesBadRequestResponse
      }
    }
  }

  "correlationId" must {

    "reuse the requestId given it matches the correlation id pattern, and return a valid UUID" in {
      implicit val hc: HeaderCarrier =
        HeaderCarrier(requestId = Some(RequestId("abcd1234-ab12-cd34-ef56")))

      val result = desConnector.correlationId

      result must startWith("abcd1234-ab12-cd34-ef56-")
      UUID.fromString(result) // throws if invalid UUID
    }

    "make a new, valid UUID when the requestId does not match the correlation id pattern" in {
      implicit val hc: HeaderCarrier =
        HeaderCarrier(requestId = Some(RequestId("not-a-valid-correlation-id-pattern")))

      val result = desConnector.correlationId

      UUID.fromString(result)
    }

    "make a new, valid UUID when the requestId is empty" in {
      implicit val hc: HeaderCarrier = HeaderCarrier(requestId = None)

      val result = desConnector.correlationId

      UUID.fromString(result)
    }
  }

}
