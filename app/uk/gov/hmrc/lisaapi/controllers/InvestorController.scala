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

package uk.gov.hmrc.lisaapi.controllers

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, InvestorService}

import scala.concurrent.ExecutionContext

class InvestorController @Inject()(
                                    val authConnector: AuthConnector,
                                    val appContext: AppContext,
                                    service: InvestorService,
                                    auditService: AuditService,
                                    val lisaMetrics: LisaMetrics
                                  )(implicit ec: ExecutionContext) extends LisaController {

  def createLisaInvestor(lisaManager: String): Action[AnyContent] = (validateHeader() andThen validateLMRN(lisaManager)).async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()
    Logger.debug(s"LISA HTTP Request: ${request.uri} and method: ${request.method}")
    withValidJson[CreateLisaInvestorRequest](
      createRequest => {
        service.createInvestor(lisaManager, createRequest).map {
          case CreateLisaInvestorSuccessResponse(investorId) =>
            success(lisaManager, createRequest, investorId)
          case CreateLisaInvestorAlreadyExistsResponse(investorId) =>
            error(lisaManager, createRequest, ErrorInvestorAlreadyExists(investorId))
          case r: CreateLisaInvestorResponse =>
            error(lisaManager, createRequest, errorMap.getOrElse(r, ErrorInternalServerError))
        } recover {
          case e: Exception =>
            Logger.error(s"createLisaInvestor: An error occurred due to ${e.getMessage} returning internal server error")
            error(lisaManager, createRequest, ErrorInternalServerError)
        }
      },
      lisaManager = lisaManager
    )
  }

  private def success(lisaManager: String, createRequest: CreateLisaInvestorRequest, investorId: String)
                     (implicit hc: HeaderCarrier, startTime: Long) = {
    auditService.audit(
      auditType = "investorCreated",
      path = investorEndpointUrl(lisaManager),
      auditData = Map(
        ZREF -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "investorID" -> investorId
      )
    )

    val data = ApiResponseData(message = "Investor created", investorId = Some(investorId))

    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.INVESTOR)
    
    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
  }

  private def error(lisaManager: String, createRequest: CreateLisaInvestorRequest, response: ErrorResponse)
                   (implicit hc: HeaderCarrier, startTime: Long) = {
    val additionalAuditData = response match {
      case res: ErrorResponseWithId => Some("investorID" -> res.id)
      case _ => None
    }

    auditService.audit(
      auditType = "investorNotCreated",
      path = investorEndpointUrl(lisaManager),
      auditData = Map(
        ZREF -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth" -> createRequest.dateOfBirth.toString("yyyy-MM-dd"),
        "reasonNotCreated" -> response.errorCode
      ) ++ additionalAuditData
    )

    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.INVESTOR)

    response.asResult
  }

  private val errorMap = Map[CreateLisaInvestorResponse, ErrorResponse](
    CreateLisaInvestorInvestorNotFoundResponse -> ErrorInvestorNotFound,
    CreateLisaInvestorServiceUnavailableResponse -> ErrorServiceUnavailable,
    CreateLisaInvestorErrorResponse -> ErrorInternalServerError
  )

  private def investorEndpointUrl(lisaManagerReferenceNumber: String):String =
    s"/manager/$lisaManagerReferenceNumber/investors"

}
