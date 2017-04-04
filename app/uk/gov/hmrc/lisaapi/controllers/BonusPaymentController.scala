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
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.BonusPaymentService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BonusPaymentController extends LisaController {
  val authConnector = LisaAuthConnector

  val service: BonusPaymentService = BonusPaymentService

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>

    withValidJson[RequestBonusPaymentRequest] { req =>

      if (req.lifeEventID.isEmpty && req.bonuses.claimReason == "Life Event") {
        Future.successful(Forbidden(Json.toJson(ErrorLifeEventNotProvided)))
      }
      else {
        service.requestBonusPayment(lisaManager, accountId, req) map { res =>
          Logger.debug("Entering Bonus Payment Controller and the response is " + res.toString)
          res match {
            case RequestBonusPaymentSuccessResponse(transactionID) => {
              Logger.debug("Matched success response")
              val data = ApiResponseData(message = "Bonus transaction created", transactionId = Some(transactionID))

              Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
            }
            case errorResponse: RequestBonusPaymentErrorResponse => {
              Logger.debug("Matched error response")
              Status(errorResponse.status).apply(Json.toJson(errorResponse.data))
            }
          }
        } recover {
          case _ => InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    }
  }

}
