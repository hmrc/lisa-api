/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.controllers.{ErrorBadRequestAccountId, ErrorBadRequestLmrn, PropertyPurchaseController}
import uk.gov.hmrc.lisaapi.metrics.LisaMetrics
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PropertyPurchaseControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
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
    "originalLifeEventId": "3456789000",
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
    "originalLifeEventId": "6789000001"
  }
}
"""

  val outcomeJson = """
{
  "fundReleaseId": "3456789000",
  "eventDate": "2017-05-05",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
}
"""

  val supersededOutcomeJson = """
{
  "fundReleaseId": "3456789000",
  "eventDate": "2017-06-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000,
  "supersede": {
    "originalLifeEventId": "5678900001",
    "originalEventDate": "2017-05-05"
  }
}
"""

  "Request Fund Release" should {

    "audit fundReleaseReported" when {
      "a initial fund release request has been successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
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
              "originalLifeEventId" -> "3456789000",
              "originalEventDate" -> "2017-05-10"
            ))
          )(any())
        }
      }
    }

    "audit fundReleaseNotReported" when {
      "the request results in a ReportLifeEventErrorResponse" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventErrorResponse))

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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doFundReleaseRequest(fundReleaseJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Fund release created"
        }
      }
      "a superseded fund release is successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doFundReleaseRequest(supersededFundReleaseJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

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
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountClosedResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already closed"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountCancelledResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already cancelled"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountVoidResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_VOID"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already void"
      }
    }

    "return with 403 forbidden and a code of SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventMismatchResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "originalLifeEventId and the originalEventDate do not match the information in the original request"
      }
    }

    "return with 403 forbidden and a code of COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotOpenLongEnoughResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH"
        (contentAsJson(res) \ "message").as[String] mustBe "The account has not been open for long enough"
      }
    }

    "return with 403 forbidden and a code of COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventOtherPurchaseOnRecordResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD"
        (contentAsJson(res) \ "message").as[String] mustBe "Another property purchase is already recorded"
      }
    }

    "return with 403 forbidden and a code of INVALID_DATA_PROVIDED" in {
      val json = Json.parse(fundReleaseJson).as[JsObject] ++ Json.obj(
        "supersede" -> Json.obj(
          "originalLifeEventId" -> "3456789000",
          "originalEventDate" -> "2017-05-10"
        )
      )

      doFundReleaseRequest(json.toString()){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVALID_DATA_PROVIDED"
        (contentAsJson(res) \ "message").as[String] mustBe "You can only change eventDate or withdrawalAmount when superseding a property purchase fund release"
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The accountId does not match HMRC’s records"
      }
    }

    "return with 406 not acceptable and a code of ACCEPT_HEADER_INVALID" when {
      "attempting to use the v1 of the api" in {
        doFundReleaseRequest(fundReleaseJson, header = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")) { res =>
          status(res) mustBe NOT_ACCEPTABLE
          (contentAsJson(res) \ "code").as[String] mustBe "ACCEPT_HEADER_INVALID"
          (contentAsJson(res) \ "message").as[String] mustBe "The accept header has an invalid version for this endpoint"
        }
      }
    }

    "return with 409 conflict and a code of LIFE_EVENT_ALREADY_EXISTS" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("123")))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_ALREADY_EXISTS"
        (contentAsJson(res) \ "message").as[String] mustBe "The investor’s life event has already been reported."
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "123"
      }
    }

    "return with 409 conflict and a code of SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadySupersededResponse("123")))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This life event has already been superseded"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "123"
      }
    }

    "return with 500 internal server error and a code of INTERNAL_SERVER_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventErrorResponse))
      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Internal server error"
      }
    }

    "return with 503 service unavailable and a code of SERVER_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).
        thenReturn(Future.successful(ReportLifeEventServiceUnavailableResponse))

      doFundReleaseRequest(fundReleaseJson){ res =>
        status(res) mustBe SERVICE_UNAVAILABLE
        (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Service unavailable"
      }
    }

  }

  "Request Extension" should {

    "audit extensionReported" when {
      "a initial extension request has been successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
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
              "originalLifeEventId" -> "6789000001"
            ))
          )(any())
        }
      }
    }

    "audit extensionNotReported" when {
      "the request results in a ReportLifeEventErrorResponse" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventErrorResponse))

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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doExtensionRequest(extensionJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Extension created"
        }
      }
      "a superseded extension is successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doExtensionRequest(supersededExtensionJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
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
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

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
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountClosedResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already closed"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountVoidResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_VOID"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already void"
      }
    }

    "return with 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountCancelledResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
        (contentAsJson(res) \ "message").as[String] mustBe "The LISA account is already cancelled"
      }
    }

    "return with 403 forbidden and a code of FIRST_EXTENSION_NOT_APPROVED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventExtensionOneNotYetApprovedResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "FIRST_EXTENSION_NOT_APPROVED"
        (contentAsJson(res) \ "message").as[String] mustBe "A first extension has not yet been approved"
      }
    }

    "return with 403 forbidden and a code of FIRST_EXTENSION_ALREADY_APPROVED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventExtensionOneAlreadyApprovedResponse("123")))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "FIRST_EXTENSION_ALREADY_APPROVED"
        (contentAsJson(res) \ "message").as[String] mustBe "A first extension has already been approved"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "123"
      }
    }

    "return with 403 forbidden and a code of SECOND_EXTENSION_ALREADY_APPROVED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventExtensionTwoAlreadyApprovedResponse("321")))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SECOND_EXTENSION_ALREADY_APPROVED"
        (contentAsJson(res) \ "message").as[String] mustBe "A second extension has already been approved"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "321"
      }
    }

    "return with 403 forbidden and a code of SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventMismatchResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "originalLifeEventId and the originalEventDate do not match the information in the original request"
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The accountId does not match HMRC’s records"
      }
    }

    "return with 404 not found and a code of FUND_RELEASE_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventFundReleaseNotFoundResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The fundReleaseId does not match HMRC’s records"
      }
    }

    "return with 406 not acceptable and a code of ACCEPT_HEADER_INVALID" when {
      "attempting to use the v1 of the api" in {
        doExtensionRequest(extensionJson, header = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")) { res =>
          status(res) mustBe NOT_ACCEPTABLE
          (contentAsJson(res) \ "code").as[String] mustBe "ACCEPT_HEADER_INVALID"
          (contentAsJson(res) \ "message").as[String] mustBe "The accept header has an invalid version for this endpoint"
        }
      }
    }

    "return with 409 conflict and a code of LIFE_EVENT_ALREADY_EXISTS" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("123")))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_ALREADY_EXISTS"
        (contentAsJson(res) \ "message").as[String] mustBe "The investor’s life event has already been reported."
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "123"
      }
    }

    "return with 409 conflict and a code of FUND_RELEASE_SUPERSEDED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventFundReleaseSupersededResponse("666")))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This fund release has already been superseded"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "666"
      }
    }

    "return with 409 conflict and a code of SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadySupersededResponse("321")))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This life event has already been superseded"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "321"
      }
    }

    "return with 500 internal server error and a code of INTERNAL_SERVER_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful(ReportLifeEventErrorResponse))
      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe INTERNAL_SERVER_ERROR
        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Internal server error"
      }
    }

    "return with 503 service unavailable and a code of SERVER_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).
        thenReturn(Future.successful(ReportLifeEventServiceUnavailableResponse))

      doExtensionRequest(extensionJson){ res =>
        status(res) mustBe SERVICE_UNAVAILABLE
        (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Service unavailable"
      }
    }

  }

  "Report Purchase Outcome" should {

    "audit purchaseOutcomeReported" when {
      "a initial purchase outcome request has been successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doOutcomeRequest(outcomeJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("purchaseOutcomeReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/outcome"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789000",
              "eventDate" -> "2017-05-05",
              "propertyPurchaseResult" -> "Purchase completed",
              "propertyPurchaseValue" -> "250000"
            ))
          )(any())
        }
      }
      "a superseded purchase outcome request has been successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doOutcomeRequest(supersededOutcomeJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("purchaseOutcomeReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/outcome"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789000",
              "eventDate" -> "2017-06-10",
              "propertyPurchaseResult" -> "Purchase completed",
              "propertyPurchaseValue" -> "250000",
              "originalLifeEventId" -> "5678900001",
              "originalEventDate" -> "2017-05-05"
            ))
          )(any())
        }
      }
    }

    "audit purchaseOutcomeNotReported" when {
      "the request results in a ReportLifeEventErrorResponse" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(ReportLifeEventErrorResponse))

        doOutcomeRequest(outcomeJson) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("purchaseOutcomeNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/outcome"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789000",
              "eventDate" -> "2017-05-05",
              "propertyPurchaseResult" -> "Purchase completed",
              "propertyPurchaseValue" -> "250000",
              "reasonNotReported" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
      "given a eventDate prior to 6 April 2017" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))

        doOutcomeRequest(outcomeJson.replace("2017-05-05", "2017-04-05")) { res =>
          await(res)
          verify(mockAuditService).audit(
            auditType = matchersEquals("purchaseOutcomeNotReported"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/property-purchase/outcome"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "fundReleaseId" -> "3456789000",
              "eventDate" -> "2017-04-05",
              "propertyPurchaseResult" -> "Purchase completed",
              "propertyPurchaseValue" -> "250000",
              "reasonNotReported" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
    }

    "return with 201 created" when {
      "a initial extension is successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doOutcomeRequest(outcomeJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Purchase outcome created"
        }
      }
      "a superseded extension is successful" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventSuccessResponse("1928374")))
        doOutcomeRequest(supersededOutcomeJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "lifeEventId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Purchase outcome superseded"
        }
      }
    }

    "return with 400 bad request and a code of BAD_REQUEST" when {
      "given a future eventDate" in {
        val invalidJson = outcomeJson.replace("2017-05-05", DateTime.now.plusDays(1).toString("yyyy-MM-dd"))

        doOutcomeRequest(invalidJson) { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe "BAD_REQUEST"
          (json \ "errors" \ 0 \ "code").as[String] mustBe "INVALID_DATE"
          (json \ "errors" \ 0 \ "path").as[String] mustBe "/eventDate"
        }
      }
      "given an invalid lmrn in the url" in {
        doOutcomeRequest(outcomeJson, "Z111") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "given an invalid accountId in the url" in {
        doOutcomeRequest(outcomeJson, accId = "1=2!") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }

    "return with 403 forbidden and a code of SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventMismatchResponse))
      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe FORBIDDEN
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "originalLifeEventId and the originalEventDate do not match the information in the original request"
      }
    }

    "return with 404 not found and a code of FUND_RELEASE_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventFundReleaseNotFoundResponse))
      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The fundReleaseId does not match HMRC’s records"
      }
    }

    "return with 406 not acceptable and a code of ACCEPT_HEADER_INVALID" when {
      "attempting to use the v1 of the api" in {
        doOutcomeRequest(outcomeJson, header = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")) { res =>
          status(res) mustBe NOT_ACCEPTABLE
          (contentAsJson(res) \ "code").as[String] mustBe "ACCEPT_HEADER_INVALID"
          (contentAsJson(res) \ "message").as[String] mustBe "The accept header has an invalid version for this endpoint"
        }
      }
    }

    "return with 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" in {
      when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventAccountNotFoundResponse))
      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe NOT_FOUND
        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
        (contentAsJson(res) \ "message").as[String] mustBe "The accountId does not match HMRC’s records"
      }
    }

    "return with 409 conflict and a code of FUND_RELEASE_SUPERSEDED" in {
      when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventFundReleaseSupersededResponse("555")))
      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "FUND_RELEASE_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This fund release has already been superseded"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "555"
      }
    }

    "return with 409 conflict and a code of SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" in {
      when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadySupersededResponse("789")))
      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED"
        (contentAsJson(res) \ "message").as[String] mustBe "This life event has already been superseded"
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "789"
      }
    }

    "return with 409 conflict and a code of LIFE_EVENT_ALREADY_EXISTS" in {
      when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventAlreadyExistsResponse("123")))
      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe CONFLICT
        (contentAsJson(res) \ "code").as[String] mustBe "LIFE_EVENT_ALREADY_EXISTS"
        (contentAsJson(res) \ "message").as[String] mustBe "The investor’s life event has already been reported."
        (contentAsJson(res) \ "lifeEventId").as[String] mustBe "123"
      }
    }

    "return with 500 internal server error and a code of INTERNAL_SERVER_ERROR" when {
      "given a generic error response from the service layer" in {
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventErrorResponse))
        doOutcomeRequest(outcomeJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "Internal server error"
        }
      }
      "given a unexpected error response from the service layer" in {
        // you shouldn't get an account closed error for a purchase completion request
        when(mockService.reportLifeEvent(any(), any(), any())(any())).thenReturn(Future.successful(ReportLifeEventAccountClosedResponse))
        doOutcomeRequest(outcomeJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
          (contentAsJson(res) \ "message").as[String] mustBe "Internal server error"
        }
      }
    }

    "return with 503 service unavailable and a code of SERVER_ERROR" in {
      when(mockService.reportLifeEvent(any(), any(),any())(any())).
        thenReturn(Future.successful(ReportLifeEventServiceUnavailableResponse))

      doOutcomeRequest(outcomeJson) { res =>
        status(res) mustBe SERVICE_UNAVAILABLE
        (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        (contentAsJson(res) \ "message").as[String] mustBe "Service unavailable"
      }
    }

  }

  def doFundReleaseRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId, header: (String, String) = acceptHeader)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.POST, "/")
    val res = SUT.requestFundRelease(lmrn, accId).apply(req.withHeaders(header).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doExtensionRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId, header: (String, String) = acceptHeader)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.POST, "/")
    val res = SUT.requestExtension(lmrn, accId).apply(req.withHeaders(header).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  def doOutcomeRequest(jsonString: String, lmrn: String = lisaManager, accId: String = accountId, header: (String, String) = acceptHeader)(callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.POST, "/")
    val res = SUT.reportPurchaseOutcome(lmrn, accId).apply(req.withHeaders(header).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService: LifeEventService = mock[LifeEventService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: AuthConnector = mock[AuthConnector]
  val mockAppContext: AppContext = mock[AppContext]
  val mockLisaMetrics: LisaMetrics = mock[LisaMetrics]

  val SUT = new PropertyPurchaseController(mockAuthCon, mockAppContext, mockService, mockAuditService, mockLisaMetrics) {
    override lazy val v2endpointsEnabled = true
  }

}