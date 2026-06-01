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

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture
import uk.gov.hmrc.lisaapi.models.des.*
import uk.gov.hmrc.lisaapi.models.hip.HipGetTransactionPending

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RoutingConnectorSpec extends BaseTestFixture {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val hipBaseTransactionUrl = "/RESTAdapter/lisa/bonus-charge/manager"
  private val desBaseTransactionUrl = "/lifetime-isa/manager"

  val hipTransactionUrl = s"$hipBaseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456/bonusChargeDetails"
  val desTransactionUrl = s"$desBaseTransactionUrl/Z123456/accounts/ABC12345/transaction/123456"

  val hipConnectorMock = mock[HipConnector]
  val desConnectorMock = mock[DesConnector]

  "RoutingConnector" must {
    "talk to HIP when useHip flag is true" in {
      val appContext       = new AppContext(mockConfiguration, mockServicesConfig)
      val routingConnector = new RoutingConnector(appContext, desConnectorMock, hipConnectorMock)
      when(mockServicesConfig.getBoolean("features.hip")).thenReturn(true)
      when(hipConnectorMock.getTransaction(anyString(), anyString(), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(HipGetTransactionPending(LocalDate.parse("2026-05-05"))))

      routingConnector.getTransaction("lisaManager", "accountNo", "tranId")
      verify(hipConnectorMock, times(1)).getTransaction("lisaManager", "accountNo", "tranId")
    }

    "talk to DES when useHip flag is false" in {
      val appContext       = new AppContext(mockConfiguration, mockServicesConfig)
      val routingConnector = new RoutingConnector(appContext, desConnectorMock, hipConnectorMock)
      when(mockServicesConfig.getBoolean("features.hip")).thenReturn(false)
      when(desConnectorMock.getTransaction(anyString(), anyString(), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(DesGetTransactionPending(LocalDate.parse("2026-05-05"))))

      routingConnector.getTransaction("lisaManager", "accountNo", "tranId")

      verify(desConnectorMock, times(1)).getTransaction("lisaManager", "accountNo", "tranId")
    }

  }

}
