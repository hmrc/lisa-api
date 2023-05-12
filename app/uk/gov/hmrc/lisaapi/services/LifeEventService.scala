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
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountNotFound, ErrorInternalServerError, ErrorLifeEventIdNotFound, ErrorResponse, ErrorServiceUnavailable}

import scala.util.matching.Regex

class LifeEventService @Inject() (desConnector: DesConnector)(implicit ec: ExecutionContext) extends Logging {

  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequestBase)(implicit
    hc: HeaderCarrier
  ): Future[ReportLifeEventResponse] =
    desConnector.reportLifeEvent(lisaManager, accountId, request) map {
      case successResponse: DesLifeEventResponse =>
        logger.debug("Matched DesLifeEventResponse")
        ReportLifeEventSuccessResponse(successResponse.lifeEventID)
      case DesUnavailableResponse                =>
        logger.debug("Matched DesUnavailableResponse")
        ReportLifeEventServiceUnavailableResponse
      case failureResponse: DesFailureResponse   =>
        logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        postErrors.applyOrElse(
          (failureResponse.code, failureResponse),
          { _: (String, DesFailureResponse) =>
            logger.warn(s"Report life event returned error: ${failureResponse.code}")
            ReportLifeEventErrorResponse
          }
        )
    }

  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: LifeEventId)(implicit
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, Seq[GetLifeEventItem]]] =
    desConnector.getLifeEvent(lisaManager, accountId, lifeEventId) map {
      case Right(successResponse) =>
        logger.debug("Matched ReportLifeEventRequestBase")
        Right(successResponse)
      case Left(failureResponse)  =>
        logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)
        val error = getErrors.getOrElse(
          failureResponse.code, {
            logger.warn(s"Report life event returned error: ${failureResponse.code}")
            ErrorInternalServerError
          }
        )
        Left(error)
    }

  private val getErrors = Map[String, ErrorResponse](
    "INVESTOR_ACCOUNT_ID_NOT_FOUND" -> ErrorAccountNotFound,
    "LIFE_EVENT_ID_NOT_FOUND"       -> ErrorLifeEventIdNotFound,
    "SERVER_ERROR"                  -> ErrorServiceUnavailable
  )

  private val postErrors: PartialFunction[(String, DesFailureResponse), ReportLifeEventResponse] = {
    case ("LIFE_EVENT_ALREADY_EXISTS", res)                        =>
      val lifeEventId = extractLifeEventIdFromReason(
        res.reason,
        "^The investor’s life event id (\\d{10}) has already been reported\\.$".r
      )
      ReportLifeEventAlreadyExistsResponse(lifeEventId)
    case ("SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED", res)         =>
      val lifeEventId =
        extractLifeEventIdFromReason(res.reason, "^The life event id (\\d{10}) has already been superseded\\.$".r)
      ReportLifeEventAlreadySupersededResponse(lifeEventId)
    case ("PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED", res) =>
      val lifeEventId = extractLifeEventIdFromReason(
        res.reason,
        "^Extension 1 life event (\\d{10}) has already been recorded for this account\\.$".r
      )
      ReportLifeEventExtensionOneAlreadyApprovedResponse(lifeEventId)
    case ("PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED", res) =>
      val lifeEventId = extractLifeEventIdFromReason(
        res.reason,
        "^Extension 2 life event (\\d{10}) has already been recorded for this account\\.$".r
      )
      ReportLifeEventExtensionTwoAlreadyApprovedResponse(lifeEventId)
    case ("FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED", res)            =>
      val lifeEventId = extractLifeEventIdFromReason(
        res.reason,
        "^The fund release life event id (\\d{10}) in the request has been superseded\\.$".r
      )
      ReportLifeEventFundReleaseSupersededResponse(lifeEventId)
    case ("LIFE_EVENT_INAPPROPRIATE", _)                           => ReportLifeEventInappropriateResponse
    case ("INVESTOR_ACCOUNTID_NOT_FOUND", _)                       => ReportLifeEventAccountNotFoundResponse
    case ("INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID", _)            => ReportLifeEventAccountClosedOrVoidResponse
    case ("INVESTOR_ACCOUNT_ALREADY_CLOSED", _)                    => ReportLifeEventAccountClosedResponse
    case ("INVESTOR_ACCOUNT_ALREADY_VOID", _)                      => ReportLifeEventAccountVoidResponse
    case ("INVESTOR_ACCOUNT_ALREADY_CANCELLED", _)                 => ReportLifeEventAccountCancelledResponse
    case ("SUPERSEDING_LIFE_EVENT_MISMATCH", _)                    => ReportLifeEventMismatchResponse
    case ("COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH", _)      => ReportLifeEventAccountNotOpenLongEnoughResponse
    case ("COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD", _)          => ReportLifeEventOtherPurchaseOnRecordResponse
    case ("FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND", _)               => ReportLifeEventFundReleaseNotFoundResponse
    case ("PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED", _)   => ReportLifeEventExtensionOneNotYetApprovedResponse
    case ("INVALID_PAYLOAD", _)                                    => ReportLifeEventInvalidPayload
  }

  private def extractLifeEventIdFromReason(reason: String, regex: Regex) =
    regex.findFirstMatchIn(reason).get.group(1)

}
