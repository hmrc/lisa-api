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

package uk.gov.hmrc.lisaapi.services

import com.fasterxml.jackson.annotation.JsonValue
import play.api.libs.json.JsValue
import uk.gov.hmrc.lisaapi.models.{LisaAccount, LisaInvestor, LisaManager, TransferAccount}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LisaService {

  def createInvestor(lisaManager: String, lisaInvestor: LisaInvestor)(implicit hc: HeaderCarrier) : Future[JsValue]

  def createTransferAccount(lisaManager: String, lisaAccount: LisaAccount): Future[JsValue]

  def availableEndpoints(lisaManger: String) : Future[JsValue]
}

