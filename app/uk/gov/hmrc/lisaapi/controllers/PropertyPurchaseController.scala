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
import play.api.mvc.{Action, AnyContent, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, PropertyPurchaseService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PropertyPurchaseController extends LisaController with LisaConstants {

  val service: PropertyPurchaseService = PropertyPurchaseService
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
                service.requestFundRelease(lisaManager, accountId, req) map {
                  case res: PropertyPurchaseSuccessResponse => {
                    Logger.debug("Fund release successful")
                    doFundReleaseAudit(lisaManager, accountId, req, true)
                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _:InitialFundReleaseRequest => ApiResponseData(message = "Fund release created", fundReleaseId = Some(res.id))
                      case _:SupersedeFundReleaseRequest => ApiResponseData(message = "Fund release superseded", fundReleaseId = Some(res.id))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case PropertyPurchaseAccountClosedResponse => {
                    Logger.debug("Fund release account closed")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyClosed.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
                  }
                  case PropertyPurchaseAccountCancelledResponse => {
                    Logger.debug("Fund release account cancelled")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyCancelled.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyCancelled))
                  }
                  case PropertyPurchaseAccountVoidResponse => {
                    Logger.debug("Fund release account voided")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyVoided.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
                  }
                  case PropertyPurchaseMismatchResponse => {
                    Logger.debug("Fund release mismatch")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseMismatch.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorFundReleaseMismatch))
                  }
                  case PropertyPurchaseAccountNotOpenLongEnoughResponse => {
                    Logger.debug("Fund release account not open long enough")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotOpenLongEnough.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountNotOpenLongEnough))
                  }
                  case PropertyPurchaseOtherPurchaseOnRecordResponse => {
                    Logger.debug("Fund release other purchase on record")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseOtherPropertyOnRecord.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorFundReleaseOtherPropertyOnRecord))
                  }
                  case PropertyPurchaseAccountNotFoundResponse => {
                    Logger.debug("Fund release account not found")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorAccountNotFound))
                  }
                  case PropertyPurchaseLifeEventAlreadyExistsResponse => {
                    Logger.debug("Fund release already exists")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseAlreadyExists.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorFundReleaseAlreadyExists))
                  }
                  case PropertyPurchaseLifeEventAlreadySupersededResponse => {
                    Logger.debug("Fund release already superseded")
                    doFundReleaseAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseAlreadySuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorFundReleaseAlreadySuperseded))
                  }
                  case _ => {
                    Logger.debug("Fund release error")
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
                service.requestPurchaseExtension(lisaManager, accountId, req) map {
                  case res: PropertyPurchaseSuccessResponse => {
                    Logger.debug("Extension successful")
                    doExtensionAudit(lisaManager, accountId, req, true)
                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestStandardPurchaseExtension => ApiResponseData(message = "Extension created", extensionId = Some(res.id))
                      case _: RequestSupersededPurchaseExtension => ApiResponseData(message = "Extension superseded", extensionId = Some(res.id))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case PropertyPurchaseAccountClosedResponse => {
                    Logger.debug("Extension account closed")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyClosed.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
                  }
                  case PropertyPurchaseAccountCancelledResponse => {
                    Logger.debug("Extension account cancelled")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyCancelled.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyCancelled))
                  }
                  case PropertyPurchaseAccountVoidResponse => {
                    Logger.debug("Extension account voided")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyVoided.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
                  }
                  case PropertyPurchaseExtensionOneNotYetApprovedResponse => {
                    Logger.debug("Extension one not yet approved")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionOneNotApproved.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionOneNotApproved))
                  }
                  case PropertyPurchaseExtensionOneAlreadyApprovedResponse => {
                    Logger.debug("Extension one already approved")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionOneAlreadyApproved.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionOneAlreadyApproved))
                  }
                  case PropertyPurchaseExtensionTwoAlreadyApprovedResponse => {
                    Logger.debug("Extension two already approved")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionTwoAlreadyApproved.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionTwoAlreadyApproved))
                  }
                  case PropertyPurchaseMismatchResponse => {
                    Logger.debug("Extension mismatch")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionMismatch.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorExtensionMismatch))
                  }
                  case PropertyPurchaseAccountNotFoundResponse => {
                    Logger.debug("Extension account not found")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorAccountNotFound))
                  }
                  case PropertyPurchaseFundReleaseNotFoundResponse => {
                    Logger.debug("Extension fund release not found")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionFundReleaseNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorExtensionFundReleaseNotFound))
                  }
                  case PropertyPurchaseLifeEventAlreadyExistsResponse => {
                    Logger.debug("Extension already exists")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionAlreadyExists.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorExtensionAlreadyExists))
                  }
                  case PropertyPurchaseFundReleaseSupersededResponse => {
                    Logger.debug("Extension fund release superseded")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionFundReleaseSuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorExtensionFundReleaseSuperseded))
                  }
                  case PropertyPurchaseLifeEventAlreadySupersededResponse => {
                    Logger.debug("Extension already superseded")
                    doExtensionAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorExtensionAlreadySuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorExtensionAlreadySuperseded))
                  }
                  case _ => {
                    Logger.debug("Extension error")
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

}
