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
  val accountId = "1234567890"
  val mockAuthCon = mock[LisaAuthConnector]

  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  val updateFirstSubscriptionDate = """{
                            |  "firstSubscriptionDate" : "2017-05-05"
                            |}""".stripMargin


  "The update first subscription date endpoint" must {
    "audit an firstSubscriptionDateUpdate event with response as UPDATED" when {
      "submitted a valid first subscription update request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED", "message")))
        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)

          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateUpdated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-05-05"
            ))
          )(any())
        }
      }
    }

    "audit an firstSubscriptionDateUpdate event with response as UPDATED_AND_ACCOUNT_OPENED" when {
      "submitted a valid first subscription update request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_OPENED", "message")))
        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)

          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateUpdated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-05-05"
            ))
          )(any())
        }
      }
    }


    "audit an firstSubscriptionDateUpdate event with response as UPDATED_AND_ACCOUNT_VOID" when {
      "submitted a valid first subscription update request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_VOID", "message")))
        doUpdateSubsDate(updateFirstSubscriptionDate) { result =>
          await(result)

          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateUpdated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-05-05"
            ))
          )(any())
        }
      }
    }



    "the data service returns a UpdateFirstSubscriptionDateAccountAlreadyVoidedResponse for a create request" in {
        when(mockService.updateSubscription(any(), any(),any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountVoidedResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) {result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-05-05",
              "reasonNotUpdated" -> "INVESTOR_ACCOUNT_ALREADY_VOID"
            ))
          )(any())
        }
      }

    "the data service returns a UpdateFirstSubscriptionDateAccountAlreadyClosedResponse for a create request" in {
      when(mockService.updateSubscription(any(), any(),any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountClosedResponse))

      doUpdateSubsDate(updateFirstSubscriptionDate) {result =>
        await(result)
        verify(mockAuditService).audit(
          auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
          path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
          auditData = matchersEquals(Map(
            "lisaManagerReferenceNumber" -> lisaManager,
            "accountID" -> accountId,
            "firstSubscriptionDate" -> "2017-05-05",
            "reasonNotUpdated" -> "INVESTOR_ACCOUNT_ALREADY_CLOSED"
          ))
        )(any())
      }
    }
      "the data service returns a UpdateFirstSubscriptionDateAccountNotFound for a create request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountNotFoundResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) {result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-05-05",
              "reasonNotUpdated" -> "INVESTOR_ACCOUNTID_NOT_FOUND"
            ))
          )(any())
        }
      }
      "the data service returns a UpdateFirstSubscriptionDateErrorResponse" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionErrorResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) {result =>
          await(result)
          verify(mockAuditService).audit(
            auditType = matchersEquals("firstSubscriptionDateNotUpdated"),
            path= matchersEquals(s"/manager/$lisaManager/accounts/$accountId/update-subscription"),
            auditData = matchersEquals(Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountID" -> accountId,
              "firstSubscriptionDate" -> "2017-05-05",
              "reasonNotUpdated" -> "INTERNAL_SERVER_ERROR"
            ))
          )(any())
        }
      }
    }

    "return with status 200 created and an account Id" when {
      "submitted a valid update subscription request request and response UPDATED" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED", "message")))
        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (OK)
        }
      }
    }

  "return with status 200 created and an account Id" when {
    "submitted a valid update subscription request request and response UPDATED_AND_ACCOUNT_OPENED" in {
      when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_OPENED", "message")))
      doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
        status(res) mustBe (OK)
      }
    }
  }

  "return with status 200 created and an account Id" when {
    "submitted a valid update subscription request request and response UPDATED_AND_ACCOUNT_VOID" in {
      when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_VOID", "message")))
      doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
        status(res) mustBe (OK)
      }
    }
  }

    "return with status 400 bad request and a code of BAD_REQUEST" when {
      "invalid json is sent" in {
        val invalidJson = updateFirstSubscriptionDate.replace("2017-05-05", "")

        doUpdateSubsDate(invalidJson) { res =>
          status(res) mustBe (BAD_REQUEST)
          (contentAsJson(res) \ "code").as[String] mustBe ("BAD_REQUEST")
        }
      }
      "invalid lmrn is sent" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionSuccessResponse("code","message")))

        doUpdateSubsDateInvalidLMRN(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (BAD_REQUEST)
          val json = contentAsJson(res)
          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
    }

    "return with status 403 forbidden and a code" when {
      "the data service returns a UpdateFirstSubscriptionDateAccountNotFound for a update request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountNotFoundResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>

          status(res) mustBe (NOT_FOUND)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNTID_NOT_FOUND")
        }
      }
    }
    "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" when {
      "the data service returns a UpdateFirstSubscriptionDateAccountAlreadyClosedOrVoidedResponse for a create request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountClosedResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (FORBIDDEN)
          (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_CLOSED")
        }
      }

    }

  "return with status 403 forbidden and a code of INVESTOR_ACCOUNT_ALREADY_VOID" when {
    "the data service returns a UpdateFirstSubscriptionDateAccountAlreadyClosedOrVoidedResponse for a create request" in {
      when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionAccountVoidedResponse))

      doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
        status(res) mustBe (FORBIDDEN)
        (contentAsJson(res) \ "code").as[String] mustBe ("INVESTOR_ACCOUNT_ALREADY_VOID")
      }
    }

  }

    "return with status 500 internal server error" when {
      "the data service returns an error for a create request" in {
        when(mockService.updateSubscription(any(), any(), any())(any())).thenReturn(Future.successful(UpdateSubscriptionErrorResponse))

        doUpdateSubsDate(updateFirstSubscriptionDate) { res =>
          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
  }



  def doUpdateSubsDate(jsonString: String)(callback: (Future[Result]) => Unit) {
    val res = SUT.updateSubscription(lisaManager, "1234567890").apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }


  def doUpdateSubsDateInvalidLMRN(jsonString: String)(callback: (Future[Result]) => Unit) {
    val res = SUT.updateSubscription("12345678", "1234567890").apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }


  val mockService: UpdateSubscriptionService = mock[UpdateSubscriptionService]
  val mockAuditService: AuditService = mock[AuditService]
  val SUT = new UpdateSubscriptionController{
    override val service: UpdateSubscriptionService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
  }

}