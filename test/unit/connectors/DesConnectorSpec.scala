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
import play.api.libs.json.Json
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.models.des.DesCreateInvestorResponse
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class DesConnectorSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))

  "DesConnector" must {

    "Return a status code of 200" when {
      "Given a 200 response from DES" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 200, responseJson = None)))

        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be((200, None))
      }
    }

    "Return no DesCreateInvestorResponse" when {
      "The DES response has no json body" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 503, responseJson = None)))

        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be((503, None))
      }
    }

    "Return any empty DesCreateInvestorResponse" when {
      "The DES response has a json body that is in an incorrect format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 200, responseJson = Some(Json.parse("""[1,2,3]""")))))

        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be((200, Some(DesCreateInvestorResponse(None, None))))
      }
    }

    "Return a populated DesCreateInvestorResponse" when {
      "The DES response has a json body that is in the correct format" in {
        when(mockHttpPost.POST[CreateLisaInvestorRequest, HttpResponse](any(), any(), any())(any(), any(), any()))
          .thenReturn(Future.successful(HttpResponse(responseStatus = 200, responseJson = Some(Json.parse("""{"rdsCode":12345, "investorId": "AB123456"}""")))))

        val result = Await.result(SUT.createInvestor("Z019283", request), Duration.Inf)

        result must be((200, Some(DesCreateInvestorResponse(rdsCode = Some(12345), investorId = Some("AB123456")))))
      }
    }

  }

  val mockHttpPost = mock[HttpPost]
  implicit val hc = HeaderCarrier()

  object SUT extends DesConnector {
    override val httpPost = mockHttpPost
  }
}
