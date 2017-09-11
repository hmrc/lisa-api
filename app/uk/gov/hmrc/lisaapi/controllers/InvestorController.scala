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

import java.util.concurrent.TimeUnit

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{MetricsEnum, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, InvestorService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class InvestorController extends LisaController with LisaConstants  {

  val service: InvestorService = InvestorService
  val auditService: AuditService = AuditService
  def createLisaInvestor(lisaManager: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime,MetricsEnum.LISA_INVESTOR)
      Logger.debug(s"LISA HTTP Request: ${request.uri} and method: ${request.method}")

      withValidLMRN(lisaManager) {
        withValidJson[CreateLisaInvestorRequest](
          createRequest => {
            service.createInvestor(lisaManager, createRequest).map { res =>
              res match {
                case CreateLisaInvestorSuccessResponse(investorId) =>
                  LisaMetrics.incrementMetrics(startTime, MetricsEnum.LISA_INVESTOR)
                  handleCreatedResponse(lisaManager, createRequest, investorId)
                case CreateLisaInvestorAlreadyExistsResponse(investorId) =>
                  LisaMetrics.incrementMetrics(startTime, MetricsEnum.lisaError(CONFLICT,MetricsEnum.LISA_INVESTOR))
                  handleExistsResponse(lisaManager, createRequest, investorId)
                case errorResponse: CreateLisaInvestorErrorResponse =>
                  handleFailureResponse(lisaManager, createRequest, errorResponse)
              }
            } recover {
              case e: Exception =>
                LisaMetrics.incrementMetrics(startTime,
                  MetricsEnum.lisaError(INTERNAL_SERVER_ERROR,MetricsEnum.LISA_INVESTOR))
                Logger.error(s"createLisaInvestor: An error occurred due to ${e.getMessage} returning internal server error")
                handleError(lisaManager, createRequest)
            }
          }, lisaManager = lisaManager
        )
      }
  }

  private def handleCreatedResponse(lisaManager: String, createRequest: CreateLisaInvestorRequest, investorId: String)(implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "investorCreated",
      path = getEndpointUrl(lisaManager),
      auditData = Map(
        ZREF -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "investorID" -> investorId
      )
    )

    val data = ApiResponseData(message = "Investor Created.", investorId = Some(investorId))

    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
  }

  private def handleExistsResponse(lisaManager: String, createRequest: CreateLisaInvestorRequest, investorId: String)(implicit hc: HeaderCarrier) = {
    val result = ErrorInvestorAlreadyExists(investorId)

    auditService.audit(
      auditType = "investorNotCreated",
      path = getEndpointUrl(lisaManager),
      auditData = Map(
        ZREF -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "investorID" -> investorId,
        "reasonNotCreated" -> result.errorCode
      )
    )
    LisaMetrics.incrementMetrics(System.currentTimeMillis(),
      MetricsEnum.lisaError(CONFLICT,MetricsEnum.LISA_INVESTOR))

    Conflict(Json.toJson(result))
  }

  private def handleFailureResponse(lisaManager: String, createRequest: CreateLisaInvestorRequest, errorResponse: CreateLisaInvestorErrorResponse)(implicit hc: HeaderCarrier) = {

    auditService.audit(
      auditType = "investorNotCreated",
      path = getEndpointUrl(lisaManager),
      auditData = Map(
        ZREF -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "reasonNotCreated" -> errorResponse.data.code
      )
    )
    LisaMetrics.incrementMetrics(System.currentTimeMillis(),
      MetricsEnum.lisaError(errorResponse.status,MetricsEnum.LISA_INVESTOR))

    Status(errorResponse.status).apply(Json.toJson(errorResponse.data))
  }

  private def handleError(lisaManager: String, createRequest: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "investorNotCreated",
      path = getEndpointUrl(lisaManager),
      auditData = Map(
        ZREF -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "reasonNotCreated" -> ErrorInternalServerError.errorCode
      )
    )
    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def getEndpointUrl(lisaManagerReferenceNumber: String):String = {
    s"/manager/$lisaManagerReferenceNumber/investors"
  }

}
