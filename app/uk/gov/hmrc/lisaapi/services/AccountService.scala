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

import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

trait AccountService  {
  val desConnector: DesConnector

  val INVESTOR_NOT_FOUND = 63214
  val INVESTOR_NOT_ELIGIBLE = 63216
  val INVESTOR_COMPLIANCE_FAILED = 63217
  val INVESTOR_PREVIOUS_ACCOUNT_DOES_NOT_EXIST = 63218
  val INVESTOR_ACCOUNT_ALREADY_EXISTS = 63219

  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaAccountResponse] = {
    val response = desConnector.createAccount(lisaManager, request)
    val httpStatusOk = 200

    response map {
      case (`httpStatusOk`, Some(data)) => {
        (data.rdsCode, data.accountId) match {
          case (None, Some(accountId)) => CreateLisaAccountSuccessResponse(accountId)
          case (Some(INVESTOR_NOT_FOUND), _) => CreateLisaAccountInvestorNotFoundResponse
          case (Some(INVESTOR_NOT_ELIGIBLE), _) => CreateLisaAccountInvestorNotEligibleResponse
          case (Some(INVESTOR_COMPLIANCE_FAILED), _) => CreateLisaAccountInvestorComplianceCheckFailedResponse
          case (Some(INVESTOR_PREVIOUS_ACCOUNT_DOES_NOT_EXIST), _) => CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse
          case (Some(INVESTOR_ACCOUNT_ALREADY_EXISTS), _) => CreateLisaAccountAlreadyExistsResponse
          case (_, _) => CreateLisaAccountErrorResponse
        }
      }
      case (_, _) => CreateLisaAccountErrorResponse
    }
  }

  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit hc: HeaderCarrier) : Future[CloseLisaAccountResponse] = {
    val response = desConnector.closeAccount(lisaManager, accountId, request)
    val httpStatusOk = 200

    response map {
      case (`httpStatusOk`, Some(data)) => {
        (data.rdsCode, data.accountId) match {
          case (None, Some(accountId)) => CloseLisaAccountSuccessResponse(accountId)
          case (_, _) => CloseLisaAccountErrorResponse
        }
      }
      case (_, _) => CloseLisaAccountErrorResponse
    }
  }

}

object AccountService extends AccountService {
  override val desConnector: DesConnector = DesConnector
}