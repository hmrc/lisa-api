/*
 * Copyright 2017 HM Revenue & Customs
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
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesLifeEventResponse}
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait LifeEventService {
  val desConnector: DesConnector

  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequest)(implicit hc: HeaderCarrier): Future[ReportLifeEventResponse] = {
    val response = desConnector.reportLifeEvent(lisaManager, accountId, request)

    response map { result =>
      result match {
        case successResponse: DesLifeEventResponse => {
          Logger.debug("Matched DesLifeEventResponse")

          ReportLifeEventSuccessResponse(successResponse.lifeEventID)
        }
        case failureResponse: DesFailureResponse => {
          Logger.debug("Matched DesFailureResponse and the code is " + failureResponse.code)

          failureResponse.code match {
            case "LIFE_EVENT_INAPPROPRIATE" => ReportLifeEventInappropriateResponse
            case "LIFE_EVENT_ALREADY_EXISTS" => ReportLifeEventAlreadyExistsResponse
            case "INVESTOR_ACCOUNTID_NOT_FOUND" => ReportLifeEventAccountNotFoundResponse
            case "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" => ReportLifeEventAccountClosedResponse
            case "INTERNAL_SERVER_ERROR" => ReportLifeEventErrorResponse
            case _ => {
              ReportLifeEventErrorResponse
            }
          }
        }
      }
    }
  }

}

object LifeEventService extends LifeEventService{
  override val desConnector: DesConnector = DesConnector
}
