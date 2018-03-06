/*
 * Copyright 2018 HM Revenue & Customs
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
import play.api.libs.json.{JsObject, JsPath, Json}
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, ReinstateAccountService}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ReinstateAccountController extends LisaController with LisaConstants {

  val service: ReinstateAccountService = ReinstateAccountService
  val auditService: AuditService = AuditService

  def reinstateAccount (lisaManager: String): Action[AnyContent] = Action.async{ implicit request =>
    implicit val startTime = System.currentTimeMillis()
    withValidLMRN(lisaManager) { () =>
      withValidJson[ReinstateLisaAccountRequest] (
        req => processReinstateAccount(lisaManager, req.accountId.toString),
        lisaManager = lisaManager
      )
    }

  }

  private def processReinstateFailure(lisaManager: String, accountId: String, err: ErrorResponse, status: Int, message: Option[String])
                                     (implicit hc: HeaderCarrier, startTime: Long): Result = {
    auditService.audit(
      auditType = "accountNotReinstated",
      path = getReinstateEndpointUrl(lisaManager, accountId),
      auditData = Map(
        ZREF -> lisaManager,
        "accountId" -> accountId,
        "reasonNotReinstated" -> err.errorCode
      )
    )

    LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(status, LisaMetricKeys.REINSTATE))

    val msg = message match {
      case Some(text) => text
      case None => err.message
    }

    val data = ApiResponseData(code = Some(err.errorCode), message = msg)

    Status(status).apply(Json.toJson(Some(data)))
  }

  private def processReinstateAccount(lisaManager: String, accountId: String)
                                     (implicit hc: HeaderCarrier, startTime: Long) = {

    service.reinstateAccountService(lisaManager, accountId).map { result =>
      result match {
        case _: ReinstateLisaAccountSuccessResponse => {
          auditService.audit(
            auditType = "accountReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId)
          )
          LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(OK, LisaMetricKeys.REINSTATE))
          val data = ApiResponseData(message = "This account has been reinstated", accountId = Some(accountId))
          Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = OK)))
        }
        case ReinstateLisaAccountAlreadyClosedResponse =>
          processReinstateFailure(
            lisaManager,
            accountId,
            ErrorAccountAlreadyClosed,
            FORBIDDEN,
            Some("You cannot reinstate this account because it was closed with a closure reason of transferred out")
          )
        case ReinstateLisaAccountAlreadyCancelledResponse =>
          processReinstateFailure(
            lisaManager,
            accountId,
            ErrorAccountAlreadyClosed,
            FORBIDDEN,
            Some("You cannot reinstate this account because it was closed with a closure reason of cancellation")
          )
        case ReinstateLisaAccountAlreadyOpenResponse =>
          processReinstateFailure(lisaManager, accountId, ErrorAccountAlreadyOpen, FORBIDDEN, None)
        case ReinstateLisaAccountInvestorComplianceCheckFailedResponse =>
          processReinstateFailure(lisaManager, accountId, ErrorInvestorComplianceCheckFailedReinstate, FORBIDDEN, None)
        case ReinstateLisaAccountNotFoundResponse =>
          processReinstateFailure(lisaManager, accountId, ErrorAccountNotFound, NOT_FOUND, None)
        case _ =>
          processReinstateFailure(lisaManager, accountId, ErrorInternalServerError, INTERNAL_SERVER_ERROR, None)
      }
    } recover {
      case _:Exception  => {
        Logger.error(s"ReinstateAccountController: reinstateAccount: An error occurred returning internal server error")
        LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.lisaMetric(INTERNAL_SERVER_ERROR, LisaMetricKeys.REINSTATE))
        InternalServerError(Json.toJson(ErrorInternalServerError))
      }
    }
  }

  private def getReinstateEndpointUrl(lisaManagerReferenceNumber: String, accountID: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountID/reinstate"
  }
}




