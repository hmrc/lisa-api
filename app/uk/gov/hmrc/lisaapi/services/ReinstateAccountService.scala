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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.{ExecutionContext, Future}

class ReinstateAccountService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def reinstateAccountService(lisaManager: String, accountId: AccountId)(implicit
    hc: HeaderCarrier
  ): Future[ReinstateLisaAccountResponse] =
    desConnector.reinstateAccount(lisaManager, accountId) map {
      case successResponse: DesReinstateAccountSuccessResponse =>
        logger.debug("Reinstate account success response")
        ReinstateLisaAccountSuccessResponse(successResponse.code, successResponse.reason)
      case DesUnavailableResponse                              =>
        logger.debug("Reinstate account returned service unavailable")
        ReinstateLisaAccountServiceUnavailableResponse
      case failureResponse: DesFailureResponse                 =>
        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED"    => ReinstateLisaAccountAlreadyClosedResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND"       => ReinstateLisaAccountNotFoundResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => ReinstateLisaAccountAlreadyCancelledResponse
          case "INVESTOR_ACCOUNT_ALREADY_OPEN"      => ReinstateLisaAccountAlreadyOpenResponse
          case "INVESTOR_COMPLIANCE_CHECK_FAILED"   => ReinstateLisaAccountInvestorComplianceCheckFailedResponse
          case _                                    =>
            logger.warn(s"Reinstate account returned error ${failureResponse.code}")
            ReinstateLisaAccountErrorResponse
        }
    }
}
