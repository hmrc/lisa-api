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

import play.api.Logger
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesAccountResponse, DesFailureResponse, DesLifeEventResponse}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait AccountService {
  val desConnector: DesConnector

  val INVESTOR_ACCOUNT_ALREADY_CLOSED = 63220
  val INVESTOR_ACCOUNT_NOT_FOUND = 63221

  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit hc: HeaderCarrier): Future[CreateLisaAccountResponse] = {
    val response = desConnector.createAccount(lisaManager, request)

    response map {
      case successResponse: DesAccountResponse => {
        Logger.debug("Matched DesAccountResponse")

        CreateLisaAccountSuccessResponse(successResponse.accountID)
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_NOT_FOUND" => CreateLisaAccountInvestorNotFoundResponse
          case "INVESTOR_ELIGIBILITY_CHECK_FAILED" => CreateLisaAccountInvestorNotEligibleResponse
          case "INVESTOR_COMPLIANCE_CHECK_FAILED" => CreateLisaAccountInvestorComplianceCheckFailedResponse
          case "INVESTOR_ACCOUNT_ALREADY_EXISTS" => CreateLisaAccountAlreadyExistsResponse
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" => CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse
          case _ => CreateLisaAccountErrorResponse
        }
      }
    }
  }

  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)(implicit hc: HeaderCarrier): Future[CreateLisaAccountResponse] = {
    val response = desConnector.transferAccount(lisaManager, request)

    response map {
      case successResponse: DesAccountResponse => {
        Logger.debug("Matched DesAccountResponse")

        CreateLisaAccountSuccessResponse(successResponse.accountID)
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_NOT_FOUND" => CreateLisaAccountInvestorNotFoundResponse
          case "INVESTOR_COMPLIANCE_CHECK_FAILED" => CreateLisaAccountInvestorComplianceCheckFailedResponse
          case "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST" => CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse
          case "INVESTOR_ACCOUNT_ALREADY_EXISTS" => CreateLisaAccountAlreadyExistsResponse
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" => CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse
          case _ => CreateLisaAccountErrorResponse
        }
      }
    }
  }

  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit hc: HeaderCarrier): Future[CloseLisaAccountResponse] = {

    val response = desConnector.closeAccount(lisaManager, accountId, request)

    response map {
      case successResponse: DesAccountResponse => {
        CloseLisaAccountSuccessResponse(accountId)
      }
      case failureResponse: DesFailureResponse => {
        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => CloseLisaAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNT_NOT_FOUND" => CloseLisaAccountNotFoundResponse
          case _ => CloseLisaAccountErrorResponse
        }
      }
    }
  }
}

object AccountService extends AccountService {
  override val desConnector: DesConnector = DesConnector
}