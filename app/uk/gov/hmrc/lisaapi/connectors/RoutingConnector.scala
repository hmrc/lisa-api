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
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.models.des.DesResponse
import uk.gov.hmrc.lisaapi.models.hip.RoutingResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RoutingConnector @Inject() (
  context: AppContext,
  desConnector: DesConnector,
  hipConnector: HipConnector
)(using ec: ExecutionContext) {

  def getTransaction(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[RoutingResponse] =
    if (context.useHip) {
      hipConnector.getTransaction(lisaManager, accountId, transactionId)
    } else
      desConnector.getTransaction(lisaManager, accountId, transactionId)

  def getBonusOrWithdrawal(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] =
    desConnector.getBonusOrWithdrawal(lisaManager, accountId, transactionId)

}
