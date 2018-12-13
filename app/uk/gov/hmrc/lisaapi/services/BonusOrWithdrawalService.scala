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

package uk.gov.hmrc.lisaapi.services

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesResponse}

import scala.concurrent.{ExecutionContext, Future}

class BonusOrWithdrawalService @Inject()(desConnector: DesConnector)(implicit ec: ExecutionContext) {

  def getBonusOrWithdrawal(lisaManager: String, accountId: String, transactionId: String)
                          (implicit hc: HeaderCarrier): Future[GetBonusOrWithdrawalResponse] = {

    val response: Future[DesResponse] = desConnector.getBonusOrWithdrawal(lisaManager, accountId, transactionId)

    response map {
      case successResponse: GetBonusOrWithdrawalResponse => {
        Logger.debug("Matched GetBonusOrWithdrawalResponse")

        successResponse
      }

      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "TRANSACTION_ID_NOT_FOUND" => GetBonusOrWithdrawalTransactionNotFoundResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetBonusOrWithdrawalInvestorNotFoundResponse
          case _ => {
            Logger.warn(s"Get bonus payment or withdrawal returned error: ${failureResponse.code}")
            GetBonusOrWithdrawalErrorResponse
          }
        }
      }
    }
  }
}
