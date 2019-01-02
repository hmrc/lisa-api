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

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.{ExecutionContext, Future}


class UpdateSubscriptionService @Inject()(desConnector: DesConnector)(implicit ec: ExecutionContext) {

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
      case DesUnavailableResponse => {
        Logger.debug("Update subscription des unavailable response")
        UpdateSubscriptionServiceUnavailableResponse
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case Constants.accNotFound => UpdateSubscriptionAccountNotFoundResponse
          case Constants.accClosed => UpdateSubscriptionAccountClosedResponse
          case Constants.accCancelled => UpdateSubscriptionAccountClosedResponse
          case Constants.accVoid => UpdateSubscriptionAccountVoidedResponse
          case _ => {
            Logger.warn(s"Update date of first subscription returned error: ${failureResponse.code}")
            UpdateSubscriptionErrorResponse
          }
        }
      }
    }
  }

}

object Constants {
  val successCode = "SUCCESS"
  val updateCode = "UPDATED"
  val voidCode = "UPDATED_AND_ACCOUNT_VOID"
  val updateMsg = "Successfully updated the firstSubscriptionDate for the LISA account"
  val voidMsg = "Successfully updated the firstSubscriptionDate for the LISA account and changed the account status " +
                "to void because the investor has another account with an earlier firstSubscriptionDate"
  val accNotFound = "INVESTOR_ACCOUNTID_NOT_FOUND"
  val accClosed = "INVESTOR_ACCOUNT_ALREADY_CLOSED"
  val accCancelled = "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
  val accVoid = "INVESTOR_ACCOUNT_ALREADY_VOID"
}

