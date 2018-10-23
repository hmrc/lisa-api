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
                        (implicit hc: HeaderCarrier): Future[PropertyPurchaseResponse] = {

    Logger.debug("Request fund release")

    val response = desConnector.requestFundRelease(lisaManager, accountId, request)

    response map {
      case successResponse: DesLifeEventResponse => {
        Logger.debug("Request fund release - matched DesLifeEventResponse")

        PropertyPurchaseSuccessResponse(successResponse.lifeEventID)
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Request fund release - matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => PropertyPurchaseAccountClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => PropertyPurchaseAccountCancelledResponse
          case "INVESTOR_ACCOUNT_ALREADY_VOID" => PropertyPurchaseAccountVoidResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => PropertyPurchaseAccountNotFoundResponse
          case "LIFE_EVENT_ALREADY_EXISTS" => PropertyPurchaseLifeEventAlreadyExistsResponse
          case "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" => PropertyPurchaseLifeEventAlreadySupersededResponse
          case "SUPERSEDING_LIFE_EVENT_MISMATCH" => PropertyPurchaseMismatchResponse
          case "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" => PropertyPurchaseAccountNotOpenLongEnoughResponse
          case "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" => PropertyPurchaseOtherPurchaseOnRecordResponse
          case _ => {
            Logger.warn(s"Request fund release returned error: ${failureResponse.code}")

            PropertyPurchaseErrorResponse
          }
        }
      }
    }
  }

  // scalastyle:off cyclomatic.complexity
  def requestPurchaseExtension(lisaManager: String, accountId: String, request: RequestPurchaseExtension)
                              (implicit hc: HeaderCarrier): Future[PropertyPurchaseResponse] = {

    Logger.debug("Request purchase extension")

    val response = desConnector.requestPurchaseExtension(lisaManager, accountId, request)

    response map {
      case successResponse: DesLifeEventResponse => {
        Logger.debug("Request purchase extension - matched DesLifeEventResponse")

        PropertyPurchaseSuccessResponse(successResponse.lifeEventID)
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Request purchase extension - matched DesFailureResponse and the code is " + failureResponse.code)

        failureResponse.code match {
          case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => PropertyPurchaseAccountClosedResponse
          case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => PropertyPurchaseAccountCancelledResponse
          case "INVESTOR_ACCOUNT_ALREADY_VOID" => PropertyPurchaseAccountVoidResponse
          case "PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED" => PropertyPurchaseExtensionOneNotYetApprovedResponse
          case "SUPERSEDING_LIFE_EVENT_MISMATCH" => PropertyPurchaseMismatchResponse
          case "INVESTOR_ACCOUNTID_NOT_FOUND" => PropertyPurchaseAccountNotFoundResponse
          case "FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND" => PropertyPurchaseFundReleaseNotFoundResponse
          case "LIFE_EVENT_ALREADY_EXISTS" => PropertyPurchaseLifeEventAlreadyExistsResponse
          case "FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED" => PropertyPurchaseFundReleaseSupersededResponse
          case "PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED" => PropertyPurchaseExtensionOneAlreadyApprovedResponse
          case "PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED" => PropertyPurchaseExtensionTwoAlreadyApprovedResponse
          case "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" => PropertyPurchaseLifeEventAlreadySupersededResponse
          case _ => {
            Logger.warn(s"Request purchase extension returned error: ${failureResponse.code}")

            PropertyPurchaseErrorResponse
          }
        }
      }
    }
  }

}

object PropertyPurchaseService extends PropertyPurchaseService {
  override val desConnector: DesConnector = DesConnector
}
