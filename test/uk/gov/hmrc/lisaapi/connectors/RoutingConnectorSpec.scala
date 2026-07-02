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

import org.mockito.ArgumentMatchers.{any, anyString, eq => eqTo}
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

  val mockHipConnector: HipConnector = mock[HipConnector]
  val mockDesConnector: DesConnector = mock[DesConnector]

  "RoutingConnector" must {
    "delegate to the HIP connector when useHip flag is true" in {
      val routingConnector = new RoutingConnector(mockAppContext, mockDesConnector, mockHipConnector)
      when(mockAppContext.useHip).thenReturn(true)
      when(mockHipConnector.getTransaction(anyString(), anyString(), anyString())(eqTo(hc)))
        .thenReturn(Future.successful(HipGetTransactionPending(LocalDate.parse("2026-05-05"), None, None)))

      routingConnector.getTransaction("lisaManager", "accountNo", "tranId")
      verify(mockHipConnector, times(1)).getTransaction("lisaManager", "accountNo", "tranId")
    }

    "delegate to the DES connector when useHip flag is false" in {
      val routingConnector = new RoutingConnector(mockAppContext, mockDesConnector, mockHipConnector)
      when(mockAppContext.useHip).thenReturn(false)
      when(mockDesConnector.getTransaction(anyString(), anyString(), anyString())(eqTo(hc)))
        .thenReturn(Future.successful(DesGetTransactionPending(LocalDate.parse("2026-05-05"))))

      routingConnector.getTransaction("lisaManager", "accountNo", "tranId")

      verify(mockDesConnector, times(1)).getTransaction("lisaManager", "accountNo", "tranId")
    }

    "delegate to the DES connector for getBonusOrWithdrawal" in {
      val routingConnector = new RoutingConnector(mockAppContext, mockDesConnector, mockHipConnector)
      when(mockDesConnector.getBonusOrWithdrawal(anyString(), anyString(), anyString())(eqTo(hc)))
        .thenReturn(Future.successful(DesUnavailableResponse))

      routingConnector.getBonusOrWithdrawal("lisaManager", "accountNo", "tranId")

      verify(mockDesConnector, times(1)).getBonusOrWithdrawal("lisaManager", "accountNo", "tranId")
    }

  }

}
