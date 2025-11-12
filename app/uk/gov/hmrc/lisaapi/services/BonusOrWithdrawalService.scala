/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesUnavailableResponse}

import scala.concurrent.{ExecutionContext, Future}

class BonusOrWithdrawalService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def getBonusOrWithdrawal(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[GetBonusOrWithdrawalResponse] =
    desConnector.getBonusOrWithdrawal(lisaManager, accountId, transactionId) map {
      case successResponse: GetBonusOrWithdrawalResponse =>
        logger.info(s"[BonusOrWithdrawalService][getBonusOrWithdrawal] Matched GetBonusOrWithdrawalResponse for lisaManager : $lisaManager")
        successResponse
      case DesUnavailableResponse                        =>
        logger.warn(s"[BonusOrWithdrawalService][getBonusOrWithdrawal] Matched DesUnavailableResponse for lisaManager : $lisaManager")
        GetBonusOrWithdrawalServiceUnavailableResponse
      case failureResponse: DesFailureResponse           =>
        logger.error(s"[BonusOrWithdrawalService][getBonusOrWithdrawal] Matched DesFailureResponse for lisaManager : $lisaManager and the code is : ${failureResponse.code}")
        failureResponse.code match {
          case "TRANSACTION_ID_NOT_FOUND"     => GetBonusOrWithdrawalTransactionNotFoundResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetBonusOrWithdrawalInvestorNotFoundResponse
          case _                              =>
            logger.error(s"[BonusOrWithdrawalService][getBonusOrWithdrawal] Get bonus payment or withdrawal returned error: ${failureResponse.code} for lisaManager : $lisaManager")
            GetBonusOrWithdrawalErrorResponse
        }
    }
}
