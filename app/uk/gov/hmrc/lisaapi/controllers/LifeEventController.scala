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

package uk.gov.hmrc.lisaapi.controllers

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.LifeEventService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LifeEventController extends LisaController {

  val service: LifeEventService = LifeEventService

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  def reportLisaLifeEvent(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>

    withValidJson[ReportLifeEventRequest] { req =>
      if(validateDatebyEvent(req)) {
        service.reportLifeEvent(lisaManager, accountId, req) map { res =>
          Logger.debug("Entering LifeEvent Controller and the response is " + res.toString)
          res match {
            case ReportLifeEventSuccessResponse(lifeEventId) => {
              Logger.debug("Matched Valid repsponse ")
              val data = ApiResponseData(message = "Life Event Created", lifeEventId = Some(lifeEventId))

              Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
            }
            case ReportLifeEventInappropriateResponse => {
              Logger.debug(("Matched Inappropriate"))
              Forbidden(Json.toJson(ErrorLifeEventInappropriate))
            }
            case ReportLifeEventAlreadyExistsResponse => {
              Logger.debug("Matched Already Exists")
              Conflict(Json.toJson(ErrorLifeEventAlreadyExists))
            }
            case ReportLifeEventAccountNotFoundResponse => {
              NotFound(Json.toJson(ErrorAccountNotFound))
            }
            case _ => {
              Logger.debug("Matched Error")
              InternalServerError(Json.toJson(ErrorInternalServerError))
            }
          }
        }
      } else {
        Logger.debug("Bad date")
        Future.successful(BadRequest(Json.toJson(ErrorLifeEventInvalidFutureDate)))
      }
    }
  }

  def validateDatebyEvent(lifeEvent: ReportLifeEventRequest): Boolean = {
    Logger.debug("Validating the date")
    val dt = lifeEvent.eventDate
    Logger.debug("The date is " + lifeEvent.eventDate.toString)
    Logger.debug("The event type is " + lifeEvent.eventType)
    lifeEvent.eventType match {
      case "LISA Investor Terminal Ill Health" => if( dt.isAfterNow ) false else true
      case "LISA Investor Death"  => if( dt.isAfterNow ) false else true
      case _ => true
    }
  }

}
