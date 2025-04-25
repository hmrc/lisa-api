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

package uk.gov.hmrc.lisaapi.controllers

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, PlayBodyParsers, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, InvestorService}

import scala.concurrent.ExecutionContext

class InvestorController @Inject() (
  authConnector: AuthConnector,
  appContext: AppContext,
  service: InvestorService,
  auditService: AuditService,
  lisaMetrics: LisaMetrics,
  cc: ControllerComponents,
  parse: PlayBodyParsers
)(implicit ec: ExecutionContext)
    extends LisaController(
      cc: ControllerComponents,
      lisaMetrics: LisaMetrics,
      appContext: AppContext,
      authConnector: AuthConnector
    ) {

  def createLisaInvestor(lisaManager: String): Action[AnyContent] =
    (validateHeader(parse) andThen validateLMRN(lisaManager)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      logger.info(s"[InvestorController][createLisaInvestor] LISA HTTP Request: ${request.uri} and method: ${request.method} lisaManager :$lisaManager")
      withValidJson[CreateLisaInvestorRequest](
        createRequest =>
          service.createInvestor(lisaManager, createRequest).map {
            case CreateLisaInvestorSuccessResponse(investorId)       =>
              logger.info(s"[InvestorController][CreateLisaInvestorSuccessResponse]  lisaManager :$lisaManager")
              success(lisaManager, createRequest, investorId)
            case CreateLisaInvestorAlreadyExistsResponse(investorId) =>
              logger.warn(s"[InvestorController][CreateLisaInvestorAlreadyExistsResponse]  lisaManager :$lisaManager")
              error(lisaManager, createRequest, ErrorInvestorAlreadyExists(investorId))
            case r: CreateLisaInvestorResponse                       =>
              logger.warn(s"[InvestorController][CreateLisaInvestorResponse]  lisaManager :$lisaManager")
              error(lisaManager, createRequest, errorMap.getOrElse(r, ErrorInternalServerError))
          } recover { case e: Exception =>
            logger.error(
              s"[InvestorController][createLisaInvestor] An error occurred due to ${e.getMessage} returning internal server error"
            )
            error(lisaManager, createRequest, ErrorInternalServerError)
          },
        lisaManager = lisaManager
      )
    }

  private def success(lisaManager: String, createRequest: CreateLisaInvestorRequest, investorId: String)(implicit
    hc: HeaderCarrier,
    startTime: Long
  ): Result = {
    auditService.audit(
      auditType = "investorCreated",
      path = investorEndpointUrl(lisaManager),
      auditData = Map(
        ZREF           -> lisaManager,
        "investorNINO" -> createRequest.investorNINO,
        "dateOfBirth"  -> createRequest.dateOfBirth.toString,
        "investorID"   -> investorId
      )
    )

    val data = ApiResponseData(message = "Investor created", investorId = Some(investorId))

    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.INVESTOR)

    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
  }

  private def error(lisaManager: String, createRequest: CreateLisaInvestorRequest, response: ErrorResponse)(implicit
    hc: HeaderCarrier,
    startTime: Long
  ): Result = {
    val additionalAuditData = response match {
      case res: ErrorResponseWithId => Some("investorID" -> res.id)
      case _                        => None
    }

    auditService.audit(
      auditType = "investorNotCreated",
      path = investorEndpointUrl(lisaManager),
      auditData = Map(
        ZREF               -> lisaManager,
        "investorNINO"     -> createRequest.investorNINO,
        "dateOfBirth"      -> createRequest.dateOfBirth.toString,
        "reasonNotCreated" -> response.errorCode
      ) ++ additionalAuditData
    )

    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.INVESTOR)

    response.asResult
  }

  private val errorMap = Map[CreateLisaInvestorResponse, ErrorResponse](
    CreateLisaInvestorInvestorNotFoundResponse   -> ErrorInvestorNotFound,
    CreateLisaInvestorServiceUnavailableResponse -> ErrorServiceUnavailable,
    CreateLisaInvestorErrorResponse              -> ErrorInternalServerError
  )

  private def investorEndpointUrl(lisaManagerReferenceNumber: String): String =
    s"/manager/$lisaManagerReferenceNumber/investors"

}
