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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{UpdateSubscriptionSuccessResponse, _}
import uk.gov.hmrc.lisaapi.services.AuditService
import uk.gov.hmrc.lisaapi.services.UpdateSubscriptionService

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateSubscriptionController extends LisaController with LisaConstants {
  val service: UpdateSubscriptionService = UpdateSubscriptionService
  val auditService: AuditService = AuditService



  def updateSubscription (lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    implicit val startTime = System.currentTimeMillis()
    LisaMetrics.startMetrics(startTime, LisaMetricKeys.UPDATE_SUBSCRIPTION)
    withValidLMRN(lisaManager) {
      withValidAccountId(accountId) {
        withValidJson [UpdateSubscriptionRequest]( updateSubsRequest =>
          service.updateSubscription(lisaManager, accountId, updateSubsRequest) map { result =>
            Logger.debug("Entering Updated subscription Controller and the response is " + result.toString)
            result match {
              case success: UpdateSubscriptionSuccessResponse => {
                val resultRes = AccountFirstSubscriptionUpdated(accountId, success.code)
                Logger.debug("First Subscription date updated")
                doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateUpdated")

                val data = ApiResponseData(message = resultRes.message, code = Some(resultRes.code), accountId = Some(accountId))
                LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = 200)))
              }

              case UpdateSubscriptionAccountNotFoundResponse => {
                Logger.debug("First Subscription date not updated")

                doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateNotUpdated", Map("reasonNotUpdated" -> ErrorAccountNotFound.errorCode))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.getErrorKey(NOT_FOUND, request.uri))

                NotFound(Json.toJson(ErrorAccountNotFound))
              }
              case UpdateSubscriptionAccountClosedResponse => {
                Logger.error(("Account Closed"))
                doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateNotUpdated", Map("reasonNotUpdated" -> ErrorAccountAlreadyClosed.errorCode))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(FORBIDDEN, request.uri))

                Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
              }
              case UpdateSubscriptionAccountVoidedResponse => {
                Logger.error(("Account Voided"))
                doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateNotUpdated", Map("reasonNotUpdated" -> ErrorAccountAlreadyVoided.errorCode))
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.lisaError(FORBIDDEN, request.uri))

                Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
              }
              case _ => {
                Logger.debug("Matched Error")

                doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateNotUpdated", Map("reasonNotUpdated" -> ErrorInternalServerError.errorCode))

                Logger.error(s"First Subscription date not updated : DES unknown case , returning internal server error")
                LisaMetrics.incrementMetrics(startTime,
                  LisaMetricKeys.getErrorKey(INTERNAL_SERVER_ERROR, request.uri))

                InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            }
          }, lisaManager = lisaManager
        )
      }
    }
  }


  private def doAudit(lisaManager: String, accountId: String, updateSubsRequest: UpdateSubscriptionRequest, auditType: String, extraData: Map[String, String] = Map())(implicit hc: HeaderCarrier) = {
    auditService.audit(
    auditType = auditType,
    path = getEndpointUrl(lisaManager, accountId),
    auditData = Map(
    "lisaManagerReferenceNumber" -> lisaManager,
    "accountID" -> accountId,
    "firstSubscriptionDate" -> updateSubsRequest.firstSubscriptionDate.toString("yyyy-MM-dd")
    ) ++ extraData
    )
  }

  private def getEndpointUrl(lisaManagerReferenceNumber: String, accountId: AccountId): String = {
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountId/update-subscription"
  }
}

