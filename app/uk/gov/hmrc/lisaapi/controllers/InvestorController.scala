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
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, InvestorService}
import uk.gov.hmrc.play.audit.model.DataEvent
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global

class InvestorController extends LisaController {

  val service: InvestorService = InvestorService
  val auditService: AuditService = AuditService

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  def createLisaInvestor(lisaManager: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      Logger.debug(s"LISA HTTP Request: ${request.uri}  and method: ${request.method}" )
      withValidJson[CreateLisaInvestorRequest] {
        createRequest => {
          service.createInvestor(lisaManager, createRequest).map { result =>
            result match {
              case CreateLisaInvestorSuccessResponse(investorId) => {
                val auditEvent = auditService.createEvent(
                  auditType = "investorCreated",
                  path = routes.InvestorController.createLisaInvestor(lisaManager).url,
                  auditData = Map(
                    "lisaManagerReferenceNumber" -> lisaManager,
                    "investorNINO" -> createRequest.investorNINO,
                    "investorID" -> investorId
                  )
                )
                auditService.sendEvent(auditEvent)

                val data = ApiResponseData(message = "Investor Created.", investorId = Some(investorId))

                Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
              }
              case CreateLisaInvestorNotFoundResponse => {
                auditService.sendEvent(DataEvent("test", "test"))
                Forbidden(Json.toJson(ErrorInvestorNotFound))
              }
              case CreateLisaInvestorAlreadyExistsResponse(investorId) => {
                auditService.sendEvent(DataEvent("test", "test"))
                Conflict(Json.toJson(ErrorInvestorAlreadyExists(investorId)))
              }
              case CreateLisaInvestorErrorResponse => {
                auditService.sendEvent(DataEvent("test", "test"))
                InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            }
          }
        }
      }
  }

}
