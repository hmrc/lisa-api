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
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesLifeEventExistResponse, DesLifeEventResponse}
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait PropertyPurchaseService {
  val desConnector: DesConnector

  // scalastyle:off cyclomatic.complexity
  def requestFundRelease(lisaManager: String, accountId: String, request: RequestFundReleaseRequest)
                        (implicit hc: HeaderCarrier): Future[RequestFundReleaseResponse] = {

    val response = desConnector.requestFundRelease(lisaManager, accountId, request)

    response map {
      case successResponse: DesLifeEventResponse => {
        Logger.debug("Matched DesLifeEventResponse")

        RequestFundReleaseSuccessResponse(successResponse.lifeEventID)
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => RequestFundReleaseAccountClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => RequestFundReleaseAccountCancelledResponse
          case "INVESTOR_ACCOUNT_ALREADY_VOID" => RequestFundReleaseAccountVoidResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => RequestFundReleaseAccountNotFoundResponse
          case "LIFE_EVENT_ALREADY_EXISTS" => RequestFundReleaseLifeEventAlreadyExistsResponse
          case "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" => RequestFundReleaseLifeEventAlreadySupersededResponse
          case "SUPERSEDING_LIFE_EVENT_MISMATCH" => RequestFundReleaseMismatchResponse
          case "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" => RequestFundReleaseAccountNotOpenLongEnoughResponse
          case "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" => RequestFundReleaseOtherPurchaseOnRecordResponse
          case _ => {
            Logger.warn(s"Report life event returned error: ${failureResponse.code}")

            RequestFundReleaseErrorResponse
          }
        }
      }
    }
  }

}

object PropertyPurchaseService extends PropertyPurchaseService {
  override val desConnector: DesConnector = DesConnector
}
