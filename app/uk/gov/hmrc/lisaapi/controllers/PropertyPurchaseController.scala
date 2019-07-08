/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent, ControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{ReportLifeEventFundReleaseNotFoundResponse, ReportLifeEventMismatchResponse, _}
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._

import scala.concurrent.{ExecutionContext, Future}

class PropertyPurchaseController @Inject() (
                                             authConnector: AuthConnector,
                                             appContext: AppContext,
                                             service: LifeEventService,
                                             auditService: AuditService,
                                             lisaMetrics: LisaMetrics,
                                             cc: ControllerComponents,
                                             parse: PlayBodyParsers
                                           )(implicit ec: ExecutionContext) extends LisaController(
  cc: ControllerComponents,
  lisaMetrics: LisaMetrics,
  appContext: AppContext,
  authConnector: AuthConnector
) {

  override val validateVersion: String => Boolean = _ == "2.0"

  def requestFundRelease(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader(parse).async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidAddress(request.body.asJson) { () =>
            withValidJson[RequestFundReleaseRequest](
              req => {
                if (conveyancerOrPropertyDetailsIncludedOnASupersedeRequest(request.body.asJson)) {
                  Logger.debug("Fund release not reported - conveyancer and/or property details included on a supersede request")
                  auditFundRelease(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> ErrorInvalidDataProvided.errorCode))
                  lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                  Future.successful(Forbidden(Json.toJson(ErrorInvalidDataProvided)))
                } else if (req.eventDate.isBefore(LISA_START_DATE)) {
                  Logger.debug("Fund release not reported - invalid event date")
                  auditFundRelease(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> "FORBIDDEN"))
                  lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)
                  Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                    ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                  )))))
                } else {
                  service.reportLifeEvent(lisaManager, accountId, req).map {
                    case res: ReportLifeEventSuccessResponse =>
                      Logger.debug("Fund release successful")
                      auditFundRelease(lisaManager, accountId, req, success = true)
                      lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                      val data = req match {
                        case _: InitialFundReleaseRequest => ApiResponseData(message = "Fund release created", lifeEventId = Some(res.lifeEventId))
                        case _: SupersedeFundReleaseRequest => ApiResponseData(message = "Fund release superseded", lifeEventId = Some(res.lifeEventId))
                      }
                      Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                    case res: ReportLifeEventResponse =>
                      val response = fundReleaseErrors.applyOrElse(res, { _: ReportLifeEventResponse => ErrorInternalServerError })
                      Logger.debug(s"Fund Release received $res, responding with $response")
                      auditFundRelease(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> response.errorCode))
                      lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.PROPERTY_PURCHASE)
                      response.asResult
                  } recover {
                    case e: Exception => Logger.error(s"************ ${e.getMessage}"); ???
                  }
                }
              },
              lisaManager = lisaManager
            )
          }
        }
      }
  }

  def requestExtension(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader(parse).async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestPurchaseExtension](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Extension not reported - invalid event date")

                auditExtension(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> "FORBIDDEN"))
                lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              } else {
                service.reportLifeEvent(lisaManager, accountId, req).map {
                  case res: ReportLifeEventSuccessResponse =>
                    Logger.debug("Extension successful")
                    auditExtension(lisaManager, accountId, req, success = true)
                    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestStandardPurchaseExtension => ApiResponseData(message = "Extension created", lifeEventId = Some(res.lifeEventId))
                      case _: RequestSupersededPurchaseExtension => ApiResponseData(message = "Extension superseded", lifeEventId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  case res: ReportLifeEventResponse =>
                    val response = extensionErrors.applyOrElse(res, {_ : ReportLifeEventResponse => ErrorInternalServerError})
                    Logger.debug(s"Extension received $res, responding with $response")
                    auditExtension(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> response.errorCode))
                    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.PROPERTY_PURCHASE)
                    Status(response.httpStatusCode)(Json.toJson(response))
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  def reportPurchaseOutcome(lisaManager: String, accountId: String): Action[AnyContent] = validateHeader(parse).async {
    implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withValidAccountId(accountId) { () =>
          withValidJson[RequestPurchaseOutcomeRequest](
            req =>
              if (req.eventDate.isBefore(LISA_START_DATE)) {
                Logger.debug("Purchase outcome not reported - invalid event date")

                auditOutcome(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> "FORBIDDEN"))
                lisaMetrics.incrementMetrics(startTime, FORBIDDEN, LisaMetricKeys.PROPERTY_PURCHASE)

                Future.successful(Forbidden(Json.toJson(ErrorForbidden(List(
                  ErrorValidation(DATE_ERROR, LISA_START_DATE_ERROR.format("eventDate"), Some("/eventDate"))
                )))))
              } else {
                service.reportLifeEvent(lisaManager, accountId, req) map {
                  case res: ReportLifeEventSuccessResponse =>
                    Logger.debug("Purchase outcome successful")
                    auditOutcome(lisaManager, accountId, req, success = true)
                    lisaMetrics.incrementMetrics(startTime, CREATED, LisaMetricKeys.PROPERTY_PURCHASE)
                    val data = req match {
                      case _: RequestPurchaseOutcomeCompletedRequest | _: RequestPurchaseOutcomeFailedRequest =>
                        ApiResponseData(message = "Purchase outcome created", lifeEventId = Some(res.lifeEventId))
                      case _: RequestPurchaseOutcomeSupersededCompletedRequest | _: RequestPurchaseOutcomeSupersededFailedRequest =>
                        ApiResponseData(message = "Purchase outcome superseded", lifeEventId = Some(res.lifeEventId))
                    }
                    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
                  case res: ReportLifeEventResponse =>
                    val response: ErrorResponse = outcomeErrors.applyOrElse(res, {_ : ReportLifeEventResponse => ErrorInternalServerError})
                    Logger.debug(s"Purchase outcome received $res, responding with $response")
                    auditOutcome(lisaManager, accountId, req, success = false, Map("reasonNotReported" -> response.errorCode))
                    lisaMetrics.incrementMetrics(startTime, response.httpStatusCode, LisaMetricKeys.PROPERTY_PURCHASE)
                    Status(response.httpStatusCode)(Json.toJson(response))
                }
              },
            lisaManager = lisaManager
          )
        }
      }
  }

  private def conveyancerOrPropertyDetailsIncludedOnASupersedeRequest(req: Option[JsValue]) = {
    req exists { json =>
      val supersedeIsDefined = (json \ "supersede").asOpt[JsValue].isDefined
      val conveyancerIsDefined = (json \ "conveyancerReference").asOpt[JsValue].isDefined
      val propertyDetailsAreDefined = (json \ "propertyDetails").asOpt[JsValue].isDefined

      supersedeIsDefined && (conveyancerIsDefined || propertyDetailsAreDefined)
    }
  }

  private val commonErrors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = {
    case ReportLifeEventAccountClosedResponse => ErrorAccountAlreadyClosed
    case ReportLifeEventAccountCancelledResponse => ErrorAccountAlreadyCancelled
    case ReportLifeEventAccountVoidResponse => ErrorAccountAlreadyVoided
    case ReportLifeEventAccountNotFoundResponse => ErrorAccountNotFound
    case ReportLifeEventAlreadySupersededResponse(lifeEventId) => ErrorLifeEventAlreadySuperseded(lifeEventId)
    case ReportLifeEventAlreadyExistsResponse(lifeEventId) => ErrorLifeEventAlreadyExists(lifeEventId)
    case ReportLifeEventMismatchResponse => ErrorLifeEventMismatch
    case ReportLifeEventServiceUnavailableResponse => ErrorServiceUnavailable
  }

  private val fundReleaseErrors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = commonErrors.orElse({
    case ReportLifeEventAccountNotOpenLongEnoughResponse => ErrorAccountNotOpenLongEnough
    case ReportLifeEventOtherPurchaseOnRecordResponse => ErrorFundReleaseOtherPropertyOnRecord
    case ReportLifeEventInvalidPayload => ErrorBadRequestInvalidPayload
  })

  private val extensionErrors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = commonErrors.orElse({
    case ReportLifeEventExtensionOneNotYetApprovedResponse => ErrorExtensionOneNotApproved
    case ReportLifeEventExtensionOneAlreadyApprovedResponse(lifeEventId) => ErrorExtensionOneAlreadyApproved(lifeEventId)
    case ReportLifeEventExtensionTwoAlreadyApprovedResponse(lifeEventId) => ErrorExtensionTwoAlreadyApproved(lifeEventId)
    case ReportLifeEventFundReleaseNotFoundResponse => ErrorFundReleaseNotFound
    case ReportLifeEventFundReleaseSupersededResponse(lifeEventId) => ErrorFundReleaseSuperseded(lifeEventId)
  })

  // common errors not included as it should be possible to complete a purchase on a closed/cancelled/void account
  private val outcomeErrors: PartialFunction[ReportLifeEventResponse, ErrorResponse] = {
    case ReportLifeEventMismatchResponse => ErrorLifeEventMismatch
    case ReportLifeEventFundReleaseNotFoundResponse => ErrorFundReleaseNotFound
    case ReportLifeEventAccountNotFoundResponse => ErrorAccountNotFound
    case ReportLifeEventFundReleaseSupersededResponse(lifeEventId) => ErrorFundReleaseSuperseded(lifeEventId)
    case ReportLifeEventAlreadySupersededResponse(lifeEventId) => ErrorLifeEventAlreadySuperseded(lifeEventId)
    case ReportLifeEventAlreadyExistsResponse(lifeEventId) => ErrorLifeEventAlreadyExists(lifeEventId)
    case ReportLifeEventServiceUnavailableResponse => ErrorServiceUnavailable
  }

  private def auditFundRelease(lisaManager: String,
                                 accountId: String,
                                 req: RequestFundReleaseRequest,
                                 success: Boolean,
                                 extraData: Map[String, String] = Map())
                                (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "fundReleaseReported" else "fundReleaseNotReported",
      path = s"/manager/$lisaManager/accounts/$accountId/events/fund-releases",
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

  private def auditExtension(lisaManager: String,
                               accountId: String,
                               req: RequestPurchaseExtension,
                               success: Boolean,
                               extraData: Map[String, String] = Map())
                                (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "extensionReported" else "extensionNotReported",
      path = s"/manager/$lisaManager/accounts/$accountId/events/purchase-extensions",
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

  private def auditOutcome(lisaManager: String,
                             accountId: String,
                             req: RequestPurchaseOutcomeRequest,
                             success: Boolean,
                             extraData: Map[String, String] = Map())
                            (implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = if (success) "purchaseOutcomeReported" else "purchaseOutcomeNotReported",
      path = s"/manager/$lisaManager/accounts/$accountId/events/purchase-outcomes",
      auditData = req.toStringMap ++ Map(
        "lisaManagerReferenceNumber" -> lisaManager,
        "accountID" -> accountId
      ) ++ extraData
    )
  }

}