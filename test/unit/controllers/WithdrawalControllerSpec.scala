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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsJson, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountNotFound, ErrorInternalServerError, WithdrawalController}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, CurrentDateService, WithdrawalService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.Source

class WithdrawalControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach
  with LisaConstants {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val lisaManager = "Z019283"
  val accountId = "ABC/12345"
  val transactionId = "1234567890"
  val validWithdrawalJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.withdrawal-charge.json")).mkString
  val validWithdrawalMinimumFieldsJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.withdrawal-charge.min.json")).mkString
  implicit val hc:HeaderCarrier = HeaderCarrier()

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockDateTimeService)

    when(mockAuthCon.authorise[Option[String]](any(),any())(any(), any())).thenReturn(Future(Some("1234")))
    when(mockDateTimeService.now()).thenReturn(new DateTime("2018-01-01"))
  }

  "the POST bonus payment endpoint" must {

    "return with status 201 created" when {

      "given a ReportWithdrawalChargeOnTimeResponse from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeOnTimeResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created"
        }
      }

      "given a ReportWithdrawalChargeLateResponse from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeLateResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction created - late notification"
        }
      }

      "given a ReportWithdrawalChargeSupersededResponse from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeSupersededResponse("1928374")))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe CREATED
          (contentAsJson(res) \ "data" \ "transactionId").as[String] mustBe "1928374"
          (contentAsJson(res) \ "data" \ "message").as[String] mustBe "Unauthorised withdrawal transaction superseded"
        }
      }

    }

    "return with status 404 not found" when {

      "given a ReportWithdrawalChargeAccountNotFound from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeAccountNotFound))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe NOT_FOUND
          (contentAsJson(res) \ "code").as[String] mustBe ErrorAccountNotFound.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorAccountNotFound.message
        }
      }

    }

    "return with status 500 internal server error" when {

      "an exception gets thrown" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenThrow(new RuntimeException("Test"))

        doRequest(validWithdrawalJson) { res =>
          reset(mockService) // removes the thenThrow

          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

      "given a RequestWithdrawalChargeError from the service layer" in {
        when(mockService.reportWithdrawalCharge(any(), any(), any())(any())).
          thenReturn(Future.successful(ReportWithdrawalChargeError))

        doRequest(validWithdrawalJson) { res =>
          status(res) mustBe INTERNAL_SERVER_ERROR
          (contentAsJson(res) \ "code").as[String] mustBe ErrorInternalServerError.errorCode
          (contentAsJson(res) \ "message").as[String] mustBe ErrorInternalServerError.message
        }
      }

    }

  }

  def doRequest(jsonString: String, lmrn: String = lisaManager)(callback: (Future[Result]) =>  Unit): Unit = {
    val res = SUT.reportWithdrawalCharge(lmrn, accountId).apply(FakeRequest(Helpers.PUT, "/").withHeaders(acceptHeader).
      withBody(AnyContentAsJson(Json.parse(jsonString))))

    callback(res)
  }

  val mockService: WithdrawalService = mock[WithdrawalService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]
  val mockDateTimeService: CurrentDateService = mock[CurrentDateService]

  val SUT = new WithdrawalController {
    override val service: WithdrawalService = mockService
    override val auditService: AuditService = mockAuditService
    override val authConnector: LisaAuthConnector = mockAuthCon
    override val dateTimeService: CurrentDateService = mockDateTimeService
  }

}
