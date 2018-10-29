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
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{ErrorBadRequestAccountId, ErrorBadRequestLmrn, PropertyPurchaseController}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, PropertyPurchaseService}

class PropertyPurchaseControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val eventDate = "2017-04-06"

  implicit val hc:HeaderCarrier = HeaderCarrier()

  before {
    reset(mockAuditService)
    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  val fundReleaseJson = """
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
"""

  val supersededFundReleaseJson = """
{
  "eventDate": "2017-05-05",
  "withdrawalAmount": 5000.00,
  "supersede": {
    "originalFundReleaseId": "3456789000",
    "originalEventDate": "2017-05-10"
  }
}
"""

  val extensionJson = """
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
}
"""

  val supersededExtensionJson = """
{
  "eventDate": "2017-05-11",
  "eventType": "Extension one",
  "supersede": {
    "originalEventDate": "2017-05-10",
    "originalExtensionId": "6789000001"
  }
}
"""

  "Request Fund Release" should {

    "audit fundReleaseReported" when {
      "a initial fund release request has been successful" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doFundReleaseRequest(fundReleaseJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("fundReleaseReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventDate" -> "2017-05-10",
              "withdrawalAmount" -> "4000.00",
              "conveyancerReference" -> "CR12345-6789",
              "nameOrNumber" -> "1",
              "postalCode" -> "AA11 1AA"
            ))
          )(any())
        }
      }
      "a superseded fund release request has been successful" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doFundReleaseRequest(supersededFundReleaseJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("fundReleaseReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventDate" -> "2017-05-05",
              "withdrawalAmount" -> "5000.00",
              "originalFundReleaseId" -> "3456789000",
              "originalEventDate" -> "2017-05-10"
            ))
          )(any())
        }
      }
    }

    "audit fundReleaseNotReported" when {
      "the request results in a PropertyPurchaseErrorResponse" in {
        when(mockService.requestFundRelease(any(), any(), any())(any()))
          .thenReturn(Future.successful(PropertyPurchaseErrorResponse))

        doFundReleaseRequest(fundReleaseJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("fundReleaseNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventDate" -> "2017-05-10",
              "withdrawalAmount" -> "4000.00",
              "conveyancerReference" -> "CR12345-6789",
              "nameOrNumber" -> "1",
              "postalCode" -> "AA11 1AA",
              "reasonNotReported" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
      "given a eventDate prior to 6 April 2017" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))

        doFundReleaseRequest(fundReleaseJson.replace("2017-05-10", "2017-04-05")) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("fundReleaseNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventDate" -> "2017-04-05",
              "withdrawalAmount" -> "4000.00",
              "conveyancerReference" -> "CR12345-6789",
              "nameOrNumber" -> "1",
              "postalCode" -> "AA11 1AA",
              "reasonNotReported" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
    }

    "return with 201 created" when {
      "a initial fund release is successful" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doFundReleaseRequest(fundReleaseJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "fundReleaseId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Fund release created"
        }
      }
      "a superseded fund release is successful" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doFundReleaseRequest(supersededFundReleaseJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "fundReleaseId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Fund release superseded"
        }
      }
    }

    "return with 400 bad request and a code of BAD_REQUEST" when {
      "given a future eventDate" in {
        val invalidJson = fundReleaseJson.replace("2017-05-10", DateTime.now.plusDays(1).toString("yyyy-MM-dd"))

        doFundReleaseRequest(invalidJson) { res =>
          status(res) mustBe BAD_REQUEST
          (contentAsJson(res) \ "code").as[String] mustBe "BAD_REQUEST"
        }
      }
      "given an invalid lmrn in the url" in {
        doFundReleaseRequest(fundReleaseJson, "Z111") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "given an invalid accountId in the url" in {
        doFundReleaseRequest(fundReleaseJson, accId = "1=2!") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }

    "return with 403 forbidden and a code of FORBIDDEN" when {
      "given a eventDate prior to 6 April 2017" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))

        doFundReleaseRequest(fundReleaseJson.replace("2017-05-10", "2017-04-05")) { res =>
          status(res) mustBe FORBIDDEN
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The eventDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/eventDate"
        }
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountClosedResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already closed"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountCancelledResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already cancelled"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountVoidResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_VOID"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already void"
      }
    }

    "return with 403 forbidden and a code of SUPERSEDED_FUND_RELEASE_MISMATCH_ERROR" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseMismatchResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_FUND_RELEASE_MISMATCH_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "originalFundReleaseId and the originalEventDate do not match the information in the original request"
      }
    }

    "return with 403 forbidden and a code of COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountNotOpenLongEnoughResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH"
        (contentAsJson(res) \ "message").as[String] mustBe "The account has not been open for long enough"
      }
    }

    "return with 403 forbidden and a code of COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseOtherPurchaseOnRecordResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD"
        (contentAsJson(res) \ "message").as[String] mustBe "Another property purchase is already recorded"
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountNotFoundResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The accountId does not match HMRC’s records"
      }
    }

    "return with 409 conflict and a code of FUND_RELEASE_ALREADY_EXISTS" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseLifeEventAlreadyExistsResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_ALREADY_EXISTS"
        (contentAsJson(res) \ "message").as[String] mustBe "The investor’s fund release has already been requested"
      }
    }

    "return with 409 conflict and a code of SUPERSEDED_FUND_RELEASE_ALREADY_SUPERSEDED" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseLifeEventAlreadySupersededResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_FUND_RELEASE_ALREADY_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This fund release has already been superseded"
      }
    }

    "return with 500 internal server error and a code of INTERNAL_SERVER_ERROR" in {
      when(mockService.requestFundRelease(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseErrorResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Internal server error"
      }
    }

  }

  "Request Extension" should {

    "audit extensionReported" when {
      "a initial extension request has been successful" in {
        when(mockService.requestPurchaseExtension(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doExtensionRequest(extensionJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("extensionReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/extension"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789001",
              "eventDate" -> "2017-05-10",
              "eventType" -> "Extension one"
            ))
          )(any())
        }
      }
      "a superseded extension request has been successful" in {
        when(mockService.requestPurchaseExtension(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doExtensionRequest(supersededExtensionJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("extensionReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/extension"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "eventDate" -> "2017-05-11",
              "eventType" -> "Extension one",
              "originalEventDate" -> "2017-05-10",
              "originalExtensionId" -> "6789000001"
            ))
          )(any())
        }
      }
    }

    "audit extensionNotReported" when {
      "the request results in a PropertyPurchaseErrorResponse" in {
        when(mockService.requestPurchaseExtension(any(), any(), any())(any()))
          .thenReturn(Future.successful(PropertyPurchaseErrorResponse))

        doExtensionRequest(extensionJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("extensionNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/extension"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789001",
              "eventDate" -> "2017-05-10",
              "eventType" -> "Extension one",
              "reasonNotReported" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
      "given a eventDate prior to 6 April 2017" in {
        when(mockService.requestFundRelease(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))

        doExtensionRequest(extensionJson.replace("2017-05-10", "2017-04-05")) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("extensionNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/extension"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789001",
              "eventDate" -> "2017-04-05",
              "eventType" -> "Extension one",
              "reasonNotReported" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
    }

    "return with 201 created" when {
      "a initial extension is successful" in {
        when(mockService.requestPurchaseExtension(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doExtensionRequest(extensionJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "extensionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Extension created"
        }
      }
      "a superseded extension is successful" in {
        when(mockService.requestPurchaseExtension(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))
        doExtensionRequest(supersededExtensionJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "extensionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Extension superseded"
        }
      }
    }

    "return with 400 bad request and a code of BAD_REQUEST" when {
      "given a future eventDate" in {
        val invalidJson = extensionJson.replace("2017-05-10", DateTime.now.plusDays(1).toString("yyyy-MM-dd"))

        doExtensionRequest(invalidJson) { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "BAD_REQUEST"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/eventDate"
        }
      }
      "given an invalid lmrn in the url" in {
        doExtensionRequest(extensionJson, "Z111") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "given an invalid accountId in the url" in {
        doExtensionRequest(extensionJson, accId = "1=2!") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }

    "return with 403 forbidden and a code of FORBIDDEN" when {
      "given a eventDate prior to 6 April 2017" in {
        when(mockService.requestPurchaseExtension(any(), any(), any())(any())).thenReturn(Future.successful(PropertyPurchaseSuccessResponse("1928374")))

        doExtensionRequest(extensionJson.replace("2017-05-10", "2017-04-05")) { res =>
          status(res) mustBe FORBIDDEN
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe "FORBIDDEN"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "message").as[String] mustBe "The eventDate cannot be before 6 April 2017"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/eventDate"
        }
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountClosedResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already closed"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountVoidResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_VOID"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already void"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountCancelledResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already cancelled"
      }
    }

    "return with 403 forbidden and a code of FIRST_EXTENSION_NOT_APPROVED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseExtensionOneNotYetApprovedResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "FIRST_EXTENSION_NOT_APPROVED"
        (contentAsJson(res) \ "message").as[String] mustBe "A first extension has not been approved"
      }
    }

    "return with 403 forbidden and a code of FIRST_EXTENSION_ALREADY_APPROVED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseExtensionOneAlreadyApprovedResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "FIRST_EXTENSION_ALREADY_APPROVED"
        (contentAsJson(res) \ "message").as[String] mustBe "A first extension has already been approved"
      }
    }

    "return with 403 forbidden and a code of SECOND_EXTENSION_ALREADY_APPROVED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseExtensionTwoAlreadyApprovedResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SECOND_EXTENSION_ALREADY_APPROVED"
        (contentAsJson(res) \ "message").as[String] mustBe "A second extension has already been approved"
      }
    }

    "return with 403 forbidden and a code of SUPERSEDED_EXTENSION_MISMATCH_ERROR" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseMismatchResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_EXTENSION_MISMATCH_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "originalExtensionId and the originalEventDate do not match the information in the original request"
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseAccountNotFoundResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The accountId does not match HMRC’s records"
      }
    }

    "return with 404 not found and a code of FUND_RELEASE_NOT_FOUND" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseFundReleaseNotFoundResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The fundReleaseId does not match with HMRC’s records"
      }
    }

    "return with 409 conflict and a code of EXTENSION_ALREADY_EXISTS" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseLifeEventAlreadyExistsResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "EXTENSION_ALREADY_EXISTS"
        (contentAsJson(res) \ "message").as[String] mustBe "The investor’s purchase extension has already been reported"
      }
    }

    "return with 409 conflict and a code of FUND_RELEASE_SUPERSEDED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseFundReleaseSupersededResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This fund release has already been superseded"
      }
    }

    "return with 409 conflict and a code of SUPERSEDED_EXTENSION_ALREADY_SUPERSEDED" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseLifeEventAlreadySupersededResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_EXTENSION_ALREADY_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This extension has already been superseded"
      }
    }

    "return with 500 internal server error and a code of INTERNAL_SERVER_ERROR" in {
      when(mockService.requestPurchaseExtension(any(), any(),any())(any())).thenReturn(Future.successful(PropertyPurchaseErrorResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Internal server error"
      }
    }

  }

  def doFundReleaseRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.PUT, "/")
    val res = SUT.requestFundRelease(lmrn, accId).apply(req.withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doExtensionRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.PUT, "/")
    val res = SUT.requestExtension(lmrn, accId).apply(req.withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService: PropertyPurchaseService = mock[PropertyPurchaseService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon :LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new PropertyPurchaseController {
    override val service: PropertyPurchaseService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
  }

}