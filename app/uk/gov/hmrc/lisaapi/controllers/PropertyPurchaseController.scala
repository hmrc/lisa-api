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
              withValidDates(lisaManager, accountId, req) { () =>
                service.requestFundRelease(lisaManager, accountId, req) map {
                  case res: RequestFundReleaseSuccessResponse => {
                    Logger.debug("Fund release successful")
                    doAudit(lisaManager, accountId, req, true)
                    LisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _:InitialFundReleaseRequest => ApiResponseData(message = "Fund release created", fundReleaseId = Some(res.id))
                      case _:SupersedeFundReleaseRequest => ApiResponseData(message = "Fund release superseded", fundReleaseId = Some(res.id))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  }
                  case RequestFundReleaseAccountClosedResponse => {
                    Logger.debug("Fund release account closed")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyClosed.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
                  }
                  case RequestFundReleaseAccountCancelledResponse => {
                    Logger.debug("Fund release account cancelled")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyCancelled.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyCancelled))
                  }
                  case RequestFundReleaseAccountVoidResponse => {
                    Logger.debug("Fund release account voided")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountAlreadyVoided.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountAlreadyVoided))
                  }
                  case RequestFundReleaseMismatchResponse => {
                    Logger.debug("Fund release mismatch")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseMismatch.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorFundReleaseMismatch))
                  }
                  case RequestFundReleaseAccountNotOpenLongEnoughResponse => {
                    Logger.debug("Fund release account not open long enough")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotOpenLongEnough.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorAccountNotOpenLongEnough))
                  }
                  case RequestFundReleaseOtherPurchaseOnRecordResponse => {
                    Logger.debug("Fund release other purchase on record")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseOtherPropertyOnRecord.errorCode))
                    LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                    Forbidden(Json.toJson(ErrorFundReleaseOtherPropertyOnRecord))
                  }
                  case RequestFundReleaseAccountNotFoundResponse => {
                    Logger.debug("Fund release account not found")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorAccountNotFound.errorCode))
                    LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.PROPERTY_PURCHASE)
                    NotFound(Json.toJson(ErrorAccountNotFound))
                  }
                  case RequestFundReleaseLifeEventAlreadyExistsResponse => {
                    Logger.debug("Fund release already exists")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseAlreadyExists.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorFundReleaseAlreadyExists))
                  }
                  case RequestFundReleaseLifeEventAlreadySupersededResponse => {
                    Logger.debug("Fund release already superseded")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorFundReleaseAlreadySuperseded.errorCode))
                    LisaMetrics.incrementMetrics(startTime, CONFLICT, LisaMetricKeys.PROPERTY_PURCHASE)
                    Conflict(Json.toJson(ErrorFundReleaseAlreadySuperseded))
                  }
                  case _ => {
                    Logger.debug("Fund release error")
                    doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> ErrorInternalServerError.errorCode))
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

  private def doAudit(lisaManager: String, accountId: String, req: RequestFundReleaseRequest, success: Boolean, extraData: Map[String, String] = Map())
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

  private def withValidDates(lisaManager: String, accountId: String, req: RequestFundReleaseRequest)
                            (success: () => Future[Result])
                            (implicit hc: HeaderCarrier, startTime: Long) = {
    if (req.eventDate.isBefore(LISA_START_DATE)) {
      Logger.debug("Fund release not reported - invalid event date")

      doAudit(lisaManager, accountId, req, false, Map("reasonNotReported" -> "FORBIDDEN"))
      LisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

      Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
        ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
      )))))
    }
    else {
      success()
    }
  }

}
