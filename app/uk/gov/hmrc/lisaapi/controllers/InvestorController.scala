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
import play.api.libs.json.{JsPath, Json}
import play.api.mvc.{Result, Action, AnyContent}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.InvestorService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class InvestorController extends LisaController {

  val service: InvestorService = InvestorService

  def createLisaInvestor( lisaManager: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async {
      implicit request =>
      Logger.debug(s"LISA HTTP Request: ${request.uri}  and method: ${request.method} and headers :${request.headers} and parameters : ${lisaManager}")
       withValidJson[CreateLisaInvestorRequest] (
               createRequest => {
          service.createInvestor(lisaManager, createRequest).map { result =>
            result match {
              case CreateLisaInvestorSuccessResponse(investorId) => {
                val data = ApiResponseData(message = "Investor Created.", investorId = Some(investorId))

                Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
              }
              case CreateLisaInvestorNotFoundResponse => Forbidden(Json.toJson(ErrorInvestorNotFound))
              case CreateLisaInvestorAlreadyExistsResponse(investorId) => Conflict(Json.toJson(ErrorInvestorAlreadyExists(investorId)))
              case CreateLisaInvestorErrorResponse => InternalServerError(Json.toJson(ErrorInternalServerError))
            }

          }
        },lisaManager=lisaManager)

  }

}
