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

import java.io

import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetrics, MetricsEnum}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, BonusPaymentService}
import uk.gov.hmrc.lisaapi.utils.LisaExtensions._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.collection.mutable.ListBuffer
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BonusPaymentController extends LisaController with LisaConstants {

  val service: BonusPaymentService = BonusPaymentService
  val auditService: AuditService = AuditService

  def requestBonusPayment(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime,MetricsEnum.BONUS_PAYMENT)

      withValidLMRN(lisaManager) {
        withValidJson[RequestBonusPaymentRequest](req =>
          (req.bonuses.claimReason, req.lifeEventId) match {
            case ("Life Event", None) =>
              handleLifeEventNotProvided(lisaManager, accountId, req)
            case _ =>
              service.requestBonusPayment(lisaManager, accountId, req) map { res =>
                Logger.debug("Entering Bonus Payment Controller and the response is " + res.toString)

                LisaMetrics.incrementMetrics(System.currentTimeMillis(), MetricsEnum.BONUS_PAYMENT)
                res match {
                  case RequestBonusPaymentSuccessResponse(transactionID) =>
                    handleSuccess(lisaManager, accountId, req, transactionID)
                  case errorResponse: RequestBonusPaymentErrorResponse =>
                    handleFailure(lisaManager, accountId, req, errorResponse)
                }
              } recover {
                case e: Exception => Logger.error(s"requestBonusPayment : An error occurred due to ${e.getMessage} returning internal server error")
                  handleError(lisaManager, accountId, req)

              }
          }, lisaManager = lisaManager
        )
      }
  }

  //scalastyle:off cyclomatic.complexity
  def newSubsOrTransferMustHaveValue(data: RequestBonusPaymentRequest, errors: Option[(Seq[(JsPath, Seq[ValidationError])])]):
    (RequestBonusPaymentRequest, Option[Seq[(JsPath, Seq[ValidationError])]]) = {

    val subsExists = data.inboundPayments.newSubsForPeriod.isDefined
    val htbExists = data.htbTransfer.isDefined
    val eitherExists = subsExists || htbExists

    val subsGtZero = subsExists && data.inboundPayments.newSubsForPeriod.get > 0
    val htbGtZero = htbExists && data.htbTransfer.get.htbTransferInForPeriod > 0
    val eitherHasValue = subsGtZero || htbGtZero

    val showSubError = !eitherExists || (subsExists && !eitherHasValue)
    val showHtbError = !eitherExists || (htbExists && !eitherHasValue)

    val errorMessage = "newSubsForPeriod and htbTransferForPeriod cannot both be zero"

    if (eitherHasValue) {
      (data, errors)
    }
    else {
      val newErrs = new ListBuffer[(JsPath, Seq[ValidationError])]()

      if (showSubError) newErrs += ((JsPath \ "inboundPayments" \ "newSubsForPeriod", Seq(ValidationError(errorMessage))))
      if (showHtbError) newErrs += ((JsPath \ "htbTransfer" \ "htbTransferInForPeriod", Seq(ValidationError(errorMessage))))

      (data, Some(newErrs))
    }

  }

  private def handleSuccess(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, transactionID: String)(implicit hc: HeaderCarrier) = {
    Logger.debug("Matched success response")
    val data = ApiResponseData(message = "Bonus transaction created", transactionId = Some(transactionID))

    auditService.audit(
      auditType = "bonusPaymentRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req)
    )

    Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = CREATED)))
  }

  private def handleFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, errorResponse: RequestBonusPaymentErrorResponse)
                           (implicit hc: HeaderCarrier) = {
    Logger.debug("Matched failure response")

    auditFailure(lisaManager, accountId, req, errorResponse.data.code)

    Status(errorResponse.status).apply(Json.toJson(errorResponse.data))
  }

  private def handleError(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)(implicit hc: HeaderCarrier) = {
    Logger.debug("An error occurred")

    auditFailure(lisaManager, accountId, req, ErrorInternalServerError.errorCode)

    InternalServerError(Json.toJson(ErrorInternalServerError))
  }

  private def handleLifeEventNotProvided(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest)(implicit hc: HeaderCarrier) = {
    Logger.debug("Life event not provided")

    auditFailure(lisaManager, accountId, req, ErrorLifeEventNotProvided.errorCode)

    Future.successful(Forbidden(Json.toJson(ErrorLifeEventNotProvided)))
  }

  private def auditFailure(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest, failureReason: String)(implicit hc: HeaderCarrier) = {
    auditService.audit(
      auditType = "bonusPaymentNotRequested",
      path = getEndpointUrl(lisaManager, accountId),
      auditData = createAuditData(lisaManager, accountId, req) ++ Map("reasonNotRequested" -> failureReason)
    )
  }

  private def createAuditData(lisaManager: String, accountId: String, req: RequestBonusPaymentRequest): Map[String, String] = {
    req.toStringMap ++ Map(ZREF -> lisaManager,
      "accountId" -> accountId)
  }

  private def getEndpointUrl(lisaManager: String, accountId: String): String = {
    s"/manager/$lisaManager/accounts/$accountId/transactions"
  }

}
