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

class UpdateSubscriptionService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def updateSubscription(lisaManager: String, accountId: String, request: UpdateSubscriptionRequest)(implicit
    hc: HeaderCarrier
  ): Future[UpdateSubscriptionResponse] =
    desConnector.updateFirstSubDate(lisaManager, accountId, request) map {
      case successResponse: DesUpdateSubscriptionSuccessResponse =>
        logger.info("Update subscription success response")
        if (successResponse.code == "SUCCESS") {
          UpdateSubscriptionSuccessResponse(
            "UPDATED",
            "Successfully updated the firstSubscriptionDate for the LISA account"
          )
        } else {
          val voidMsg = "Successfully updated the firstSubscriptionDate for the LISA account and changed" +
            " the account status to void because the investor has another account with an earlier firstSubscriptionDate"
          UpdateSubscriptionSuccessResponse("UPDATED_AND_ACCOUNT_VOID", voidMsg)
        }
      case DesUnavailableResponse                                =>
        logger.info("Update subscription des unavailable response")
        UpdateSubscriptionServiceUnavailableResponse
      case failureResponse: DesFailureResponse                   =>
        logger.info("Matched DesFailureResponse and the code is " + failureResponse.code)
        desFailures.getOrElse(
          failureResponse.code, {
            logger.warn(s"Update date of first subscription returned error: ${failureResponse.code}")
            UpdateSubscriptionErrorResponse
          }
        )
    }

  private val desFailures = Map[String, UpdateSubscriptionResponse](
    "INVESTOR_ACCOUNTID_NOT_FOUND"       -> UpdateSubscriptionAccountNotFoundResponse,
    "INVESTOR_ACCOUNT_ALREADY_CLOSED"    -> UpdateSubscriptionAccountClosedResponse,
    "INVESTOR_ACCOUNT_ALREADY_CANCELLED" -> UpdateSubscriptionAccountCancelledResponse,
    "INVESTOR_ACCOUNT_ALREADY_VOID"      -> UpdateSubscriptionAccountVoidedResponse
  )
}
