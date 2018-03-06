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
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetTransactionAccountNotFoundResponse, GetTransactionErrorResponse, GetTransactionSuccessResponse, GetTransactionTransactionNotFoundResponse}
import uk.gov.hmrc.lisaapi.services.TransactionService

import scala.concurrent.ExecutionContext.Implicits.global

class TransactionController extends LisaController with LisaConstants {

  val service: TransactionService = TransactionService

  def getTransaction(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateAccept(acceptHeaderValidationRules).async { implicit request =>
      implicit val startTime = System.currentTimeMillis()
      LisaMetrics.startMetrics(startTime, LisaMetricKeys.TRANSACTION)

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            service.getTransaction(lisaManager, accountId, transactionId) map {
              case success: GetTransactionSuccessResponse => {
                Logger.debug("Matched Valid Response")

                LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.TRANSACTION)

                Ok(Json.toJson(success))
              }
              case GetTransactionAccountNotFoundResponse => {
                Logger.debug("Matched Not Found Response")

                LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(NOT_FOUND, request.uri))

                NotFound(Json.toJson(ErrorAccountNotFound))
              }
              case GetTransactionTransactionNotFoundResponse => {
                Logger.debug("Matched Not Found Response")

                LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(NOT_FOUND, request.uri))

                NotFound(Json.toJson(ErrorTransactionNotFound))
              }
              case GetTransactionErrorResponse => {
                Logger.debug("Matched an error")

                LisaMetrics.incrementMetrics(startTime, LisaMetricKeys.getMetricKey(INTERNAL_SERVER_ERROR, request.uri))

                InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            }
          }
        }
      }
    }

}
