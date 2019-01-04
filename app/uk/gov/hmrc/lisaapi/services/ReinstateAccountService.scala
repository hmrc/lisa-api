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

package uk.gov.hmrc.lisaapi.services

import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ReinstateAccountService {
  val desConnector: DesConnector

  def reinstateAccountService(lisaManager: String, accountId: AccountId)(implicit hc: HeaderCarrier): Future[ReinstateLisaAccountResponse] = {
    val response = desConnector.reinstateAccount(lisaManager, accountId)

    response map {
      case successResponse: DesReinstateAccountSuccessResponse => {
        Logger.debug("Reinstate account success response")
        ReinstateLisaAccountSuccessResponse(successResponse.code, successResponse.reason)
      }
      case DesUnavailableResponse => {
        Logger.debug("Reinstate account returned service unavailable")
        ReinstateLisaAccountServiceUnavailableResponse
      }
      case failureResponse: DesFailureResponse => {
        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => ReinstateLisaAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => ReinstateLisaAccountNotFoundResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => ReinstateLisaAccountAlreadyCancelledResponse
          case "INVESTOR_ACCOUNT_ALREADY_OPEN" => ReinstateLisaAccountAlreadyOpenResponse
          case "INVESTOR_COMPLIANCE_CHECK_FAILED" => ReinstateLisaAccountInvestorComplianceCheckFailedResponse
          case _ => {
            Logger.warn(s"Reinstate account returned error ${failureResponse.code}")
            ReinstateLisaAccountErrorResponse
          }
        }
      }
    }
  }
}


object ReinstateAccountService extends ReinstateAccountService {
  override val desConnector: DesConnector = DesConnector
}






