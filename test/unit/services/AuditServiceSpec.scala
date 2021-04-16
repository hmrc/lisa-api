/*
 * Copyright 2021 HM Revenue & Customs
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

import helpers.ServiceTestFixture
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.AuditService
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.http.Authorization

import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends ServiceTestFixture with BeforeAndAfter {

  implicit val hc: HeaderCarrier = HeaderCarrier(authorization = Some(Authorization("abcde")))

  val auditService: AuditService = new AuditService(mockAuditConnector, mockConfiguration, mockAppContext)

  "AuditService" must {

    before {
      when(mockAppContext.appName).thenReturn("lisa-api")
      reset(mockAuditConnector)
    }

    "build an audit event with the correct mandatory details" in {
      auditService.audit("investorCreated", "/create", Map("investorID" -> "1234567890"))
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.auditSource mustBe "lisa-api"
      event.auditType mustBe "investorCreated"
    }

    "build an audit event with the correct tags" in {
      auditService.audit("investorCreated", "/create", Map("investorID" -> "1234567890"))
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.tags must contain ("path" -> "/create")
      event.tags must contain ("transactionName" -> "investorCreated")
      event.tags must contain key "clientIP"
    }

    "build an audit event with the correct detail" in {
      auditService.audit("investorCreated", "/create", Map("investorId" -> "1234567890", "investorNINO" -> "AB123456D"))
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.detail must contain ("investorId" -> "1234567890")
      event.detail must contain ("investorNINO" -> "AB123456D")
    }

    "build an audit event with the correct detail when passed a RequestBonusPaymentRequest" in {
      val data = RequestBonusPaymentRequest(
        lifeEventId = Some("1234567891"),
        periodStartDate = new DateTime("2017-04-06"),
        periodEndDate = new DateTime("2017-05-05"),
        htbTransfer = Some(HelpToBuyTransfer(1f, 0f)),
        inboundPayments = InboundPayments(Some(4000f), 4000f, 4000f, 4000f),
        bonuses = Bonuses(1000f, 1000f, Some(1000f), "Life Event")
      )
      auditService.audit("investorCreated", "/create", data.toStringMap)
      val captor: ArgumentCaptor[DataEvent] = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.detail must contain ("lifeEventId" -> "1234567891")
      event.detail must contain ("periodStartDate" -> "2017-04-06")
      event.detail must contain ("periodEndDate" -> "2017-05-05")
      event.detail must contain ("htbTransferInForPeriod" -> "1.0")
      event.detail must contain ("htbTransferTotalYTD" -> "0.0")
      event.detail must contain ("newSubsForPeriod" -> "4000.0")
      event.detail must contain ("newSubsYTD" -> "4000.0")
      event.detail must contain ("totalSubsForPeriod" -> "4000.0")
      event.detail must contain ("totalSubsYTD" -> "4000.0")
      event.detail must contain ("bonusDueForPeriod" -> "1000.0")
      event.detail must contain ("totalBonusDueYTD" -> "1000.0")
      event.detail must contain ("bonusPaidYTD" -> "1000.0")
      event.detail must contain ("claimReason" -> "Life Event")
    }

    "send an event via the audit connector" in {
      auditService.audit("investorCreated", "/create", Map("investorId" -> "1234567890"))

      verify(mockAuditConnector).sendEvent(any())(any(), any())
    }
  }
}
