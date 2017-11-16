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
import org.mockito.ArgumentCaptor
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import uk.gov.hmrc.lisaapi.models.{Bonuses, HelpToBuyTransfer, InboundPayments, RequestBonusPaymentRequest}
import uk.gov.hmrc.lisaapi.services.AuditService
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.model.DataEvent

import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.http.HeaderCarrier

class AuditServiceSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter {

  "AuditService" must {

    before {
      reset(mockAuditConnector)
    }

    "build an audit event with the correct mandatory details" in {
      val res = SUT.audit("investorCreated", "/create", Map("investorID" -> "1234567890"))
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.auditSource mustBe "lisa-api"
      event.auditType mustBe "investorCreated"
    }

    "build an audit event with the correct tags" in {
      val res = SUT.audit("investorCreated", "/create", Map("investorID" -> "1234567890"))
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.tags must contain ("path" -> "/create")
      event.tags must contain ("transactionName" -> "investorCreated")
      event.tags must contain key "clientIP"
    }

    "build an audit event with the correct detail" in {
      val res = SUT.audit("investorCreated", "/create", Map("investorId" -> "1234567890", "investorNINO" -> "AB123456D"))
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])

      verify(mockAuditConnector).sendEvent(captor.capture())(any(), any())

      val event = captor.getValue

      event.detail must contain ("investorId" -> "1234567890")
      event.detail must contain ("investorNINO" -> "AB123456D")
      event.detail must contain key "Authorization"
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
      val res = SUT.audit("investorCreated", "/create", data.toStringMap)
      val captor = ArgumentCaptor.forClass(classOf[DataEvent])

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

      event.detail must contain key "Authorization"
    }

    "send an event via the audit connector" in {
      val event = SUT.audit("investorCreated", "/create", Map("investorId" -> "1234567890"))

      verify(mockAuditConnector).sendEvent(any())(any(), any())
    }
  }

  implicit val hc:HeaderCarrier = HeaderCarrier()
  val mockAuditConnector = mock[AuditConnector]

  object SUT extends AuditService {
    override val connector:AuditConnector = mockAuditConnector
  }
}
