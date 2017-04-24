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

package unit.controllers

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, ShouldMatchers, WordSpec}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.AccountController
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}

import scala.concurrent.Future


class AccountControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC12345"

  override def beforeEach() {
    reset(mockAuditService)
  }


  val createAccountJson = """{
                            |  "investorID" : "9876543210",
                            |  "accountID" :"8765432100",
                            |  "creationReason" : "New",
                            |  "firstSubscriptionDate" : "2011-03-23"
                            |}""".stripMargin

  val createAccountJsonWithTransfer = """{
                                        |  "investorID" : "9876543210",
                                        |  "accountID" :"8765432100",
                                        |  "creationReason" : "New",
                                        |  "firstSubscriptionDate" : "2011-03-23",
                                        |  "transferAccount": {
                                        |    "transferredFromAccountID": "Z543210",
                                        |    "transferredFromLMRN": "Z543333",
                                        |    "transferInDate": "2015-12-13"
                                        |  }
                                        |}""".stripMargin

  val createAccountJsonWithInvalidTransfer = """{
                                               |  "investorID" : "9876543210",
                                               |  "accountID" :"8765432100",
                                               |  "creationReason" : "New",
                                               |  "firstSubscriptionDate" : "2011-03-23",
                                               |  "transferAccount": "X"
                                               |}""".stripMargin

  val transferAccountJson = """{
                              |  "investorID" : "9876543210",
                              |  "accountID" :"8765432100",
                              |  "creationReason" : "Transferred",
                              |  "firstSubscriptionDate" : "2011-03-23",
                              |  "transferAccount": {
                              |    "transferredFromAccountID": "Z543210",
                              |    "transferredFromLMRN": "Z543333",
                              |    "transferInDate": "2015-12-13"
                              |  }
                              |}""".stripMargin

  val transferAccountJsonIncomplete = """{
                                        |  "investorID" : "9876543210",
                                        |  "accountID" :"8765432100",
                                        |  "creationReason" : "Transferred",
                                        |  "firstSubscriptionDate" : "2011-03-23"
                                        |}""".stripMargin

  val closeAccountJson = """{"accountClosureReason" : "Voided", "closureDate" : "2000-06-23"}"""

  "The Create / Transfer Account endpoint" must {

    "audit an accountCreated event" when {
      "submitted a valid create account request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse("AB123456")))
        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23"
            ))(SUT.hc)
        }
      }
    }

    "audit an accountNotCreated event" when {
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountNotCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "reasonNotCreated" -> "INVESTOR_NOT_FOUND"
            ))(SUT.hc)
        }
      }
      "the data service returns a CreateLisaAccountInvestorNotEligibleResponse" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotEligibleResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountNotCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "reasonNotCreated" -> "INVESTOR_ELIGIBILITY_CHECK_FAILED"
            )
          )(SUT.hc)
        }
      }
      "the data service returns a CreateLisaAccountInvestorComplianceCheckFailedResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorComplianceCheckFailedResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountNotCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "reasonNotCreated" -> "INVESTOR_COMPLIANCE_CHECK_FAILED"
            )
          )(SUT.hc)
        }
      }
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountNotCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID"
            )
          )(SUT.hc)
        }
      }
      "the data service returns a CreateLisaAccountAlreadyExistsResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountAlreadyExistsResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountNotCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_EXISTS"
            )
          )(SUT.hc)
        }
      }
      "the data service returns an error for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = "accountNotCreated",
            path=s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "reasonNotCreated" -> "INTERNAL_SERVER_ERROR"
            )
          )(SUT.hc)
        }
      }
    }

    "audit an accountTransferred event" when {
      "submitted a valid transfer account request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse("AB123456")))
        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13"
            ))(SUT.hc)
        }
      }
    }

    "audit and accountNotTransferred event" when {
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13",
              "reasonNotCreated" -> "INVESTOR_NOT_FOUND"
            ))(SUT.hc)
        }
      }

      "the data service returns a CreateLisaAccountInvestorComplianceCheckFailedResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorComplianceCheckFailedResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13",
              "reasonNotCreated" -> "INVESTOR_COMPLIANCE_CHECK_FAILED"
            ))(SUT.hc)
        }
      }

      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13",
              "reasonNotCreated" -> "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST"
            ))(SUT.hc)
        }
      }

      "the data service returns a CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID"
            ))(SUT.hc)
        }
      }

      "the data service returns a CreateLisaAccountAlreadyExistsResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountAlreadyExistsResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_EXISTS"
            ))(SUT.hc)
        }
      }

      "the data service returns an error for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotTransferred",
            path = s"/manager/$lisaManager/accounts",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorID" -> "9876543210",
              "accountID" -> "8765432100",
              "firstSubscriptionDate" -> "2011-03-23",
              "transferredFromAccountID" -> "Z543210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> "2015-12-13",
              "reasonNotCreated" -> "INTERNAL_SERVER_ERROR"
            ))(SUT.hc)
        }
      }
    }

    "return with status 201 created and an account Id" when {
      "submitted a valid create account request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse("AB123456")))
        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "accountId").as[String] mustBe ("AB123456")
        }
      }
      "submitted a valid transfer account request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse("AB123456")))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "accountId").as[String] mustBe ("AB123456")
        }
      }
    }

    "return with status 400 bad request and a code of BAD_REQUEST" when {
      "invalid json is sent" in {
        val invalidJson = createAccountJson.replace("9876543210", "")

        doCreateOrTransferRequest(invalidJson) { res =>
          status(res) mustBe (BAD_REQUEST)
          (contentAsJson(res) \ "code").as[String] mustBe ("BAD_REQUEST")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_NOT_FOUND" when {
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>

          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_NOT_FOUND")
        }
      }
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_NOT_FOUND")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ELIGIBILITY_CHECK_FAILED" when {
      "the data service returns a CreateLisaAccountInvestorNotEligibleResponse" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotEligibleResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ELIGIBILITY_CHECK_FAILED")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_COMPLIANCE_CHECK_FAILED" when {
      "the data service returns a CreateLisaAccountInvestorComplianceCheckFailedResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorComplianceCheckFailedResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_COMPLIANCE_CHECK_FAILED")
        }
      }
      "the data service returns a CreateLisaAccountInvestorComplianceCheckFailedResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorComplianceCheckFailedResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_COMPLIANCE_CHECK_FAILED")
        }
      }
    }

    "return with status 403 forbidden and a code of PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST" when {
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST")
        }
      }
    }

    "return with status 403 forbidden and a code of TRANSFER_ACCOUNT_DATA_NOT_PROVIDED" when {
      "sent a transfer request json with no transferAccount data" in {
        doCreateOrTransferRequest(transferAccountJsonIncomplete) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("TRANSFER_ACCOUNT_DATA_NOT_PROVIDED")
        }
      }
    }

    "return with status 403 forbidden and a code of TRANSFER_ACCOUNT_DATA_PROVIDED" when {
      "sent a create request json with full transferAccount data" in {
        doCreateOrTransferRequest(createAccountJsonWithTransfer) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("TRANSFER_ACCOUNT_DATA_PROVIDED")
        }
      }
      "sent a create request json with partial transferAccount data" in {
        doCreateOrTransferRequest(createAccountJsonWithTransfer.replace("\"transferredFromAccountID\": \"Z543210\",", "")) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("TRANSFER_ACCOUNT_DATA_PROVIDED")
        }
      }
      "sent a create request json with invalid transferAccount data" in {
        doCreateOrTransferRequest(createAccountJsonWithInvalidTransfer) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("TRANSFER_ACCOUNT_DATA_PROVIDED")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" when {
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID")
        }
      }
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID")
        }
      }
    }

    "return with status 409 conflict and a code of INVESTOR_ACCOUNT_ALREADY_EXISTS" when {
      "the data service returns a CreateLisaAccountAlreadyExistsResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountAlreadyExistsResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (CONFLICT)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_EXISTS")
        }
      }
      "the data service returns a CreateLisaAccountAlreadyExistsResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountAlreadyExistsResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (CONFLICT)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_EXISTS")
        }
      }
    }

    "return with status 500 internal server error" when {
      "the data service returns an error for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
      "the data service returns an error for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
      "the data service returns a CreateLisaAccountInvestorNotEligibleResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotEligibleResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }

  }

  "The Close Account endpoint" must {

    "Audit an account closed event" when {

      "return with status 200 ok" when {
        "submitted a valid close account request" in {
          when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse("AB123456")))

          doSyncCloseRequest(closeAccountJson) { res =>
            verify(mockAuditService).audit(
              auditType = "accountClosed",
              path=s"/manager/$lisaManager/accounts/AB123456/close-account",
              auditData = Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountClosureReason" -> "Voided",
                "closureDate" -> "2000-06-23",
                "accountID" -> "AB123456"
               ))(SUT.hc)
          }
        }
      }
    }

    "Audit an accountNotClosed event" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyClosedResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotClosed",
            path=s"/manager/$lisaManager/accounts/ABC12345/close-account",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "Voided",
              "closureDate" -> "2000-06-23",
              "accountID" -> "ABC12345",
              "reasonNotClosed" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            ))(SUT.hc)
        }
      }

      "the data service returns a CloseLisaAccountNotFoundResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountNotFoundResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = "accountNotClosed",
            path=s"/manager/$lisaManager/accounts/ABC12345/close-account",
            auditData = Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "Voided",
              "closureDate" -> "2000-06-23",
              "accountID" -> "ABC12345",
              "reasonNotClosed" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            ))(SUT.hc)

        }
      }

      "return with status 500 internal server error" when {
        "the data service returns an error" in {
          when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountErrorResponse))

          doSyncCloseRequest(closeAccountJson) { res =>
            verify(mockAuditService).audit(
              auditType = "accountNotClosed",
              path=s"/manager/$lisaManager/accounts/ABC12345/close-account",
              auditData = Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountClosureReason" -> "Voided",
                "closureDate" -> "2000-06-23",
                "accountID" -> "ABC12345",
                "reasonNotClosed" -> "INTERNAL_SERVER_ERROR"
              ))(SUT.hc)

          }
        }
      }
    }
    "return with status 200 ok" when {
      "submitted a valid close account request" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse("AB123456")))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (OK)
        }
      }
    }

    /* TODO: 403 & WRONG_LISA_MANAGER */

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyClosedResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }

    "return with status 404 forbidden and a code of INVESTOR_ACCOUNTID_NOT_FOUND" when {
      "the data service returns a CloseLisaAccountNotFoundResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountNotFoundResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (NOT_FOUND)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
        }
      }
    }

    "return with status 400 bad request" when {
      "submitted an invalid close account request" in {
        doCloseRequest(closeAccountJson.replace("Voided", "X")) { res =>
          status(res) mustBe (BAD_REQUEST)
        }
      }
    }

    "return with status 500 internal server error" when {
      "the data service returns an error" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountErrorResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }

  }

  def doCreateOrTransferRequest(jsonString: String)(callback: (Future[Result]) => Unit) {
    val res = SUT.createOrTransferLisaAccount(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncCreateOrTransferRequest(jsonString: String)(callback: (Result) => Unit) {
    val res = await(SUT.createOrTransferLisaAccount(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  def doCloseRequest(jsonString: String)(callback: (Future[Result]) => Unit) {
    val res = SUT.closeLisaAccount(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncCloseRequest(jsonString: String)(callback: Result => Unit) {
    val res = await(SUT.closeLisaAccount(lisaManager, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  val mockService: AccountService = mock[AccountService]
  val mockAuditService: AuditService = mock[AuditService]
  val SUT = new AccountController{
    override val service: AccountService = mockService
    override val auditService: AuditService = mockAuditService
  }

}