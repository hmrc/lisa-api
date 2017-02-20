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

package uk.gov.hmrc.lisaapi.connectors

import uk.gov.hmrc.lisaapi.config.WSHttp
import uk.gov.hmrc.lisaapi.models.CreateLisaInvestorRequest
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost}

import scala.concurrent.Future

trait DesConnector extends ServicesConfig {

  val httpPost:HttpPost = WSHttp
  lazy val desUrl = baseUrl("des")

  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier): Future[String] = {
    ???
  }

}


object DesConnector extends DesConnector {

}