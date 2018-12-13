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

import com.google.inject.Inject
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{UpdateSubscriptionSuccessResponse, _}
import uk.gov.hmrc.lisaapi.services.{AuditService, UpdateSubscriptionService}

import scala.concurrent.{ExecutionContext, Future}

class UpdateSubscriptionController @Inject() (
                                               val authConnector: AuthConnector,
                                               val appContext: AppContext,
                                               service: UpdateSubscriptionService,
                                               auditService: AuditService
                                             )(implicit ec: ExecutionContext) extends LisaController2 {

  val failureEvent: String = "firstSubscriptionDateNotUpdated"
  val failureReason: String = "reasonNotUpdated"

  def updateSubscription (lisaManager: String, accountId: String): Action[AnyContent] = validateHeader().async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()
    withValidLMRN(lisaManager) { () =>
      withValidAccountId(accountId) { () =>
        withValidJson[UpdateSubscriptionRequest]( updateSubsRequest => {
          withValidDates(lisaManager, accountId, updateSubsRequest) { () =>
            service.updateSubscription(lisaManager, accountId, updateSubsRequest) map { result =>
              Logger.debug("Entering Updated subscription Controller and the response is " + result.toString)
              result match {
                case success: UpdateSubscriptionSuccessResponse =>
                  Logger.debug("First Subscription date updated")
                  doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateUpdated")
                  val data = ApiResponseData(message = success.message, code = Some(success.code), accountId = Some(accountId))
                  LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                  Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = OK)))
                case UpdateSubscriptionAccountNotFoundResponse =>
                  Logger.debug("First Subscription date not updated")
                  doAudit(lisaManager, accountId, updateSubsRequest, failureEvent,
                    Map(failureReason -> ErrorAccountNotFound.errorCode))
                  LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                  NotFound(Json.toJson(ErrorAccountNotFound))
                case UpdateSubscriptionAccountClosedResponse =>
                  Logger.error("Account Closed")
                  doAudit(lisaManager, accountId, updateSubsRequest, failureEvent,
                    Map(failureReason -> ErrorAccountAlreadyClosed.errorCode))
                  LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                  Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
                case UpdateSubscriptionAccountVoidedResponse =>
                  Logger.error("Account Voided")
                  doAudit(lisaManager, accountId, updateSubsRequest, failureEvent,
                    Map(failureReason -> ErrorAccountAlreadyVoided.errorCode))
                  LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                  Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
                case _ =>
                  Logger.debug("Matched Error")
                  doAudit(lisaManager, accountId, updateSubsRequest, failureEvent,
                    Map(failureReason -> ErrorInternalServerError.errorCode))
                  Logger.error(s"First Subscription date not updated : DES unknown case , returning internal server error")
                  LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                  InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            }
          }
        }, lisaManager = lisaManager)
      }
    }
  }

  private def withValidDates(lisaManager: String, accountId: String, updateSubsRequest: UpdateSubscriptionRequest)
                                     (success: () => Future[Result])
                                     (implicit hc: HeaderCarrier, startTime: Long): Future[Result] = {

    if (updateSubsRequest.firstSubscriptionDate.isBefore(LISA_START_DATE)) {
      Logger.debug("First Subscription date not updated - failed business rule validation")

      doAudit(lisaManager, accountId, updateSubsRequest, "firstSubscriptionDateNotUpdated", Map("reasonNotUpdated" -> "FORBIDDEN"))

      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.UPDATE_SUBSCRIPTION)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("firstSubscriptionDate"), Some("/firstSubscriptionDate"))
      )))))
    }
    else {
      success()
    }
  }

  private def doAudit(lisaManager: String,
                      accountId: String,
                      updateSubsRequest: UpdateSubscriptionRequest,
                      auditType: String,
                      extraData: Map[String, String] = Map())
                     (implicit hc: HeaderCarrier) = {
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

