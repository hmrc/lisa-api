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
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService, UpdateSubscriptionService}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class UpdateFirstSubscriptionDateSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "1234/567890"
  val mockAuthCon = mock[LisaAuthConnector]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    when(mockAuthCon.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  val updateFirstSubscriptionDate =
    """{
      |  "firstSubscriptionDate" : "2017-04-06"
      |}""".stripMargin

  "The update first subscription date endpoint" must {
    "audit a firstSubscriptionDateUpdated event" when {
      "the data service returns a UpdateSubscriptionSuccessResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("code", "message")))
        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)

          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-06"
            ))
          )(any())
        }
      }
    }
    "audit a firstSubscriptionDateNotUpdated event" when {
      "the json fails date validation" in {
        doUpdateSubsDate(updateFirstSubscriptionDate.replace("2017-04-06", "2017-04-05")) { result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-05",
              "reasonNotUpdated" -> "FORBIDDEN"
            ))
          )(any())
        }
      }
      "the data service returns a UpdateFirstSubscriptionDateAccountAlreadyVoidedResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountVoidedResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-06",
              "reasonNotUpdated" -> "INVESTOR_ACCOUNT_ALREADY_VOID"
            ))
          )(any())
        }
      }
      "the data service returns a UpdateFirstSubscriptionDateAccountAlreadyClosedResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountClosedResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-06",
              "reasonNotUpdated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
            ))
          )(any())
        }
      }
      "the data service returns a UpdateFirstSubscriptionDateAccountNotFound" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountNotFoundResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-06",
              "reasonNotUpdated" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            ))
          )(any())
        }
      }
      "the data service returns a UpdateSubscriptionServiceUnavailableResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionServiceUnavailableResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-06",
              "reasonNotUpdated" -> "SERVER_ERROR"
            ))
          )(any())
        }
      }
      "the data service returns a UpdateFirstSubscriptionDateErrorResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionErrorResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-04-06",
              "reasonNotUpdated" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
    }
    "return with status 200 ok and an account Id" when {
      "the data service returns a UpdateSubscriptionSuccessResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED", "message")))
        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (OK)
          (contentAsJson(res) \ "data" \ "accountId").as[String] mustBe accountId
        }
      }
    }
    "return with status 400 bad request and a code of BAD_REQUEST" when {
      "invalid json is sent" in {
        val invalidJson = updateFirstSubscriptionDate.replace("2017-04-06", "")

        doUpdateSubsDate(invalidJson) { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ("BAD_REQUEST")
          (json \ "errors" \ 0 \ "code").as[String] mustBe ("INVALID_DATE")
          (json \ "errors" \ 0 \ "path").as[String] mustBe ("/firstSubscriptionDate")
        }
      }
      "an invalid lmrn is sent" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("code", "message")))

        doUpdateSubsDate(updateFirstSubscriptionDate, lmrn = "123") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "an invalid accountId is sent" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("code", "message")))

        doUpdateSubsDate(updateFirstSubscriptionDate, accId = "1=2!") { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }
    "return with status 403 forbidden and a code of FORBIDDEN" when {
      "an invalid date is sent" in {
        val invalidJson = updateFirstSubscriptionDate.replace("2017-04-06", "2017-04-05")

        doUpdateSubsDate(invalidJson) { res =>
          status(res) mustBe (FORBIDDEN)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ("FORBIDDEN")
          (json \ "errors" \ 0 \ "code").as[String] mustBe ("INVALID_DATE")
          (json \ "errors" \ 0 \ "path").as[String] mustBe ("/firstSubscriptionDate")
        }
      }
    }
    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED" when {
      "the data service returns a UpdateSubscriptionAccountClosedResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountClosedResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }
    }
    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" when {
      "the data service returns a UpdateSubscriptionAccountVoidedResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountVoidedResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_VOID")
        }
      }
    }
    "return with status 404 not found and a code of INVESTOR_ACCOUNTID_NOT_FOUND" when {
      "the data service returns a UpdateFirstSubscriptionDateAccountNotFound" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountNotFoundResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>

          status(res) mustBe (NOT_FOUND)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
        }
      }
    }
    "return with status 500 internal server error" when {
      "the data service returns an error" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionErrorResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
    }
    "return with status 503 service unavailable" when {
      "the data service returns a UpdateSubscriptionServiceUnavailableResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionServiceUnavailableResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe SERVICE_UNAVAILABLE
          (contentAsJson(res) \ "code").as[String] mustBe "SERVER_ERROR"
        }
      }
    }
  }

  def doUpdateSubsDate(jsonString: String, lmrn: String = lisaManager, accId: String = accountId)(callback: (Future[Result]) => Unit) {
    val res = SUT.updateSubscription(lmrn, accId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService: UpdateSubscriptionService = mock[UpdateSubscriptionService]
  val mockAuditService: AuditService = mock[AuditService]
  val SUT = new UpdateSubscriptionController{
    override val service: UpdateSubscriptionService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
    override lazy val v2endpointsEnabled = true
  }

}