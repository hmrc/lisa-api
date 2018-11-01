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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PropertyPurchaseController extends LisaController with LisaConstants {

  val service: LifeEventService = LifeEventService
  val auditService: AuditService = AuditService

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off method.length
  def requestFundRelease(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestFundReleaseRequest](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Fund release not reported - invalid event date")

                doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
                LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              }
              else {
                service.reportLifeEvent(lisaManager, accountId, req) map {
                  case res: ReportLifeEventSuccessResponse => {
                    Logger.debug("Fund release successful")
                    doFundReleaseAudit(lisaManager, accountId, req, true)
                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _:InitialFundReleaseRequest => ApiResponseData(message = "Fund release created", fundReleaseId = Some(res.lifeEventId))
                      case _:SupersedeFundReleaseRequest => ApiResponseData(message = "Fund release superseded", fundReleaseId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case ReportLifeEventAccountClosedResponse => {
                    Logger.debug("Fund release account closed")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyClosed.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
                  }
                  case ReportLifeEventAccountCancelledResponse => {
                    Logger.debug("Fund release account cancelled")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyCancelled.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyCancelled))
                  }
                  case ReportLifeEventAccountVoidResponse => {
                    Logger.debug("Fund release account voided")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyVoided.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
                  }
                  case ReportLifeEventMismatchResponse => {
                    Logger.debug("Fund release mismatch")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseMismatch.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorFundReleaseMismatch))
                  }
                  case ReportLifeEventAccountNotOpenLongEnoughResponse => {
                    Logger.debug("Fund release account not open long enough")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotOpenLongEnough.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountNotOpenLongEnough))
                  }
                  case ReportLifeEventOtherPurchaseOnRecordResponse => {
                    Logger.debug("Fund release other purchase on record")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseOtherPropertyOnRecord.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorFundReleaseOtherPropertyOnRecord))
                  }
                  case ReportLifeEventAccountNotFoundResponse => {
                    Logger.debug("Fund release account not found")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorAccountNotFound))
                  }
                  case ReportLifeEventAlreadyExistsResponse => {
                    Logger.debug("Fund release already exists")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseAlreadyExists.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorFundReleaseAlreadyExists))
                  }
                  case ReportLifeEventAlreadySupersededResponse => {
                    Logger.debug("Fund release already superseded")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseAlreadySuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorFundReleaseAlreadySuperseded))
                  }
                  case unexpected:ReportLifeEventResponse => {
                    Logger.debug(s"Fund release error: $unexpected")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorInternalServerError.errorCode))
                    LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.PROPERTY_PURCHASE)
                    InternalServerError(Json.toJson(ErrorInternalServerError))
                  }
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  // scalastyle:off cyclomatic.complexity
  // scalastyle:off method.length
  def requestExtension(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestPurchaseExtension](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Extension not reported - invalid event date")

                doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
                LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              }
              else {
                service.reportLifeEvent(lisaManager, accountId, req) map {
                  case res: ReportLifeEventSuccessResponse => {
                    Logger.debug("Extension successful")
                    doExtensionAudit(lisaManager, accountId, req, true)
                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestStandardPurchaseExtension => ApiResponseData(message = "Extension created", extensionId = Some(res.lifeEventId))
                      case _: RequestSupersededPurchaseExtension => ApiResponseData(message = "Extension superseded", extensionId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case ReportLifeEventAccountClosedResponse => {
                    Logger.debug("Extension account closed")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyClosed.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
                  }
                  case ReportLifeEventAccountCancelledResponse => {
                    Logger.debug("Extension account cancelled")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyCancelled.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyCancelled))
                  }
                  case ReportLifeEventAccountVoidResponse => {
                    Logger.debug("Extension account voided")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyVoided.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
                  }
                  case ReportLifeEventExtensionOneNotYetApprovedResponse => {
                    Logger.debug("Extension one not yet approved")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionOneNotApproved.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionOneNotApproved))
                  }
                  case ReportLifeEventExtensionOneAlreadyApprovedResponse => {
                    Logger.debug("Extension one already approved")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionOneAlreadyApproved.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionOneAlreadyApproved))
                  }
                  case ReportLifeEventExtensionTwoAlreadyApprovedResponse => {
                    Logger.debug("Extension two already approved")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionTwoAlreadyApproved.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionTwoAlreadyApproved))
                  }
                  case ReportLifeEventMismatchResponse => {
                    Logger.debug("Extension mismatch")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionMismatch.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionMismatch))
                  }
                  case ReportLifeEventAccountNotFoundResponse => {
                    Logger.debug("Extension account not found")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorAccountNotFound))
                  }
                  case ReportLifeEventFundReleaseNotFoundResponse => {
                    Logger.debug("Extension fund release not found")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionFundReleaseNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorExtensionFundReleaseNotFound))
                  }
                  case ReportLifeEventAlreadyExistsResponse => {
                    Logger.debug("Extension already exists")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionAlreadyExists.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorExtensionAlreadyExists))
                  }
                  case ReportLifeEventFundReleaseSupersededResponse => {
                    Logger.debug("Extension fund release superseded")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionFundReleaseSuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorExtensionFundReleaseSuperseded))
                  }
                  case ReportLifeEventAlreadySupersededResponse => {
                    Logger.debug("Extension already superseded")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionAlreadySuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorExtensionAlreadySuperseded))
                  }
                  case unexpected:ReportLifeEventResponse => {
                    Logger.debug(s"Extension error: $unexpected")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorInternalServerError.errorCode))
                    LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.PROPERTY_PURCHASE)
                    InternalServerError(Json.toJson(ErrorInternalServerError))
                  }
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  def reportPurchaseOutcome(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      implicit val startTime = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestPurchaseOutcomeRequest](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Purchase outcome not reported - invalid event date")

                doOutcomeAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
                LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              }
              else {
                service.reportLifeEvent(lisaManager, accountId, req) map {
                  case res: ReportLifeEventSuccessResponse => {
                    Logger.debug("Purchase outcome successful")
                    doOutcomeAudit(lisaManager, accountId, req, true)
                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestPurchaseOutcomeStandardRequest => ApiResponseData(message = "Purchase outcome created", extensionId = Some(res.lifeEventId))
                      case _: RequestPurchaseOutcomeSupersededRequest => ApiResponseData(message = "Purchase outcome superseded", extensionId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case unexpected: ReportLifeEventResponse => {
                    Logger.debug(s"Purchase outcome error: $unexpected")
                    doOutcomeAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorInternalServerError.errorCode))
                    LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.PROPERTY_PURCHASE)
                    InternalServerError(Json.toJson(ErrorInternalServerError))
                  }
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  private def doFundReleaseAudit(lisaManager: String, accountId: String, req: RequestFundReleaseRequest, success: Boolean, extraData: Map[String, String] = Map())
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

  private def doExtensionAudit(lisaManager: String, accountId: String, req: RequestPurchaseExtension, success: Boolean, extraData: Map[String, String] = Map())
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

  private def doOutcomeAudit(lisaManager: String, accountId: String, req: RequestPurchaseOutcomeRequest, success: Boolean, extraData: Map[String, String] = Map())
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
