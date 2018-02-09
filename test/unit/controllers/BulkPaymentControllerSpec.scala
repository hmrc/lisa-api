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

import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.BulkPaymentController
import uk.gov.hmrc.lisaapi.models.GetBulkPaymentNotFoundResponse
import uk.gov.hmrc.lisaapi.services.{AuditService, BulkPaymentService}

import scala.concurrent.Future

class BulkPaymentControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  "Get Bulk Payment" must {
    "return not found" when {
      "the service returns a GetBulkPaymentNotFoundResponse" in {
        when(mockService.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBulkPaymentNotFoundResponse))

        val result = SUT.getBulkPayment("Z123456", "2018-01-01", "2018-01-01").
          apply(FakeRequest(Helpers.GET, "/").withHeaders(acceptHeader))

        status(result) mustBe NOT_FOUND

        val json = contentAsJson(result)

        (json \ "code").as[String] mustBe "PAYMENT_NOT_FOUND"
        (json \ "message").as[String] mustBe "No bonus payments have been made for this date range"
      }
    }
  }

  val mockService: BulkPaymentService = mock[BulkPaymentService]
  val mockAuthCon :LisaAuthConnector = mock[LisaAuthConnector]
  val SUT = new BulkPaymentController {
    override val service: BulkPaymentService = mockService
    override val authConnector = mockAuthCon
  }

}
