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

import play.api.Logger
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

trait AccountService {
  val desConnector: DesConnector

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
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse
          case _ => CreateLisaAccountErrorResponse
        }
      }
    }
  }

  def getAccount(lisaManager: String, accountId: String)(implicit hc: HeaderCarrier): Future[GetLisaAccountResponse] = {
    val response: Future[DesResponse] = desConnector.getAccountInformation(lisaManager, accountId)

    response map {
      case res: DesGetAccountResponse => {
        Logger.debug("Matched DesAccountResponse")

        val transferAccount = res.transferAccount.map {data =>
          GetLisaAccountTransferAccount(data.transferredFromAccountId, data.transferredFromLMRN, data.transferInDate)
        }

        GetLisaAccountSuccessResponse(
          res.accountId,
          res.investorId,
          res.creationReason,
          res.firstSubscriptionDate,
          res.accountStatus,
          res.accountClosureReason,
          res.closureDate,
          transferAccount
        )
      }

      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetLisaAccountDoesNotExistResponse
          case _ => GetLisaAccountErrorResponse
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
            case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => CreateLisaAccountInvestorAccountAlreadyClosedOrVoidedResponse
            case _ => CreateLisaAccountErrorResponse
          }
        }
      }
    }

    def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit hc: HeaderCarrier): Future[CloseLisaAccountResponse] = {

      val response = desConnector.closeAccount(lisaManager, accountId, request)

      response map {
        case DesEmptySuccessResponse => {
          CloseLisaAccountSuccessResponse(accountId)
        }
        case failureResponse: DesFailureResponse => {
          failureResponse.code match {
            case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => CloseLisaAccountAlreadyClosedResponse
            case "INVESTOR_ACCOUNTID_NOT_FOUND" => CloseLisaAccountNotFoundResponse
            case "CANCELLATION_PERIOD_EXCEEDED" => CloseLisaAccountCancellationPeriodExceeded
            case "ACCOUNT_WITHIN_CANCELLATION_PERIOD" => CloseLisaAccountWithinCancellationPeriod
            case "BONUS_REPAYMENT_REQUIRED" => CloseLisaAccountBonusPaymentRequired
            case _ => CloseLisaAccountErrorResponse
          }
        }
      }
    }
  }


  object AccountService extends AccountService {
    override val desConnector: DesConnector = DesConnector
  }