/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.play.bootstrap.config.{RunMode, ServicesConfig}

@Singleton
class AppContext @Inject()(runModeConfiguration: Configuration, runMode: RunMode) extends ServicesConfig(runModeConfiguration: Configuration, runMode: RunMode) {
  lazy val appName = runModeConfiguration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl = runModeConfiguration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val serviceLocatorUrl: String = baseUrl("service-locator")
  lazy val registrationEnabled: Boolean = runModeConfiguration.getBoolean(s"microservice.services.service-locator.enabled").getOrElse(false)
  lazy val apiContext = runModeConfiguration.getString("api.context").getOrElse(throw new RuntimeException(s"Missing Key api.context"))
  lazy val baseUrl = runModeConfiguration.getString(s"baseUrl").getOrElse(throw new RuntimeException(s"Missing Key baseUrl"))
  lazy val v1apiStatus = runModeConfiguration.getString("api.status").getOrElse(throw new RuntimeException(s"Missing Key api.status"))
  lazy val v2apiStatus = runModeConfiguration.getString("api.statusv2").getOrElse(throw new RuntimeException(s"Missing Key api.statusv2"))
  lazy val desAuthToken = runModeConfiguration.getString("desauthtoken").getOrElse(throw new RuntimeException(s"Missing Key desauthtoken"))
  lazy val desUrlHeaderEnv: String =  runModeConfiguration.getString("environment").getOrElse(throw new RuntimeException(s"Missing Key environment"))
  lazy val access = runModeConfiguration.getConfig(s"api.access")
  lazy val v1endpointsEnabled = runModeConfiguration.getBoolean("api.endpointsEnabled").getOrElse(throw new RuntimeException(s"Missing key api.endpointsEnabled"))
  lazy val v2endpointsEnabled = runModeConfiguration.getBoolean("api.endpointsEnabledv2").getOrElse(throw new RuntimeException(s"Missing key api.endpointsEnabledv2"))

  def endpointIsDisabled(endpoint: String): Boolean = {
    runModeConfiguration.getStringList("api.disabledEndpoints").fold(false)(list => list.contains(endpoint))
  }
}
