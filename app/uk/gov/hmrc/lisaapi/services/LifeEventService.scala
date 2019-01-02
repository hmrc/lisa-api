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
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountNotFound, ErrorInternalServerError, ErrorLifeEventIdNotFound, ErrorResponse, ErrorServiceUnavailable}


class LifeEventService @Inject()(desConnector: DesConnector)(implicit ec: ExecutionContext) {

  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequestBase)
                     (implicit hc: HeaderCarrier): Future[ReportLifeEventResponse] = {
    val response = desConnector.reportLifeEvent(lisaManager, accountId, request)

    response map {
      case successResponse: DesLifeEventResponse => {
        Logger.debug("Matched DesLifeEventResponse")
        ReportLifeEventSuccessResponse(successResponse.lifeEventID)
      }
      case DesUnavailableResponse => {
        Logger.debug("Matched DesUnavailableResponse")
        ReportLifeEventServiceUnavailableResponse
      }
      case failureResponse: DesFailureResponse => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        postErrors.getOrElse(failureResponse.code, {
          Logger.warn(s"Report life event returned error: ${failureResponse.code}")
          ReportLifeEventErrorResponse
        })
      }
    }
  }

  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: LifeEventId)
                  (implicit hc: HeaderCarrier): Future[Either[ErrorResponse, Seq[GetLifeEventItem]]] = {
    val response = desConnector.getLifeEvent(lisaManager, accountId, lifeEventId)

    response map {
      case Right(successResponse) => {
        Logger.debug("Matched ReportLifeEventRequestBase")
        Right(successResponse)
      }
      case Left(failureResponse) => {
        Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

        val error = getErrors.getOrElse(failureResponse.code, {
          Logger.warn(s"Report life event returned error: ${failureResponse.code}")
          ErrorInternalServerError
        })

        Left(error)
      }
    }
  }

  private val getErrors = Map[String, ErrorResponse](
    "INVESTOR_ACCOUNT_ID_NOT_FOUND" -> ErrorAccountNotFound,
    "LIFE_EVENT_ID_NOT_FOUND" -> ErrorLifeEventIdNotFound,
    "SERVER_ERROR" -> ErrorServiceUnavailable
  )

  private val postErrors = Map[String, ReportLifeEventResponse](
    "LIFE_EVENT_ALREADY_EXISTS" -> ReportLifeEventAlreadyExistsResponse,
    "LIFE_EVENT_INAPPROPRIATE" -> ReportLifeEventInappropriateResponse,
    "INVESTOR_ACCOUNTID_NOT_FOUND" -> ReportLifeEventAccountNotFoundResponse,
    "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" -> ReportLifeEventAccountClosedOrVoidResponse,
    "INVESTOR_ACCOUNT_ALREADY_CLOSED" -> ReportLifeEventAccountClosedResponse,
    "INVESTOR_ACCOUNT_ALREADY_VOID" -> ReportLifeEventAccountVoidResponse,
    "INVESTOR_ACCOUNT_ALREADY_CANCELLED" -> ReportLifeEventAccountCancelledResponse,
    "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" -> ReportLifeEventAlreadySupersededResponse,
    "SUPERSEDING_LIFE_EVENT_MISMATCH" -> ReportLifeEventMismatchResponse,
    "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" -> ReportLifeEventAccountNotOpenLongEnoughResponse,
    "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" -> ReportLifeEventOtherPurchaseOnRecordResponse,
    "FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED" -> ReportLifeEventFundReleaseSupersededResponse,
    "FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND" -> ReportLifeEventFundReleaseNotFoundResponse,
    "PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED" -> ReportLifeEventExtensionOneAlreadyApprovedResponse,
    "PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED" -> ReportLifeEventExtensionTwoAlreadyApprovedResponse,
    "PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED" -> ReportLifeEventExtensionOneNotYetApprovedResponse
  )
}
