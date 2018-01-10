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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, ReinstateAccountService}

import scala.concurrent.ExecutionContext.Implicits.global

class ReinstateAccountController extends LisaController with LisaConstants {

  val service: ReinstateAccountService = ReinstateAccountService
  val auditService: AuditService = AuditService



  def reinstateAccount (lisaManager: String, accountId: String): Action[AnyContent] = Action.async{ implicit request =>
    implicit val startTime = System.currentTimeMillis()
    LisaMetrics.startMetrics(startTime, LisaMetricKeys.REINSTATE)
    withValidLMRN(lisaManager) {
      withValidAccountId(accountId) {
        processReinstateAccount(lisaManager, accountId)
      }
    }
  }



  private def processReinstateAccount(lisaManager: String, accountId: String)(implicit hc: HeaderCarrier, startTime:Long) = {
    service.reinstateAccountService(lisaManager, accountId).map { result =>
      result match {
        case ReinstateLisaAccountSuccessResponse => {
          LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.REINSTATE)

          auditService.audit(
            auditType = "accountReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId)
          )

          val data = ApiResponseData(message = "This account has been reinstated", accountId = Some(accountId))

          Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = 200)))
        }
        case ReinstateLisaAccountAlreadyClosedResponse => {
          auditService.audit(
            auditType = "accountNotReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId,
              "reasonNotReinstated" -> ErrorAccountAlreadyClosed.errorCode)
          )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.REINSTATE))
          val data = ApiResponseData( code = Some(ErrorAccountAlreadyClosed.errorCode), message = "You cannot reinstate this account because it was closed with a closure reason of transferred out")
          Forbidden(Json.toJson(Some(data)))
        }

        case ReinstateLisaAccountAlreadyCancelledResponse => {
          auditService.audit(
            auditType = "accountNotReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId,
              "reasonNotReinstated" -> ErrorAccountAlreadyClosed.errorCode)
          )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.REINSTATE))
          val data = ApiResponseData( code = Some(ErrorAccountAlreadyClosed.errorCode), message = "You cannot reinstate this account because it was closed with a closure reason of cancellation")
          Forbidden(Json.toJson(Some(data)))
        }

        case ReinstateLisaAccountAlreadyOpenResponse => {
          auditService.audit(
            auditType = "accountNotReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId,
              "reasonNotReinstated" -> ErrorAccountAlreadyOpen.errorCode)
          )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.REINSTATE))

          Forbidden(Json.toJson(ErrorAccountAlreadyOpen))
        }

        case ReinstateLisaAccountInvestorComplianceCheckFailedResponse => {
          auditService.audit(
            auditType = "accountNotReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId,
              "reasonNotReinstated" -> ErrorInvestorComplianceCheckFailedReinstate.errorCode)
          )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(FORBIDDEN,LisaMetricKeys.REINSTATE))

          Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailedReinstate))
        }

        case ReinstateLisaAccountNotFoundResponse => {
          auditService.audit(
            auditType = "accountNotReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId,
              "reasonNotReinstated" -> ErrorAccountNotFound.errorCode)
          )
          LisaMetrics.incrementMetrics(startTime,
          LisaMetricKeys.lisaError(NOT_FOUND,LisaMetricKeys.REINSTATE))

          NotFound(Json.toJson(ErrorAccountNotFound))
        }
        case _ => {
          auditService.audit(
            auditType = "accountNotReinstated",
            path = getReinstateEndpointUrl(lisaManager, accountId),
            auditData = Map(ZREF -> lisaManager, "accountId" -> accountId,
              "reasonNotReinstated" -> ErrorInternalServerError.errorCode)
          )
          Logger.error(s"ReinstateAccountController: reinstateAccount unknown case from DES returning internal server error" )
          LisaMetrics.incrementMetrics(startTime,
            LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.CLOSE))

          InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    } recover {
        case e:Exception  =>     Logger.error(s"ReinstateAccountController: reinstateAccount: An error occurred due to ${e.getMessage} returning internal server error")
                              LisaMetrics.incrementMetrics(startTime,
                                LisaMetricKeys.lisaError(INTERNAL_SERVER_ERROR,LisaMetricKeys.CLOSE))
                              InternalServerError(Json.toJson(ErrorInternalServerError))
       }
  }

  private def getReinstateEndpointUrl(lisaManagerReferenceNumber: String, accountID: String): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountID/reinstate-account"
  }
}




