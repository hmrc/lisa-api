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
import play.api.mvc.{Action, AnyContent, ControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.{AuditService, TransactionService}

import scala.concurrent.{ExecutionContext, Future}

class TransactionController @Inject()(
                                       authConnector: AuthConnector,
                                       appContext: AppContext,
                                       service: TransactionService,
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

  def getTransaction(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateHeader(parse).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      logger.info(s"[TransactionController][getTransaction]  accountId : $accountId, lisaManager : $lisaManager")

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { _ =>
          withValidAccountId(accountId) { () =>
            withValidTransactionId(transactionId) { () =>
              service.getTransaction(lisaManager, accountId, transactionId) flatMap {
                case success: GetTransactionSuccessResponse =>
                  logger.info("Matched Valid Response")
                  lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.TRANSACTION)

                  withApiVersion {
                    case Some(VERSION_1) =>
                      if (success.paymentStatus == TransactionPaymentStatus.REFUND_CANCELLED) {
                        auditGetTransaction(
                          lisaManager,
                          accountId,
                          transactionId,
                          Some(ErrorInternalServerError.errorCode)
                        )
                        Future.successful(ErrorInternalServerError.asResult)
                      } else {
                        auditGetTransaction(lisaManager, accountId, transactionId)
                        Future.successful(Ok(Json.toJson(success.copy(transactionType = None, supersededBy = None))))
                      }
                    case Some(VERSION_2) =>
                      auditGetTransaction(lisaManager, accountId, transactionId)
                      Future.successful(Ok(Json.toJson(success.copy(bonusDueForPeriod = None))))
                  }
                case GetTransactionTransactionNotFoundResponse =>
                  logger.info("Matched Not Found Response")
                  lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.TRANSACTION)

                  withApiVersion {
                    case Some(VERSION_1) =>
                      auditGetTransaction(
                        lisaManager,
                        accountId,
                        transactionId,
                        Some(ErrorBonusPaymentTransactionNotFound.errorCode)
                      )
                      Future.successful(ErrorBonusPaymentTransactionNotFound.asResult)
                    case Some(VERSION_2) =>
                      auditGetTransaction(
                        lisaManager,
                        accountId,
                        transactionId,
                        Some(ErrorTransactionNotFound.errorCode)
                      )
                      Future.successful(ErrorTransactionNotFound.asResult)
                  }
                case res: GetTransactionResponse =>
                  logger.warn("Matched an error")
                  val errorResponse = errors.applyOrElse(
                    res,
                    { _: GetTransactionResponse =>
                      logger.error(s"Matched an unexpected response: $res, returning a 500 error")
                      ErrorInternalServerError
                    }
                  )
                  auditGetTransaction(lisaManager, accountId, transactionId, Some(errorResponse.errorCode))
                  lisaMetrics.incrementMetrics(startTime, errorResponse.httpStatusCode, LisaMetricKeys.TRANSACTION)
                  Future.successful(errorResponse.asResult)
              }
            }
          }
        }
      }
    }

  private val errors: PartialFunction[GetTransactionResponse, ErrorResponse] = {
    case GetTransactionAccountNotFoundResponse => ErrorAccountNotFound
    case GetTransactionServiceUnavailableResponse => ErrorServiceUnavailable
  }

  private def auditGetTransaction(
                                   lisaManager: String,
                                   accountId: String,
                                   transactionId: String,
                                   failureReason: Option[String] = None
                                 )(implicit hc: HeaderCarrier) = {
    val path = getTransactionEndpointUrl(lisaManager, accountId, transactionId)
    val auditData = Map(
      ZREF -> lisaManager,
      "accountId" -> accountId,
      "transactionId" -> transactionId
    )

    failureReason map { reason =>
      auditService.audit(
        auditType = "getTransactionNotReported",
        path = path,
        auditData = auditData ++ Map("reasonNotReported" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "getTransactionReported",
      path = path,
      auditData = auditData
    )
  }

  private def getTransactionEndpointUrl(
                                         lisaManagerReferenceNumber: String,
                                         accountId: String,
                                         transactionId: String
                                       ): String =
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountId/transactions/$transactionId/payments"

}
