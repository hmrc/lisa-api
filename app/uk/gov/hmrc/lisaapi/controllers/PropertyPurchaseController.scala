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
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.{LisaConstants, controllers}
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{ReportLifeEventFundReleaseNotFoundResponse, ReportLifeEventMismatchResponse, _}
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.{ExecutionContext, Future}

class PropertyPurchaseController @Inject() (
                                             val authConnector: AuthConnector,
                                             val appContext: AppContext,
                                             service: LifeEventService,
                                             auditService: AuditService,
                                             val lisaMetrics: LisaMetrics
                                           )(implicit ec: ExecutionContext) extends LisaController {

  override val validateVersion: String => Boolean = _ == "2.0"

  def requestFundRelease(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader().async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestFundReleaseRequest](
            req => {
              if (conveyancerOrPropertyDetailsIncludedOnASupersedeRequest(request.body.asJson)) {
                Logger.debug("Fund release not reported - conveyancer and/or property details included on a supersede request")
                doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorInvalidDataProvided.errorCode))
                lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                Future.successful(Forbidden(Json.toJson(ErrorInvalidDataProvided)))
              }
              else if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Fund release not reported - invalid event date")
                doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
                lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              }
              else {
                service.reportLifeEvent(lisaManager, accountId, req).map {
                  case res: ReportLifeEventSuccessResponse => {
                    Logger.debug("Fund release successful")
                    doFundReleaseAudit(lisaManager, accountId, req, true)
                    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: InitialFundReleaseRequest => ApiResponseData(message = "Fund release created", lifeEventId = Some(res.lifeEventId))
                      case _: SupersedeFundReleaseRequest => ApiResponseData(message = "Fund release superseded", lifeEventId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case res: ReportLifeEventResponse => {
                    val response = fundReleaseErrors.getOrElse(res, ErrorInternalServerError)
                    Logger.debug(s"Fund Release received $res, responding with $response")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> response.errorCode))
                    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.PROPERTY_PURCHASE)
                    response.asResult
                  }
                }
              }
            },
            lisaManager = lisaManager
          )
        }
      }
  }

  def requestExtension(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader().async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestPurchaseExtension](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Extension not reported - invalid event date")

                doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
                lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              }
              else {
                service.reportLifeEvent(lisaManager, accountId, req).map {
                  case res: ReportLifeEventSuccessResponse => {
                    Logger.debug("Extension successful")
                    doExtensionAudit(lisaManager, accountId, req, true)
                    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestStandardPurchaseExtension => ApiResponseData(message = "Extension created", lifeEventId = Some(res.lifeEventId))
                      case _: RequestSupersededPurchaseExtension => ApiResponseData(message = "Extension superseded", lifeEventId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case res: ReportLifeEventResponse => {
                    val response = extensionErrors.getOrElse(res, ErrorInternalServerError)
                    Logger.debug(s"Extension received $res, responding with $response")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> response.errorCode))
                    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.PROPERTY_PURCHASE)
                    Status(response.httpStatusCode)(Json.toJson(response))
                  }
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  def reportPurchaseOutcome(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader().async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestPurchaseOutcomeRequest](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Purchase outcome not reported - invalid event date")

                doOutcomeAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
                lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              }
              else {
                service.reportLifeEvent(lisaManager, accountId, req) map {
                  case res: ReportLifeEventSuccessResponse => {
                    Logger.debug("Purchase outcome successful")
                    doOutcomeAudit(lisaManager, accountId, req, true)
                    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestPurchaseOutcomeStandardRequest => {
                        ApiResponseData(message = "Purchase outcome created", lifeEventId = Some(res.lifeEventId))
                      }
                      case _: RequestPurchaseOutcomeSupersededRequest => {
                        ApiResponseData(message = "Purchase outcome superseded", lifeEventId = Some(res.lifeEventId))
                      }
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case res: ReportLifeEventResponse => {
                    val response = outcomeErrors.getOrElse(res, ErrorInternalServerError)
                    Logger.debug(s"Purchase outcome received $res, responding with $response")
                    doOutcomeAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> response.errorCode))
                    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.PROPERTY_PURCHASE)
                    Status(response.httpStatusCode)(Json.toJson(response))
                  }
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  private def conveyancerOrPropertyDetailsIncludedOnASupersedeRequest(req: Option[JsValue]) = {
    req match {
      case None => false
      case Some(json) => {
        val supersedeIsDefined = (json \ "supersede").asOpt[JsValue].isDefined
        val conveyancerIsDefined = (json \ "conveyancerReference").asOpt[JsValue].isDefined
        val propertyDetailsAreDefined = (json \ "propertyDetails").asOpt[JsValue].isDefined

        (supersedeIsDefined && (conveyancerIsDefined || propertyDetailsAreDefined))
      }
    }
  }

  private val commonErrors = Map[ReportLifeEventResponse, ErrorResponse] (
    ReportLifeEventAccountClosedResponse -> ErrorAccountAlreadyClosed,
    ReportLifeEventAccountCancelledResponse -> ErrorAccountAlreadyCancelled,
    ReportLifeEventAccountVoidResponse -> ErrorAccountAlreadyVoided,
    ReportLifeEventAccountNotFoundResponse -> ErrorAccountNotFound,
    ReportLifeEventAlreadySupersededResponse -> ErrorLifeEventAlreadySuperseded,
    ReportLifeEventAlreadyExistsResponse -> ErrorLifeEventAlreadyExists,
    ReportLifeEventMismatchResponse -> ErrorLifeEventMismatch
  )

  private val fundReleaseErrors = commonErrors ++ Map (
    ReportLifeEventAccountNotOpenLongEnoughResponse -> ErrorAccountNotOpenLongEnough,
    ReportLifeEventOtherPurchaseOnRecordResponse -> ErrorFundReleaseOtherPropertyOnRecord
  )

  private val extensionErrors = commonErrors ++ Map (
    ReportLifeEventExtensionOneNotYetApprovedResponse -> ErrorExtensionOneNotApproved,
    ReportLifeEventExtensionOneAlreadyApprovedResponse -> ErrorExtensionOneAlreadyApproved,
    ReportLifeEventExtensionTwoAlreadyApprovedResponse -> ErrorExtensionTwoAlreadyApproved,
    ReportLifeEventFundReleaseNotFoundResponse -> ErrorFundReleaseNotFound,
    ReportLifeEventFundReleaseSupersededResponse -> ErrorFundReleaseSuperseded
  )

  // common errors not included as it should be possible to complete a purchase on a closed/cancelled/void account
  private val outcomeErrors = Map[ReportLifeEventResponse, ErrorResponse] (
    ReportLifeEventMismatchResponse -> ErrorLifeEventMismatch,
    ReportLifeEventFundReleaseNotFoundResponse -> ErrorFundReleaseNotFound,
    ReportLifeEventAccountNotFoundResponse -> ErrorAccountNotFound,
    ReportLifeEventFundReleaseSupersededResponse -> ErrorFundReleaseSuperseded,
    ReportLifeEventAlreadySupersededResponse -> ErrorLifeEventAlreadySuperseded,
    ReportLifeEventAlreadyExistsResponse -> ErrorLifeEventAlreadyExists
  )

  private def doFundReleaseAudit(lisaManager: String,
                                 accountId: String,
                                 req: RequestFundReleaseRequest,
                                 success: Boolean,
                                 extraData: Map[String, String] = Map())
                                (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "fundReleaseReported" else "fundReleaseNotReported",
      path = s"/manager/$lisaManager/accounts/$accountId/property-purchase",
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

  private def doExtensionAudit(lisaManager: String,
                               accountId: String,
                               req: RequestPurchaseExtension,
                               success: Boolean,
                               extraData: Map[String, String] = Map())
                                (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "extensionReported" else "extensionNotReported",
      path = s"/manager/$lisaManager/accounts/$accountId/property-purchase/extension",
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

  private def doOutcomeAudit(lisaManager: String,
                             accountId: String,
                             req: RequestPurchaseOutcomeRequest,
                             success: Boolean,
                             extraData: Map[String, String] = Map())
                            (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "purchaseOutcomeReported" else "purchaseOutcomeNotReported",
      path = s"/manager/$lisaManager/accounts/$accountId/property-purchase/outcome",
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

}