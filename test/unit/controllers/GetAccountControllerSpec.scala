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
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.JsObject
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.GetAccountController
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, GetLisaAccountErrorResponse, GetLisaAccountSuccessResponse, GetLisaAccountTransferAccount}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetAccountControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite with BeforeAndAfterEach {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val mockAuthCon = mock[LisaAuthConnector]

  val validDate = "2017-04-06"
  val invalidDate = "2015-04-05"

  "The Get Account Details endpoint" must {

    when(mockAuthCon.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))

    "return the correct json" when {
      "returning a valid open account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(
          GetLisaAccountSuccessResponse(
            investorId = "9876543210",
            accountId = "8765432100",
            creationReason = "New",
            firstSubscriptionDate = new DateTime(validDate),
            accountStatus = "OPEN",
            subscriptionStatus = "AVAILABLE",
            accountClosureReason = None,
            closureDate = None,
            transferAccount = None
          )
        ))

        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK
          val json = contentAsJson(res)

          (json \ "investorId").as[String] mustBe "9876543210"
          (json \ "accountId").as[String] mustBe "8765432100"
          (json \ "creationReason").as[String] mustBe "New"
          (json \ "firstSubscriptionDate").as[String] mustBe validDate
          (json \ "accountStatus").as[String] mustBe "OPEN"
          (json \ "subscriptionStatus").as[String] mustBe "AVAILABLE"
          (json \ "accountClosureReason").asOpt[String] mustBe None
          (json \ "closureDate").asOpt[String] mustBe None
          (json \ "transferAccount").asOpt[JsObject] mustBe None
        })
      }
      "returning a valid close account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(
          GetLisaAccountSuccessResponse(
            investorId = "9876543210",
            accountId = "8765432100",
            creationReason = "New",
            firstSubscriptionDate = new DateTime(validDate),
            accountStatus = "CLOSED",
            subscriptionStatus = "ACTIVE",
            accountClosureReason = Some("All funds withdrawn"),
            closureDate = Some(new DateTime(validDate)),
            transferAccount = None)
        ))

        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK

          val json = contentAsJson(res)

          (json \ "investorId").as[String] mustBe "9876543210"
          (json \ "accountId").as[String] mustBe "8765432100"
          (json \ "creationReason").as[String] mustBe "New"
          (json \ "firstSubscriptionDate").as[String] mustBe validDate
          (json \ "accountStatus").as[String] mustBe "CLOSED"
          (json \ "subscriptionStatus").as[String] mustBe "ACTIVE"
          (json \ "accountClosureReason").asOpt[String] mustBe Some("All funds withdrawn")
          (json \ "closureDate").asOpt[String] mustBe Some(validDate)
          (json \ "transferAccount").asOpt[JsObject] mustBe None
        })
      }
      "returning a valid transfer account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(
          GetLisaAccountSuccessResponse(
            investorId = "9876543210",
            accountId = "8765432100",
            creationReason = "Transferred",
            firstSubscriptionDate = new DateTime(validDate),
            accountStatus = "OPEN",
            subscriptionStatus = "ACTIVE",
            accountClosureReason = None,
            closureDate = None,
            transferAccount = Some(
              GetLisaAccountTransferAccount(
                "8765432102",
                "Z543333",
                new DateTime(validDate)
              )
            )
          )
        ))

        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK

          val json = contentAsJson(res)

          (json \ "investorId").as[String] mustBe "9876543210"
          (json \ "accountId").as[String] mustBe "8765432100"
          (json \ "creationReason").as[String] mustBe "Transferred"
          (json \ "firstSubscriptionDate").as[String] mustBe validDate
          (json \ "accountStatus").as[String] mustBe "OPEN"
          (json \ "subscriptionStatus").as[String] mustBe "ACTIVE"
          (json \ "accountClosureReason").asOpt[String] mustBe None
          (json \ "closureDate").asOpt[String] mustBe None
          (json \ "transferAccount" \ "transferredFromAccountId").as[String] mustBe "8765432102"
          (json \ "transferAccount" \ "transferredFromLMRN").as[String] mustBe "Z543333"
          (json \ "transferAccount" \ "transferInDate").as[String] mustBe validDate
        })
      }
      "returning a valid void account response" in {
        when(mockService.getAccount(any(), any())(any())).thenReturn(Future.successful(
          GetLisaAccountSuccessResponse(
            investorId = "9876543210",
            accountId = "8765432100",
            creationReason = "New",
            firstSubscriptionDate = new DateTime(validDate),
            accountStatus = "VOID",
            subscriptionStatus = "ACTIVE",
            accountClosureReason = None,
            closureDate = None,
            transferAccount = None
          )
        ))

        doSyncGetAccountDetailsRequest(res => {
          status(res) mustBe OK

          val json = contentAsJson(res)

          (json \ "investorId").as[String] mustBe "9876543210"
          (json \ "accountId").as[String] mustBe "8765432100"
          (json \ "creationReason").as[String] mustBe "New"
          (json \ "firstSubscriptionDate").as[String] mustBe validDate
          (json \ "accountStatus").as[String] mustBe "VOID"
          (json \ "subscriptionStatus").as[String] mustBe "ACTIVE"
          (json \ "accountClosureReason").asOpt[String] mustBe None
          (json \ "closureDate").asOpt[String] mustBe None
          (json \ "transferAccount").asOpt[JsObject] mustBe None
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

  def doSyncGetAccountDetailsRequest(callback: (Future[Result]) => Unit) {
    val res = SUT.getAccountDetails(lisaManager, accountId).apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))
    callback(res)
  }

  val mockService: AccountService = mock[AccountService]
  val mockAuditService: AuditService = mock[AuditService]
  val SUT = new GetAccountController{
    override val service: AccountService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector = mockAuthCon
    override lazy val v2endpointsEnabled = true
  }

}