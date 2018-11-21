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

package uk.gov.hmrc.lisaapi.config


import play.api.Play._
import uk.gov.hmrc.play.config.ServicesConfig

object AppContext extends ServicesConfig {
  lazy val appName = current.configuration.getString("appName").getOrElse(throw new RuntimeException("appName is not configured"))
  lazy val appUrl = current.configuration.getString("appUrl").getOrElse(throw new RuntimeException("appUrl is not configured"))
  lazy val serviceLocatorUrl: String = baseUrl("service-locator")
  lazy val registrationEnabled: Boolean = current.configuration.getBoolean(s"microservice.services.service-locator.enabled").getOrElse(false)
  lazy val apiContext = current.configuration.getString("api.context").getOrElse(throw new RuntimeException(s"Missing Key api.context"))
  lazy val baseUrl = current.configuration.getString(s"baseUrl").getOrElse(throw new RuntimeException(s"Missing Key baseUrl"))
  lazy val v1apiStatus = current.configuration.getString("api.status").getOrElse(throw new RuntimeException(s"Missing Key api.status"))
  lazy val v2apiStatus = current.configuration.getString("api.statusv2").getOrElse(throw new RuntimeException(s"Missing Key api.statusv2"))
  lazy val desAuthToken = current.configuration.getString("desauthtoken").getOrElse(throw new RuntimeException(s"Missing Key desauthtoken"))
  lazy val desUrlHeaderEnv: String =  current.configuration.getString("environment").getOrElse(throw new RuntimeException(s"Missing Key environment"))
  lazy val access = current.configuration.getConfig(s"api.access")
  lazy val v1endpointsEnabled = current.configuration.getBoolean("api.endpointsEnabled").getOrElse(throw new RuntimeException(s"Missing key api.endpointsEnabled"))
  lazy val v2endpointsEnabled = current.configuration.getBoolean("api.endpointsEnabledv2").getOrElse(throw new RuntimeException(s"Missing key api.endpointsEnabledv2"))
}
