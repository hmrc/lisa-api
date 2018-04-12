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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.BulkPaymentService

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class BulkPaymentServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "Get Bulk Payment" must {

    "return payments" when {
      "the connector passes a success message with some payments" in {
        val successResponse = GetBulkPaymentSuccessResponse(lmrn, List(
          BulkPaymentPaid(950.2, date, "12345")
        ))

        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(successResponse))

        doRequest{ response =>
          response mustBe successResponse
        }
      }
    }

    "return payment not found" when {
      "the connector passes a not found response" in {
        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBulkPaymentNotFoundResponse))

        doRequest{ response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the connector passes a success message with no payments" in {
        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(GetBulkPaymentSuccessResponse(lmrn, Nil)))

        doRequest{ response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the connector returns a NOT_FOUND error" in {
        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("NOT_FOUND", "not found")))

        doRequest{ response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the connector returns a INVALID_CALCULATEACCRUEDINTEREST error" in {
        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVALID_CALCULATEACCRUEDINTEREST", "not found")))

        doRequest{ response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
      "the connector returns a INVALID_CUSTOMERPAYMENTINFORMATION error" in {
        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("INVALID_CUSTOMERPAYMENTINFORMATION", "not found")))

        doRequest{ response =>
          response mustBe GetBulkPaymentNotFoundResponse
        }
      }
    }

    "return error" when {
      "the connector returns any other error" in {
        when(mockDesConnector.getBulkPayment(any(), any(), any())(any())).
          thenReturn(Future.successful(DesFailureResponse("code", "reason")))

        doRequest{ response =>
          response mustBe GetBulkPaymentErrorResponse
        }
      }
    }

  }

  private def doRequest(callback: (GetBulkPaymentResponse) => Unit) = {
    val response = Await.result(SUT.getBulkPayment(lmrn, date, date)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val lmrn = "Z123456"
  val date = new DateTime("2018-01-01")
  val mockDesConnector = mock[DesConnector]
  object SUT extends BulkPaymentService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
