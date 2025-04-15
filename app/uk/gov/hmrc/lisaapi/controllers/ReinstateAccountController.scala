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
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, ReinstateAccountService}

import scala.concurrent.ExecutionContext

class ReinstateAccountController @Inject()(
                                            authConnector: AuthConnector,
                                            appContext: AppContext,
                                            service: ReinstateAccountService,
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

  def reinstateAccount(lisaManager: String): Action[AnyContent] = Action.async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()
    logger.info(s"[ReinstateAccountController][reinstateAccount] started lisaManager : $lisaManager")
    withValidLMRN(lisaManager) { () =>
      withValidJson[ReinstateLisaAccountRequest](
        req => processReinstateAccount(lisaManager, req.accountId),
        lisaManager = lisaManager
      )
    }
  }

  private def processReinstateAccount(lisaManager: String, accountId: String)(implicit
                                                                              hc: HeaderCarrier,
                                                                              startTime: Long
  ) =
    service.reinstateAccountService(lisaManager, accountId).map {
      case _: ReinstateLisaAccountSuccessResponse =>
        auditService.audit(
          auditType = "accountReinstated",
          path = getReinstateEndpointUrl(lisaManager, accountId),
          auditData = Map(ZREF -> lisaManager, "accountId" -> accountId)
        )
        lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.REINSTATE)
        val data = ApiResponseData(message = "This account has been reinstated", accountId = Some(accountId))
        logger.info(s"[ReinstateAccountController][processReinstateAccount] success response for lisaManager : $lisaManager , accountId : $accountId")
        Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = OK)))
      case ReinstateLisaAccountAlreadyClosedResponse =>
        val message =
          Some("You cannot reinstate this account because it was closed with a closure reason of transferred out")
        processReinstateFailure(lisaManager, accountId, ErrorAccountAlreadyClosed, message)
      case ReinstateLisaAccountAlreadyCancelledResponse =>
        val message =
          Some("You cannot reinstate this account because it was closed with a closure reason of cancellation")
        processReinstateFailure(lisaManager, accountId, ErrorAccountAlreadyCancelled, message)
      case ReinstateLisaAccountAlreadyOpenResponse =>
        processReinstateFailure(lisaManager, accountId, ErrorAccountAlreadyOpen)
      case ReinstateLisaAccountInvestorComplianceCheckFailedResponse =>
        processReinstateFailure(lisaManager, accountId, ErrorInvestorComplianceCheckFailedReinstate)
      case ReinstateLisaAccountNotFoundResponse =>
        processReinstateFailure(lisaManager, accountId, ErrorAccountNotFound)
      case ReinstateLisaAccountServiceUnavailableResponse =>
        processReinstateFailure(lisaManager, accountId, ErrorServiceUnavailable)
      case ReinstateLisaAccountErrorResponse =>
        processReinstateFailure(lisaManager, accountId, ErrorInternalServerError)
    } recover { case _: Exception =>
      logger.error(s"ReinstateAccountController: reinstateAccount: An error occurred returning internal server error")
      lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.REINSTATE)
      ErrorInternalServerError.asResult
    }

  private def getReinstateEndpointUrl(lisaManagerReferenceNumber: String, accountID: String): String =
    s"/manager/$lisaManagerReferenceNumber/reinstate-account"

  private def processReinstateFailure(
                                       lisaManager: String,
                                       accountId: String,
                                       err: ErrorResponse,
                                       message: Option[String] = None
                                     )(implicit hc: HeaderCarrier, startTime: Long): Result = {
    auditService.audit(
      auditType = "accountNotReinstated",
      path = getReinstateEndpointUrl(lisaManager, accountId),
      auditData = Map(
        ZREF -> lisaManager,
        "accountId" -> accountId,
        "reasonNotReinstated" -> err.errorCode
      )
    )

    logger.info(s"[ReinstateAccountController][processReinstateFailure] failed for lisaManager : $lisaManager , " +
      s"accountId : $accountId, error : ${message.getOrElse(err.message)}")

    lisaMetrics.incrementMetrics(startTime, err.httpStatusCode, LisaMetricKeys.REINSTATE)
    val data = ApiResponseData(code = Some(err.errorCode), message = message.getOrElse(err.message))
    Status(err.httpStatusCode).apply(Json.toJson(Some(data)))
  }

}
