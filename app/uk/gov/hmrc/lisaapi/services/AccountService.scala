/*
 * Copyright 2023 HM Revenue & Customs
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
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class AccountService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit
    hc: HeaderCarrier
  ): Future[CreateLisaAccountResponse] =
    desConnector.createAccount(lisaManager, request) map {
      case successResponse: DesAccountResponse =>
        logger.debug("Matched DesAccountResponse")
        CreateLisaAccountSuccessResponse(successResponse.accountID)
      case DesUnavailableResponse              =>
        logger.debug("Matched DesUnavailableResponse")
        CreateLisaAccountServiceUnavailableResponse
      case failureResponse: DesFailureResponse =>
        logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        failureResponse.code match {
          case "INVESTOR_NOT_FOUND"                 => CreateLisaAccountInvestorNotFoundResponse
          case "INVESTOR_ELIGIBILITY_CHECK_FAILED"  => CreateLisaAccountInvestorNotEligibleResponse
          case "INVESTOR_COMPLIANCE_CHECK_FAILED"   => CreateLisaAccountInvestorComplianceCheckFailedResponse
          case "INVESTOR_ACCOUNT_ALREADY_EXISTS"    => CreateLisaAccountAlreadyExistsResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => CreateLisaAccountInvestorAccountAlreadyCancelledResponse
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED"    => CreateLisaAccountInvestorAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_VOID"      => CreateLisaAccountInvestorAccountAlreadyVoidResponse
          case _                                    =>
            logger.warn(s"Create account returned error: ${failureResponse.code}")
            CreateLisaAccountErrorResponse
        }
    }

  def getAccount(lisaManager: String, accountId: String)(implicit hc: HeaderCarrier): Future[GetLisaAccountResponse] =
    desConnector.getAccountInformation(lisaManager, accountId) map {
      case res: GetLisaAccountSuccessResponse  =>
        logger.debug("Matched GetLisaAccountSuccessResponse")
        res
      case DesUnavailableResponse              =>
        logger.debug("Matched GetLisaAccountServiceUnavailable")
        GetLisaAccountServiceUnavailable
      case failureResponse: DesFailureResponse =>
        logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        failureResponse.code match {
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => GetLisaAccountDoesNotExistResponse
          case _                              =>
            logger.warn(s"Get account returned error: ${failureResponse.code}")
            GetLisaAccountErrorResponse
        }
    }

  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)(implicit
    hc: HeaderCarrier
  ): Future[CreateLisaAccountResponse] =
    desConnector.transferAccount(lisaManager, request) map {
      case successResponse: DesAccountResponse =>
        logger.debug("Matched DesAccountResponse")
        CreateLisaAccountSuccessResponse(successResponse.accountID)
      case DesUnavailableResponse              =>
        logger.debug("Matched DesUnavailableResponse")
        CreateLisaAccountServiceUnavailableResponse
      case failureResponse: DesFailureResponse =>
        logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        failureResponse.code match {
          case "INVESTOR_NOT_FOUND"                       => CreateLisaAccountInvestorNotFoundResponse
          case "INVESTOR_COMPLIANCE_CHECK_FAILED"         => CreateLisaAccountInvestorComplianceCheckFailedResponse
          case "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST" =>
            CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse
          case "INVESTOR_ACCOUNT_ALREADY_EXISTS"          => CreateLisaAccountAlreadyExistsResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED"       => CreateLisaAccountInvestorAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED"          => CreateLisaAccountInvestorAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_VOID"            => CreateLisaAccountInvestorAccountAlreadyVoidResponse
          case _                                          =>
            logger.warn(s"Transfer account returned error: ${failureResponse.code}")
            CreateLisaAccountErrorResponse
        }
    }

  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit
    hc: HeaderCarrier
  ): Future[CloseLisaAccountResponse] =
    desConnector.closeAccount(lisaManager, accountId, request) map {
      case DesEmptySuccessResponse             => CloseLisaAccountSuccessResponse(accountId)
      case DesUnavailableResponse              => CloseLisaAccountServiceUnavailable
      case failureResponse: DesFailureResponse =>
        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_VOID"      => CloseLisaAccountAlreadyVoidResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => CloseLisaAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED"    => CloseLisaAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND"       => CloseLisaAccountNotFoundResponse
          case "CANCELLATION_PERIOD_EXCEEDED"       => CloseLisaAccountCancellationPeriodExceeded
          case "ACCOUNT_WITHIN_CANCELLATION_PERIOD" => CloseLisaAccountWithinCancellationPeriod
          case _                                    =>
            logger.warn(s"Close account returned error: ${failureResponse.code}")
            CloseLisaAccountErrorResponse
        }
    }

}
