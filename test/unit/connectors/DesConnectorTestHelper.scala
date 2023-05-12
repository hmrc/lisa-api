/*
 * Copyright 2023 HM Revenue & Customs
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

import helpers.BaseTestFixture
import org.joda.time.DateTime
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des.{DesFailure, DesResponse}
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.io.Source

trait DesConnectorTestHelper extends BaseTestFixture with GuiceOneAppPerSuite {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val validBonusPaymentResponseJson: String =
    Source.fromInputStream(getClass.getResourceAsStream("/json/request.valid.bonus-payment-response.json")).mkString

  val desConnector: DesConnector = new DesConnector(mockHttp, mockAppContext)

  def doCreateInvestorRequest(callback: DesResponse => Unit): Unit = {
    val request  = CreateLisaInvestorRequest("AB123456A", "A", "B", new DateTime("2000-01-01"))
    val response = Await.result(desConnector.createInvestor("Z019283", request), Duration.Inf)

    callback(response)
  }

  def doCreateAccountRequest(callback: DesResponse => Unit): Unit = {
    val request  = CreateLisaAccountCreationRequest("1234567890", "9876543210", new DateTime("2000-01-01"))
    val response = Await.result(desConnector.createAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  def doTransferAccountRequest(callback: DesResponse => Unit): Unit = {
    val transferAccount = AccountTransfer("1234", "1234", new DateTime("2000-01-01"))
    val request         = CreateLisaAccountTransferRequest(
      "Transferred",
      "1234567890",
      "9876543210",
      new DateTime("2000-01-01"),
      transferAccount
    )
    val response        = Await.result(desConnector.transferAccount("Z019283", request), Duration.Inf)

    callback(response)
  }

  def doCloseAccountRequest(callback: DesResponse => Unit): Unit = {
    val request  = CloseLisaAccountRequest("All funds withdrawn", new DateTime("2000-01-01"))
    val response = Await.result(desConnector.closeAccount("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  def doReinstateAccountRequest(callback: DesResponse => Unit): Unit = {
    val response = Await.result(desConnector.reinstateAccount("Z123456", "ABC12345"), Duration.Inf)

    callback(response)
  }

  def updateFirstSubscriptionDateRequest(callback: DesResponse => Unit): Unit = {
    val request  = UpdateSubscriptionRequest(new DateTime("2000-01-01"))
    val response = Await.result(desConnector.updateFirstSubDate("Z019283", "123456789", request), Duration.Inf)

    callback(response)
  }

  def doReportLifeEventRequest(callback: DesResponse => Unit): Unit = {
    val request  = ReportLifeEventRequest("LISA Investor Terminal Ill Health", new DateTime("2000-01-01"))
    val response = Await.result(desConnector.reportLifeEvent("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  def doRetrieveLifeEventRequest(callback: Either[DesFailure, Seq[GetLifeEventItem]] => Unit): Unit = {
    val response = Await.result(desConnector.getLifeEvent("Z123456", "ABC12345", "1234567890"), Duration.Inf)

    callback(response)
  }

  def doRequestBonusPaymentRequest(callback: DesResponse => Unit): Unit = {
    val request = RequestBonusPaymentRequest(
      lifeEventId = Some("1234567891"),
      periodStartDate = new DateTime("2017-04-06"),
      periodEndDate = new DateTime("2017-05-05"),
      htbTransfer = Some(HelpToBuyTransfer(0, 0)),
      inboundPayments = InboundPayments(Some(4000), 4000, 4000, 4000),
      bonuses = Bonuses(1000, 1000, None, "Life Event")
    )

    val response = Await.result(desConnector.requestBonusPayment("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }

  def doRetrieveBonusPaymentRequest(callback: DesResponse => Unit): Unit = {
    val response = Await.result(desConnector.getBonusOrWithdrawal("Z123456", "ABC12345", "123456"), Duration.Inf)

    callback(response)
  }

  def doRetrieveTransactionRequest(callback: DesResponse => Unit): Unit = {
    val response = Await.result(desConnector.getTransaction("Z123456", "ABC12345", "123456"), Duration.Inf)

    callback(response)
  }

  def doRetrieveBulkPaymentRequest(callback: DesResponse => Unit): Unit = {
    val response = Await.result(
      desConnector.getBulkPayment("Z123456", new DateTime("2018-01-01"), new DateTime("2018-01-01")),
      Duration.Inf
    )

    callback(response)
  }

  def doRetrieveAccountRequest(callback: DesResponse => Unit): Unit = {
    val response = Await.result(desConnector.getAccountInformation("Z123456", "123456"), Duration.Inf)

    callback(response)
  }

  def doReportWithdrawalRequest(callback: DesResponse => Unit): Unit = {
    val request = SupersededWithdrawalChargeRequest(
      Some(250.00),
      new DateTime("2017-12-06"),
      new DateTime("2018-01-05"),
      1000.00,
      250.00,
      500.00,
      fundsDeductedDuringWithdrawal = true,
      Some(
        WithdrawalIncrease(
          "2345678901",
          250.00,
          250.00,
          "Additional withdrawal"
        )
      ),
      "Superseded withdrawal"
    )

    val response = Await.result(desConnector.reportWithdrawalCharge("Z123456", "ABC12345", request), Duration.Inf)

    callback(response)
  }
}
