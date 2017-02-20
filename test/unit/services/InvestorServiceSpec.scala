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

package unit.services

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.lisaapi.services.InvestorService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class InvestorServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite {

  "InvestorService" must {

    "Return a Created Response" when {

      "Given a successful future from the DES connector" in {
        when(mockDesConnector.createInvestor(any(), any())(any())).thenReturn(Future.successful("Created"))
        val request = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))

        val result = Await.result(SUT.createInvestor("Z019283", request)(HeaderCarrier()), Duration.Inf)
        result mustBe "Created"
      }

    }

  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends InvestorService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
