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
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfter, MustMatchers}
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.api.controllers.ErrorAcceptHeaderInvalid
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountNotFound, ErrorBadRequestLmrn, TransactionController}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.TransactionService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransactionControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter
  with MustMatchers {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lmrn = "Z1234"
  val accountId = "12345"
  val transactionId = "67890"

  before {
    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  "Get transaction" must {

    "return 200 ok" when {
      "data is returned from the service" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionSuccessResponse(
          transactionId = transactionId,
          creationDate = new DateTime("2000-01-01"),
          bonusDueForPeriod = Some(1.0),
          status = "Cancelled"
        )))

        val res = SUT.getTransaction(lmrn, accountId, transactionId).apply(FakeRequest().withHeaders(acceptHeader))

        status(res) mustBe OK

        val json = contentAsJson(res)

        (contentAsJson(res) \ "transactionId").as[String] mustBe transactionId
        (contentAsJson(res) \ "creationDate").as[String] mustBe "2000-01-01"
        (contentAsJson(res) \ "bonusDueForPeriod").as[Amount] mustBe 1.0
        (contentAsJson(res) \ "status").as[String] mustBe "Cancelled"
      }
    }

    "return 400 bad request" when {
      "the LMRN in the URL is in an incorrect format" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionErrorResponse))

        val res = SUT.getTransaction("1234", accountId, transactionId).apply(FakeRequest().withHeaders(acceptHeader))

        status(res) mustBe BAD_REQUEST

        (contentAsJson(res) \ "message").as[String] mustBe ErrorBadRequestLmrn.message
      }
    }

    "return 404 not found" when {
      "the service returns account not found" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionAccountNotFoundResponse))

        val res = SUT.getTransaction(lmrn, accountId, transactionId).apply(FakeRequest().withHeaders(acceptHeader))

        status(res) mustBe NOT_FOUND

        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
      }
      "the service returns transaction not found" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionTransactionNotFoundResponse))

        val res = SUT.getTransaction(lmrn, accountId, transactionId).apply(FakeRequest().withHeaders(acceptHeader))

        status(res) mustBe NOT_FOUND

        (contentAsJson(res) \ "code").as[String] mustBe "TRANSACTION_NOT_FOUND"
      }
      "the accountId in the URL is in an incorrect format" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionErrorResponse))

        val res = SUT.getTransaction(lmrn, "!!!", transactionId).apply(FakeRequest().withHeaders(acceptHeader))

        status(res) mustBe NOT_FOUND

        (contentAsJson(res) \ "code").as[String] mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
      }
    }

    "return 406 not acceptable" when {
      "the http accept header is missing" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionErrorResponse))

        val res = SUT.getTransaction(lmrn, accountId, transactionId).apply(FakeRequest())

        status(res) mustBe NOT_ACCEPTABLE

        (contentAsJson(res) \ "message").as[String] mustBe ErrorAcceptHeaderInvalid.message
      }
    }

    "return 500 internal server error" when {
      "the service returns an error" in {
        when(mockService.getTransaction(any(), any(), any())(any())).thenReturn(Future.successful(GetTransactionErrorResponse))

        val res = SUT.getTransaction(lmrn, accountId, transactionId).apply(FakeRequest().withHeaders(acceptHeader))

        status(res) mustBe INTERNAL_SERVER_ERROR

        (contentAsJson(res) \ "code").as[String] mustBe "INTERNAL_SERVER_ERROR"
      }
    }

  }

  val mockService: TransactionService = mock[TransactionService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new TransactionController {
    override val service: TransactionService = mockService
    override val authConnector = mockAuthCon
  }

}
