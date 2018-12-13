/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi

import play.api.{Configuration, Environment}
import play.api.inject.{Binding, Module}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.{AppContext, LisaAuthConnector, WSHttp}
import uk.gov.hmrc.lisaapi.models.AnnualReturnValidator
import uk.gov.hmrc.lisaapi.services._
import uk.gov.hmrc.lisaapi.utils.BonusPaymentValidator

class LisaModule extends Module {
  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[AuthConnector].to[LisaAuthConnector],
      bind[AppContext] toInstance AppContext,
      bind[AnnualReturnValidator] toInstance AnnualReturnValidator,
      bind[BonusPaymentValidator] toInstance BonusPaymentValidator,
      bind[CurrentDateService] toInstance CurrentDateService,
      bind[WSHttp] toInstance WSHttp
    )
  }
}
