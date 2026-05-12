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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.helpers.ConnectorSpecHelper
import uk.gov.hmrc.lisaapi.models.*

import java.time.LocalDate
import scala.io.Source
import scala.util.Using

trait DesConnectorTestHelper extends ConnectorSpecHelper {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val validBonusPaymentResponseJson: String =
    Using.resource(
      Source.fromInputStream(getClass.getResourceAsStream("/json/request.valid.bonus-payment-response.json"))
    )(_.mkString)

  val createInvestorPayload: CreateLisaInvestorRequest =
    CreateLisaInvestorRequest("AB123456A", "A", "B", LocalDate.parse("2000-01-01"))

  val createAccountPayload: CreateLisaAccountCreationRequest =
    CreateLisaAccountCreationRequest("1234567890", "9876543210", LocalDate.parse("2000-01-01"))

  val transferAccountPayload: CreateLisaAccountTransferRequest = {
    val transferAccount = AccountTransfer("1234", "1234", LocalDate.parse("2000-01-01"))
    CreateLisaAccountTransferRequest(
      "Transferred",
      "1234567890",
      "9876543210",
      LocalDate.parse("2000-01-01"),
      transferAccount
    )
  }

  val closeAccountPayload: CloseLisaAccountRequest =
    CloseLisaAccountRequest("All funds withdrawn", LocalDate.parse("2000-01-01"))

  val updateFirstSubscriptionDatePayload: UpdateSubscriptionRequest =
    UpdateSubscriptionRequest(LocalDate.parse("2000-01-01"))

  val reportLifeEventPayload: ReportLifeEventRequest =
    ReportLifeEventRequest("LISA Investor Terminal Ill Health", LocalDate.parse("2000-01-01"))

  val requestBonusPaymentPayload: RequestBonusPaymentRequest =
    RequestBonusPaymentRequest(
      lifeEventId = Some("1234567891"),
      periodStartDate = LocalDate.parse("2017-04-06"),
      periodEndDate = LocalDate.parse("2017-05-05"),
      htbTransfer = Some(HelpToBuyTransfer(0, 0)),
      inboundPayments = InboundPayments(Some(4000), 4000, 4000, 4000),
      bonuses = Bonuses(1000, 1000, None, "Life Event")
    )

  val reportWithdrawalPayload: SupersededWithdrawalChargeRequest =
    SupersededWithdrawalChargeRequest(
      Some(250.00),
      LocalDate.parse("2017-12-06"),
      LocalDate.parse("2018-01-05"),
      1000.00,
      250.00,
      500.00,
      fundsDeductedDuringWithdrawal = true,
      Some(WithdrawalIncrease("2345678901", 250.00, 250.00, "Additional withdrawal")),
      "Superseded withdrawal"
    )

}
