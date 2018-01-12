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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait UpdateSubscriptionService {
  val desConnector: DesConnector


  def updateSubscription(lisaManager: String, accountId: String, request: UpdateSubscriptionRequest)(implicit hc: HeaderCarrier): Future[UpdateSubscriptionResponse] = {
    val response = desConnector.updateFirstSubDate(lisaManager, accountId, request)

    response map {
      case successResponse: DesUpdateSubscriptionSuccessResponse => {
        Logger.debug("Update subscription success response")
        successResponse.code == Constants.successCode match{
          case true => UpdateSubscriptionSuccessResponse(Constants.updateCode,Constants.updateMsg)
          case _ => UpdateSubscriptionSuccessResponse(Constants.voidCode,  Constants.voidMsg)
        }
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case Constants.accNotFound => UpdateSubscriptionAccountNotFoundResponse
          case Constants.accClosed => UpdateSubscriptionAccountClosedResponse
          case Constants.accCancelled => UpdateSubscriptionAccountClosedResponse
          case Constants.accVoid => UpdateSubscriptionAccountVoidedResponse
          case _ => {
            UpdateSubscriptionErrorResponse
          }
        }
      }
    }
  }

}

object UpdateSubscriptionService extends UpdateSubscriptionService {
  override val desConnector: DesConnector = DesConnector
}

object Constants {
  val successCode = "SUCCESS"
  val updateCode = "UPDATED"
  val voidCode = "UPDATED_AND_ACCOUNT_VOIDED"
  val updateMsg = "LISA account firstSubscriptionDate has been successfully updated"
  val voidMsg = "LISA account firstSubscriptionDate has been successfully updated. " +
    "The account status has been changed to 'Void' as the Investor has another account with a more recent firstSubscriptionDate"
  val accNotFound = "INVESTOR_ACCOUNTID_NOT_FOUND"
  val accClosed = "INVESTOR_ACCOUNT_ALREADY_CLOSED"
  val accCancelled = "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
  val accVoid = "INVESTOR_ACCOUNT_ALREADY_VOID"
}

