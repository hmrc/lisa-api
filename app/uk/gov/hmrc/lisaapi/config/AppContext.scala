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

package uk.gov.hmrc.lisaapi.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppContext @Inject()(config: Configuration, serviceConfig: ServicesConfig) {
  lazy val appName: String = serviceConfig.getString("appName")
  lazy val serviceLocatorUrl: String = serviceConfig.baseUrl("service-locator")
  lazy val registrationEnabled: Boolean = serviceConfig.getConfBool(s"microservice.services.service-locator.enabled", defBool = false)
  lazy val apiContext: String = serviceConfig.getString("api.context")
  lazy val baseUrl: String = serviceConfig.getString(s"baseUrl")
  lazy val v1apiStatus: String = serviceConfig.getString("api.status")
  lazy val v2apiStatus: String = serviceConfig.getString("api.statusv2")
  lazy val desAuthToken: String = serviceConfig.getString("desauthtoken")
  lazy val desUrlHeaderEnv: String =  serviceConfig.getString("environment")
  lazy val access: Option[Configuration] =  config.getOptional[Configuration](s"api.access")
  lazy val v1endpointsEnabled: Boolean = serviceConfig.getBoolean("api.endpointsEnabled")
  lazy val v2endpointsEnabled: Boolean = serviceConfig.getBoolean("api.endpointsEnabledv2")
  lazy val desUrl: String = serviceConfig.baseUrl("des")

  def endpointIsDisabled(endpoint: String): Boolean = {
    config.getOptional[Seq[String]]("api.disabledEndpoints").fold(false)(list => list.contains(endpoint))
  }
}
