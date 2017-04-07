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

  val INVESTOR_ACCOUNT_ALREADY_CLOSED = 63220
  val INVESTOR_ACCOUNT_NOT_FOUND = 63221

  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaAccountResponse] = {
    val response = desConnector.createAccount(lisaManager, request)
    Future.successful(CreateLisaAccountInvestorNotEligibleResponse)
  }

  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaAccountResponse] = {
    val response = desConnector.transferAccount(lisaManager, request)
    Future.successful(CreateLisaAccountInvestorNotEligibleResponse)
  }

  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit hc: HeaderCarrier) : Future[CloseLisaAccountResponse] = {
    val response = desConnector.closeAccount(lisaManager, accountId, request)
    val httpStatusOk = 200

    response map {
      case (`httpStatusOk`, Some(data)) => {
        (data.rdsCode, data.accountId) match {
          case (None, Some(accountId)) => CloseLisaAccountSuccessResponse(accountId)
          case (Some(INVESTOR_ACCOUNT_ALREADY_CLOSED), _) => CloseLisaAccountAlreadyClosedResponse
          case (Some(INVESTOR_ACCOUNT_NOT_FOUND), _) => CloseLisaAccountNotFoundResponse
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