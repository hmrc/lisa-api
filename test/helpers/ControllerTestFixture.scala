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

package helpers

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import play.api.mvc.{ControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.LisaMetrics
import uk.gov.hmrc.lisaapi.models.AnnualReturnValidator
import uk.gov.hmrc.lisaapi.services._
import uk.gov.hmrc.lisaapi.utils.{BonusPaymentValidator, ErrorConverter, WithdrawalChargeValidator}

trait ControllerTestFixture extends BaseTestFixture with GuiceOneAppPerSuite with BeforeAndAfterEach with LisaConstants {

  val injector: Injector = app.injector
  val mockControllerComponents: ControllerComponents = injector.instanceOf[ControllerComponents]
  val mockParser: PlayBodyParsers = injector.instanceOf[PlayBodyParsers]

  val mockAccountService: AccountService = mock[AccountService]
  val mockAuditService: AuditService = mock[AuditService]
  val mockLifeEventService: LifeEventService = mock[LifeEventService]
  val mockDateTimeService: CurrentDateService = mock[CurrentDateService]
  val mockBonusPaymentService: BonusPaymentService = mock[BonusPaymentService]
  val mockBonusOrWithdrawalService: BonusOrWithdrawalService = mock[BonusOrWithdrawalService]
  val mockBulkPaymentService: BulkPaymentService = mock[BulkPaymentService]
  val mockInvestorService: InvestorService = mock[InvestorService]
  val mockReinstateAccountService: ReinstateAccountService = mock[ReinstateAccountService]
  val mockTransactionService: TransactionService = mock[TransactionService]
  val mockUpdateSubscritionService: UpdateSubscriptionService = mock[UpdateSubscriptionService]
  val mockWithdrawalService: WithdrawalService = mock[WithdrawalService]

  val mockAuthConnector: AuthConnector = mock[AuthConnector]

  val mockErrorConverter: ErrorConverter = mock[ErrorConverter]

  val mockLisaMetrics: LisaMetrics = mock[LisaMetrics]
  
  val mockAnnualReturnValidator: AnnualReturnValidator = mock[AnnualReturnValidator]
  val mockBonusPaymentValidator: BonusPaymentValidator = mock[BonusPaymentValidator]
  val mockWithdrawalChargeValidator: WithdrawalChargeValidator = mock[WithdrawalChargeValidator]

}
