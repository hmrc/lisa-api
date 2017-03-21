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
import org.mockito.Matchers.any
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito.when
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.ReportLifeEventRequest
import uk.gov.hmrc.lisaapi.models.des.{DesResponse, DesSuccessResponse}
import uk.gov.hmrc.lisaapi.services.LifeEventService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

/**
  * Created by mark on 21/03/17.
  */
class LifeEventServiceSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  "LifeEventService" must {

    "return a Success Reponse" when {
      "given and lifeEventId from the DES connector" in {

        when(mockDesConnector.reportLifeEvent(any(), any(),any())(any())).thenReturn(Future.successful((200,Some(DesSuccessResponse("AB123456")))))

        doRequest{response => response mustBe DesSuccessResponse("AB123456")}
      }
    }

    "return a Not_"
  }

  private def doRequest(callback: (DesResponse) => Unit) = {
    val request = ReportLifeEventRequest("1234567890", "Z543210", "LISA Investor Terminal Ill Health", new DateTime("2017-04-06"))
    val response = Await.result(SUT.reportLifeEvent("Z019283", "192837", request)(HeaderCarrier()), Duration.Inf)

    callback(response)
  }

  val mockDesConnector = mock[DesConnector]
  object SUT extends LifeEventService {
    override val desConnector: DesConnector = mockDesConnector
  }
}
