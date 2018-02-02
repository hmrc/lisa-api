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

package unit.controllers

import org.joda.time.DateTime
import org.mockito.Matchers.{eq => matchersEquals, _}
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class AccountControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val mockAuthCon = mock[LisaAuthConnector]

  override def beforeEach() {
    reset(mockAuditService)
  }
  
  val validDate = "2017-04-06"

  val createAccountJson = s"""{
                            |  "investorId" : "9876543210",
                            |  "accountId" :"8765/432100",
                            |  "creationReason" : "New",
                            |  "firstSubscriptionDate" : "$validDate"
                            |}""".stripMargin

  val createAccountJsonWithTransfer = s"""{
                                        |  "investorId" : "9876543210",
                                        |  "accountId" :"8765/432100",
                                        |  "creationReason" : "New",
                                        |  "firstSubscriptionDate" : "$validDate",
                                        |  "transferAccount": {
                                        |    "transferredFromAccountId": "Z54/3210",
                                        |    "transferredFromLMRN": "Z543333",
                                        |    "transferInDate": "$validDate"
                                        |  }
                                        |}""".stripMargin

  val createAccountJsonWithInvalidTransfer = s"""{
                                               |  "investorId" : "9876543210",
                                               |  "accountId" :"8765/432100",
                                               |  "creationReason" : "New",
                                               |  "firstSubscriptionDate" : "$validDate",
                                               |  "transferAccount": "X"
                                               |}""".stripMargin

  val transferAccountJson = s"""{
                              |  "investorId" : "9876543210",
                              |  "accountId" :"8765/432100",
                              |  "creationReason" : "Transferred",
                              |  "firstSubscriptionDate" : "$validDate",
                              |  "transferAccount": {
                              |    "transferredFromAccountId": "Z54/3210",
                              |    "transferredFromLMRN": "Z543333",
                              |    "transferInDate": "$validDate"
                              |  }
                              |}""".stripMargin

  val transferAccountJsonIncomplete = s"""{
                                        |  "investorId" : "9876543210",
                                        |  "accountId" :"8765/432100",
                                        |  "creationReason" : "Transferred",
                                        |  "firstSubscriptionDate" : "$validDate"
                                        |}""".stripMargin

  val closeAccountJson = s"""{"accountClosureReason" : "All funds withdrawn", "closureDate" : "$validDate"}"""

  "The Create / Transfer Account endpoint" must {

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))

    "audit an accountCreated event" when {
      "submitted a valid create account request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))
        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountCreated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate"
            )))(any())
        }
      }
    }

    "audit an accountNotCreated event" when {
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotCreated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_NOT_FOUND"
            )))(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorNotEligibleResponse" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotEligibleResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotCreated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_ELIGIBILITY_CHECK_FAILED"
            ))
          )(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorComplianceCheckFailedResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorComplianceCheckFailedResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotCreated"),
            path=matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_COMPLIANCE_CHECK_FAILED"
            ))
          )(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a create request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path=matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST"
            ))
          )(any())
        }
      }
      "the data service returns a CreateLisaAccountAlreadyExistsResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountAlreadyExistsResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotCreated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_EXISTS"
            ))
          )(any())
        }
      }
      "the data service returns an error for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doSyncCreateOrTransferRequest(createAccountJson) { _ =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotCreated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "reasonNotCreated" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
    }

    "audit an accountTransferred event" when {
      "submitted a valid transfer account request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))
        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate"
            )))(any())
        }
      }
    }

    "audit an accountNotTransferred event" when {
      "the data service returns a CreateLisaAccountInvestorNotFoundResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorNotFoundResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_NOT_FOUND"
            )))(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorComplianceCheckFailedResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorComplianceCheckFailedResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_COMPLIANCE_CHECK_FAILED"
            )))(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST"
            )))(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorAccountAlreadyClosed for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            )))(any())
        }
      }
      "the data service returns a CreateLisaAccountInvestorAccountAlreadyVoid for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyVoidResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_VOID"
            )))(any())
        }
      }
      "the data service returns a CreateLisaAccountAlreadyExistsResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountAlreadyExistsResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "INVESTOR_ACCOUNT_ALREADY_EXISTS"
            )))(any())
        }
      }
      "the data service returns an error for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountErrorResponse))

        doSyncCreateOrTransferRequest(transferAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotTransferred"),
            path = matchersEquals(s"/manager/$lisaManager/accounts"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "investorId" -> "9876543210",
              "accountId" -> "8765/432100",
              "firstSubscriptionDate" -> s"$validDate",
              "transferredFromAccountId" -> "Z54/3210",
              "transferredFromLMRN" -> "Z543333",
              "transferInDate" -> s"$validDate",
              "reasonNotCreated" -> "INTERNAL_SERVER_ERROR"
            )))(any())
        }
      }
    }

    "return with status 201 created and an account Id" when {
      "submitted a valid create account request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))
        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "accountId").as[String] mustBe (accountId)
        }
      }
      "submitted a valid transfer account request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (CREATED)
          (contentAsJson(res) \ "data" \ "accountId").as[String] mustBe (accountId)
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
      "invalid lmrn is sent" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))

        doCreateOrTransferRequest(createAccountJson, "ZZ1234") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "invalid accountId is sent" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))

        doCreateOrTransferRequest(transferAccountJson.replace("/", "\\\\")) { res =>
          status(res) mustBe (BAD_REQUEST)

          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "BAD_REQUEST"
          (json \ "message").as[String] mustBe "Bad Request"

          val errors = (json \ "errors").as[List[JsObject]]

          errors must contain(Json.obj("code" -> "INVALID_FORMAT", "message" -> "Invalid format has been used", "path" -> "/accountId"))
          errors must contain(Json.obj("code" -> "INVALID_FORMAT", "message" -> "Invalid format has been used", "path" -> "/transferAccount/transferredFromAccountId"))
        }
      }
    }
    
    "return with status 403 forbidden and a code of FORBIDDEN" when {
      "given a firstSubscriptionDate prior to 6 April 2017" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))

        val invalidJson = createAccountJson.replace(s"$validDate", "2017-04-05")

        doCreateOrTransferRequest(invalidJson) { res =>
          status(res) mustBe (FORBIDDEN)

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The firstSubscriptionDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/firstSubscriptionDate"
        }
      }
      "given a firstSubscriptionDate and transferInDate prior to 6 April 2017" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountSuccessResponse(accountId)))

        val invalidJson = transferAccountJson.replace(s"$validDate", "2017-04-05")

        doCreateOrTransferRequest(invalidJson) { res =>
          status(res) mustBe (FORBIDDEN)

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The firstSubscriptionDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/firstSubscriptionDate"
          (json \ "errors" \ 1 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 1 \ "message").as[String] mustBe "The transferInDate cannot be before 6 April 2017"
          (json \ "errors" \ 1 \ "path").as[String] mustBe "/transferAccount/transferInDate"
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

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" when {
      "the data service returns a CreateLisaAccountInvestorAccountAlreadyClosedResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
      "the data service returns a CreateLisaAccountInvestorAccountAlreadyClosedResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyClosedResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" when {
      "the data service returns a CreateLisaAccountInvestorAccountAlreadyVoidResponse for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyVoidResponse))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_VOID")
        }
      }
      "the data service returns a CreateLisaAccountInvestorAccountAlreadyVoidResponse for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.successful(CreateLisaAccountInvestorAccountAlreadyVoidResponse))

        doCreateOrTransferRequest(transferAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_VOID")
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
      "the data service throws an exception for a create request" in {
        when(mockService.createAccount(any(), any())(any())).thenReturn(Future.failed(new RuntimeException("Test")))

        doCreateOrTransferRequest(createAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
      "the data service throws an exception for a transfer request" in {
        when(mockService.transferAccount(any(), any())(any())).thenReturn(Future.failed(new RuntimeException("Test")))

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

  "The Get Account Details endpoint" must {

    when(mockAuthCon.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))

    "return the correct json" when {
      "returning a valid open account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(GetLisaAccountSuccessResponse("9876543210", "8765432100", "New", s"$validDate", "OPEN", "ACTIVE", None, None, None)))
        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.toJson (GetLisaAccountSuccessResponse("9876543210", "8765432100", "New", s"$validDate", "OPEN", "ACTIVE", None, None, None))
        })
      }
      "returning a valid close account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(GetLisaAccountSuccessResponse("9876543210", "8765432100", "New", s"$validDate", "CLOSED", "ACTIVE", Some("All funds withdrawn"), Some("2017-01-03"), None)))
        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.toJson (GetLisaAccountSuccessResponse("9876543210", "8765432100", "New", s"$validDate",  "CLOSED", "ACTIVE", Some("All funds withdrawn"), Some("2017-01-03"), None))
        })
      }
      "returning a valid transfer account response" in {
        when(mockService.getAccount(any(), any())(any())).
          thenReturn(Future.successful(GetLisaAccountSuccessResponse("9876543210", "8765432100", "Transferred", s"$validDate", "OPEN", "ACTIVE", None, None, Some(GetLisaAccountTransferAccount("8765432102", "Z543333", new DateTime(s"$validDate"))))))

        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.toJson (GetLisaAccountSuccessResponse("9876543210", "8765432100", "Transferred", s"$validDate", "OPEN", "ACTIVE", None, None, Some(GetLisaAccountTransferAccount("8765432102", "Z543333", new DateTime(s"$validDate")))))
        })
      }
      "returning a valid void account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(GetLisaAccountSuccessResponse("9876543210", "8765432100", "New", s"$validDate", "VOID", "ACTIVE", None, None, None)))
        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK
          contentAsJson(res) mustBe Json.toJson (GetLisaAccountSuccessResponse("9876543210", "8765432100", "New", s"$validDate", "VOID", "ACTIVE", None, None, None))
        })
      }
      "returning a account not found response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(GetLisaAccountDoesNotExistResponse))
        doSyncGetAccountDetailsRequest(res => {
          (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
        })
      }
      "returning a internal server error response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(GetLisaAccountErrorResponse))
        doSyncGetAccountDetailsRequest(res => {
          (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        })
      }
    }

  }

  "The Close Account endpoint" must {

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))

    "audit an account closed event" when {
      "return with status 200 ok" when {
        "submitted a valid close account request" in {
          when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse(accountId)))

          doSyncCloseRequest(closeAccountJson) { res =>
            verify(mockAuditService).audit(
              auditType = matchersEquals("accountClosed"),
              path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
              auditData = matchersEquals(Map(
                "lisaManagerReferenceNumber" -> lisaManager,
                "accountClosureReason" -> "All funds withdrawn",
                "closureDate" -> s"$validDate",
                "accountId" -> accountId
               )))(any())
          }
        }
      }
    }

    "audit an accountNotClosed event" when {
      "the data service returns a CloseLisaAccountAlreadyVoidResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyVoidResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INVESTOR_ACCOUNT_ALREADY_VOID"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyClosedResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountCancellationPeriodExceeded" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountCancellationPeriodExceeded))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "CANCELLATION_PERIOD_EXCEEDED"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountWithinCancellationPeriod" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountWithinCancellationPeriod))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "ACCOUNT_WITHIN_CANCELLATION_PERIOD"
            )))(any())
        }
      }
      "the data service returns a CloseLisaAccountNotFoundResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountNotFoundResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            )))(any())

        }
      }
      "the data service returns a CloseLisaAccountErrorResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountErrorResponse))

        doSyncCloseRequest(closeAccountJson) { res =>
          verify(mockAuditService).audit(
            auditType = matchersEquals("accountNotClosed"),
            path=matchersEquals(s"/manager/$lisaManager/accounts/$accountId/close-account"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountClosureReason" -> "All funds withdrawn",
              "closureDate" -> s"$validDate",
              "accountId" -> accountId,
              "reasonNotClosed" -> "INTERNAL_SERVER_ERROR"
            )))(any())

        }
      }
    }

    "return with status 200 ok" when {
      "submitted a valid close account request" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse(accountId)))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (OK)
        }
      }
    }

    "return with status 403 forbidden and a code of FORBIDDEN" when {
      "given a closure date prior to 6 April 2017" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountSuccessResponse(accountId)))

        val json = closeAccountJson.replace(validDate, "2017-04-05")

        doCloseRequest(json) { res =>
          status(res) mustBe (FORBIDDEN)

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The closureDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/closureDate"
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" when {
      "the data service returns a CloseLisaAccountAlreadyVoidResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyVoidResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_VOID")
        }
      }
    }

    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountAlreadyClosedResponse))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }

    "return with status 403 forbidden and a code of CANCELLATION_PERIOD_EXCEEDED" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountCancellationPeriodExceeded))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("CANCELLATION_PERIOD_EXCEEDED")
        }
      }
    }

    "return with status 403 forbidden and a code of ACCOUNT_WITHIN_CANCELLATION_PERIOD" when {
      "the data service returns a CloseLisaAccountAlreadyClosedResponse" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenReturn(Future.successful(CloseLisaAccountWithinCancellationPeriod))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("ACCOUNT_WITHIN_CANCELLATION_PERIOD")
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
        doCloseRequest(closeAccountJson.replace("All funds withdrawn", "X")) { res =>
          status(res) mustBe (BAD_REQUEST)
        }
      }
      "submitted an invalid lmrn" in {
        doCloseRequest(closeAccountJson, "Z12345") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
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
      "an exception is thrown" in {
        when(mockService.closeAccount(any(), any(), any())(any())).thenThrow(new RuntimeException("Test"))

        doCloseRequest(closeAccountJson) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }

  }

  def doCreateOrTransferRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) => Unit) {
    val res = SUT.createOrTransferLisaAccount(lmrn).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doSyncCreateOrTransferRequest(jsonString: String)(callback: (Result) => Unit) {
    val res = await(SUT.createOrTransferLisaAccount(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString)))))

    callback(res)
  }

  def doSyncGetAccountDetailsRequest(callback: (Future[Result]) => Unit) {
    val res = SUT.getAccountDetails(lisaManager, accountId).apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
    callback(res)
  }


  def doCloseRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) => Unit) {
    val res = SUT.closeLisaAccount(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
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
    override val authConnector = mockAuthCon
  }

}