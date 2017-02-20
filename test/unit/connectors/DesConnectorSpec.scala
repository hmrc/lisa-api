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

package unit.connectors

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.services.InvestorService
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse, Upstream5xxResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class DesConnectorSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "DesConnector" must {

    val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))

    "Return a Created Response" when {
      "Given a 201 response from DES" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(201)))

        val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be("Created")
      }
    }

    "Return a Error Response" when {
      "Given a 500 response from DES" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(500)))

        val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be("Error")
      }

      "An upstream exception is thrown" ignore {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
            .thenThrow(new RuntimeException("Upstream exceptions can be thrown by Http Verbs by default"))

        val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be("Error")
      }
    }

  }

  val mockHttpPost = mock[HttpPost]
  implicit val hc = HeaderCarrier()

  object SUT extends DesConnector {
    override val httpPost = mockHttpPost
  }
}
