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
import uk.gov.hmrc.lisaapi.models.{UpdateSubscriptionSuccessResponse, _}
import uk.gov.hmrc.lisaapi.services.{AuditService, UpdateSubscriptionService}

import scala.concurrent.{ExecutionContext, Future}

class UpdateSubscriptionController @Inject()(
                                              authConnector: AuthConnector,
                                              appContext: AppContext,
                                              service: UpdateSubscriptionService,
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

  def updateSubscription(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader(parse) andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async {
      implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        logger.info(s"[UpdateSubscriptionController][updateSubscription] lisaManager: $lisaManager, accountId: $accountId")
        withValidJson[UpdateSubscriptionRequest](
          updateSubsRequest =>
            withValidDates(lisaManager, accountId, updateSubsRequest) { () =>
              service.updateSubscription(lisaManager, accountId, updateSubsRequest) map { result =>
                logger.info(s"[UpdateSubscriptionController][updateSubscription] response : ${request.toString()} lisaManager: $lisaManager, accountId: $accountId")
                result match {
                  case success: UpdateSubscriptionSuccessResponse =>
                    logger.info("First Subscription date updated")
                    logger.info(s"[UpdateSubscriptionController][updateSubscription] First Subscription date updated lisaManager: $lisaManager, accountId: $accountId")
                    auditUpdateSubscription(lisaManager, accountId, updateSubsRequest)
                    val data =
                      ApiResponseData(message = success.message, code = Some(success.code), accountId = Some(accountId))
                    lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.UPDATE_SUBSCRIPTION)
                    Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = OK)))
                  case UpdateSubscriptionAccountNotFoundResponse =>
                    error(ErrorAccountNotFound, lisaManager, accountId, updateSubsRequest)
                  case UpdateSubscriptionAccountClosedResponse =>
                    error(ErrorAccountAlreadyClosed, lisaManager, accountId, updateSubsRequest)
                  case UpdateSubscriptionAccountCancelledResponse =>
                    error(ErrorAccountAlreadyCancelled, lisaManager, accountId, updateSubsRequest)
                  case UpdateSubscriptionAccountVoidedResponse =>
                    error(ErrorAccountAlreadyVoided, lisaManager, accountId, updateSubsRequest)
                  case UpdateSubscriptionServiceUnavailableResponse =>
                    error(ErrorServiceUnavailable, lisaManager, accountId, updateSubsRequest)
                  case _ =>
                    logger.error(s"[UpdateSubscriptionController][updateSubscription]  in errorResponse accountId : $accountId, lisaManager : $lisaManager")
                    error(ErrorInternalServerError, lisaManager, accountId, updateSubsRequest)
                }
              }
            },
          lisaManager = lisaManager
        )
    }

  private def error(e: ErrorResponse, lisaManager: String, accountId: String, req: UpdateSubscriptionRequest)(implicit
                                                                                                              hc: HeaderCarrier,
                                                                                                              startTime: Long
  ): Result = {
    logger.info(s"[UpdateSubscriptionController][error] failed for lisaManager : $lisaManager , accountId : $accountId, error : ${e.message}")
    auditUpdateSubscription(lisaManager, accountId, req, Some(e.errorCode))
    lisaMetrics.incrementMetrics(startTime, e.httpStatusCode, LisaMetricKeys.UPDATE_SUBSCRIPTION)
    e.asResult
  }

  private def withValidDates(lisaManager: String, accountId: String, updateSubsRequest: UpdateSubscriptionRequest)(
    success: () => Future[Result]
  )(implicit hc: HeaderCarrier, startTime: Long): Future[Result] =
    if (updateSubsRequest.firstSubscriptionDate.isBefore(LISA_START_DATE)) {
      logger.info("First Subscription date not updated - failed business rule validation")

      auditUpdateSubscription(lisaManager, accountId, updateSubsRequest, Some("FORBIDDEN"))

      lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.UPDATE_SUBSCRIPTION)

      Future.successful(
        Forbidden(
          ErrorForbidden(
            List(
              ErrorValidation(
                DATE_ERROR,
                LISA_START_DATE_ERROR.format("firstSubscriptionDate"),
                Some("/firstSubscriptionDate")
              )
            )
          ).asJson
        )
      )
    } else {
      success()
    }

  private def auditUpdateSubscription(
                                       lisaManager: String,
                                       accountId: String,
                                       updateSubsRequest: UpdateSubscriptionRequest,
                                       failureReason: Option[String] = None
                                     )(implicit hc: HeaderCarrier) = {
    val path = s"/manager/$lisaManager/accounts/$accountId/update-subscription"
    val auditData = Map(
      "lisaManagerReferenceNumber" -> lisaManager,
      "accountID" -> accountId,
      "firstSubscriptionDate" -> updateSubsRequest.firstSubscriptionDate.toString
    )

    failureReason map { reason =>
      auditService.audit(
        auditType = "firstSubscriptionDateNotUpdated",
        path = path,
        auditData = auditData ++ Map("reasonNotUpdated" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "firstSubscriptionDateUpdated",
      path = path,
      auditData = auditData
    )
  }

}
