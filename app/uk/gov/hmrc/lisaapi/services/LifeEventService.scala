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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier


trait LifeEventService {
  val desConnector: DesConnector

  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequestBase)
                     (implicit hc: HeaderCarrier): Future[ReportLifeEventResponse] = {
    val response = desConnector.reportLifeEvent(lisaManager, accountId, request)

    response map {
      case successResponse: DesLifeEventResponse => {
        Logger.debug("Matched DesLifeEventResponse")
        ReportLifeEventSuccessResponse(successResponse.lifeEventID)
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        handleFailureResponse(failureResponse)
      }
    }
  }

  private def handleFailureResponse(failureResponse: DesFailureResponse): ReportLifeEventResponse = {
    failureResponse.code match {
      case "LIFE_EVENT_ALREADY_EXISTS" => ReportLifeEventAlreadyExistsResponse
      case "LIFE_EVENT_INAPPROPRIATE" => ReportLifeEventInappropriateResponse
      case "INVESTOR_ACCOUNTID_NOT_FOUND" => ReportLifeEventAccountNotFoundResponse
      case "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" => ReportLifeEventAccountClosedOrVoidResponse
      case "INVESTOR_ACCOUNT_ALREADY_CLOSED" => ReportLifeEventAccountClosedResponse
      case "INVESTOR_ACCOUNT_ALREADY_VOID" => ReportLifeEventAccountVoidResponse
      case "INVESTOR_ACCOUNT_ALREADY_CANCELLED" => ReportLifeEventAccountCancelledResponse
      case "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" => ReportLifeEventAlreadySupersededResponse
      case "SUPERSEDING_LIFE_EVENT_MISMATCH" => ReportLifeEventMismatchResponse
      case "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" => ReportLifeEventAccountNotOpenLongEnoughResponse
      case "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" => ReportLifeEventOtherPurchaseOnRecordResponse
      case "FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED" => ReportLifeEventFundReleaseSupersededResponse
      case "FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND" => ReportLifeEventFundReleaseNotFoundResponse
      case "PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED" => ReportLifeEventExtensionOneAlreadyApprovedResponse
      case "PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED" => ReportLifeEventExtensionTwoAlreadyApprovedResponse
      case "PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED" => ReportLifeEventExtensionOneNotYetApprovedResponse
      case _ => {
        Logger.warn(s"Report life event returned error: ${failureResponse.code}")
        ReportLifeEventErrorResponse
      }
    }
  }
}

object LifeEventService extends LifeEventService{
  override val desConnector: DesConnector = DesConnector
}
