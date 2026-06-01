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

package uk.gov.hmrc.lisaapi.services

import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture
import uk.gov.hmrc.lisaapi.models.GetTransactionSuccessResponse

import java.time.LocalDate
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RoutingTransactionServiceSpec extends BaseTestFixture {
  implicit val hc: HeaderCarrier = HeaderCarrier()
  
  val hipServiceMock = mock[HipTransactionService]
  val desServiceMock = mock[TransactionService]

  val transactionResponse = GetTransactionSuccessResponse(
    transactionId = "12345",
    paymentStatus = "Pending",
    paymentDueDate = Some(LocalDate.parse("2000-01-01")),
    transactionType = Some("Payment"),
    bonusDueForPeriod = Some(1.0)
  )

  "RoutingTransationService" must {
  
    "talk to HIP Transaction Service when useHip flag is true" in {
      val appContext = new AppContext(mockConfiguration, mockServicesConfig)
      val routingTransactionService: RoutingTransactionService = new RoutingTransactionService(appContext, desServiceMock, hipServiceMock)
      
      when(mockServicesConfig.getBoolean("features.hip")).thenReturn(true)
      when(hipServiceMock.getTransaction(anyString(), anyString(), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(transactionResponse))
      routingTransactionService.getTransaction("lisaManager", "accountNo", "tranId")
      verify(hipServiceMock, times(1)).getTransaction("lisaManager", "accountNo", "tranId")
    }
    
    "talk to DES Transaction Service when useHip flag is false" in {
      val appContext = new AppContext(mockConfiguration, mockServicesConfig)
      val routingTransactionService: RoutingTransactionService = new RoutingTransactionService(appContext, desServiceMock, hipServiceMock)

      when(mockServicesConfig.getBoolean("features.hip")).thenReturn(false)
      when(desServiceMock.getTransaction(anyString(), anyString(), anyString())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(transactionResponse))
      routingTransactionService.getTransaction("lisaManager", "accountNo", "tranId")
      verify(desServiceMock, times(1)).getTransaction("lisaManager", "accountNo", "tranId")
    }

  }
}
