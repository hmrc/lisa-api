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
import uk.gov.hmrc.lisaapi.services.{AuditService, InvestorService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class InvestorController extends LisaController {

  val service: InvestorService = InvestorService
  val auditService: AuditService = AuditService

  def createLisaInvestor(lisaManager: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      Logger.debug(s"LISA HTTP Request: ${request.uri} and method: ${request.method}")

      withValidJson[CreateLisaInvestorRequest] {
        createRequest => {
          service.createInvestor(lisaManager, createRequest).map { res =>
            res match {
              case CreateLisaInvestorSuccessResponse(investorId) =>
                handleCreatedResponse(lisaManager, createRequest, investorId)
              case errorResponse: CreateLisaInvestorErrorResponse =>
                handleFailureResponse(lisaManager, createRequest, errorResponse)
            }
          }
        }
      }
  }

  private def handleCreatedResponse(lisaManager: String, createRequest: CreateLisaInvestorRequest, investorId: String)(implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "investorCreated",
      path = getEndpointUrl(lisaManager),
      auditData = Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "investorID" -> investorId
      )
    )

    val data = ApiResponseData(message = "Investor Created.", investorId = Some(investorId))

    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
  }

  private def handleFailureResponse(lisaManager: String, createRequest: CreateLisaInvestorRequest, errorResponse: CreateLisaInvestorErrorResponse)(implicit hc: HeaderCarrier) = {

    auditService.audit(
      auditType = "investorNotCreated",
      path = getEndpointUrl(lisaManager),
      auditData = Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "reasonNotCreated" -> errorResponse.data.code
      )
    )

    Status(errorResponse.status).apply(Json.toJson(errorResponse.data))
  }

  private def getEndpointUrl(lisaManagerReferenceNumber: String):String = {
    s"/manager/$lisaManagerReferenceNumber/investors"
  }

}
