/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.connectors

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.http.Fault
import play.api.libs.json.{Json, Writes}
import play.api.test.Helpers.*
import uk.gov.hmrc.http.{HeaderCarrier, RequestId}
import uk.gov.hmrc.lisaapi.models.*
import uk.gov.hmrc.lisaapi.models.des.*

import java.time.LocalDate
import java.util.UUID

class RoutingConnectorSpec extends DesConnectorTestHelper {

  lazy val hipConnector: HipConnector = injector.instanceOf[HipConnector]
  lazy val desConnector: DesConnector = injector.instanceOf[DesConnector]

  val managerPathDES = "/RESTAdapter/lisa/bonus-charge/manager/Z123456"
  val managerPathHIP = "/RESTAdapter/lisa/bonus-charge/manager/Z123456"
  val accTransPath   = "/accounts/ABC12345/transaction/123456"

  val getTransactionUrlDES = s"$managerPathDES$accTransPath"
  val getTransactionUrlHIP = s"$managerPathHIP$accTransPath"
  
  "Retrieve Transaction endpoint" must {

    "return a Hip response when UseHip feature is true" in {
      val json = ""
      stubForGet(getTransactionUrlHIP, SERVICE_UNAVAILABLE, json)
      val response = await(hipConnector.getTransaction("Z123456", "ABC12345", "123456"))

      response mustBe HipUnavailableResponse
      verifyDesGet(getTransactionUrlHIP, withOriginator = true)
    }

    "return a Des response when UseHip feature is false" in {
      stubForGet(getTransactionUrlDES, SERVICE_UNAVAILABLE, "")
      val response = await(desConnector.getTransaction("Z123456", "ABC12345", "123456"))

      response mustBe DesUnavailableResponse
      verifyDesGet(getTransactionUrlDES, withOriginator = true)
    }
    
  }

}
