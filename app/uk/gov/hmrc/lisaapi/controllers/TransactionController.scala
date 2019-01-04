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

import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.TransactionService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class TransactionController extends LisaController with LisaConstants {

  val service: TransactionService = TransactionService

  def getTransaction(lisaManager: String, accountId: String, transactionId: String): Action[AnyContent] =
    validateHeader().async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withValidLMRN(lisaManager) { () =>
        withEnrolment(lisaManager) { (_) =>
          withValidAccountId(accountId) { () =>
            service.getTransaction(lisaManager, accountId, transactionId) flatMap {
              case success: GetTransactionSuccessResponse => {
                Logger.debug("Matched Valid Response")

                LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.TRANSACTION)

                withApiVersion {
                  case Some(VERSION_1) => Future.successful(Ok(Json.toJson(success.copy(transactionType = None, supersededBy = None))))
                  case Some(VERSION_2) => Future.successful(Ok(Json.toJson(success.copy(bonusDueForPeriod = None))))
                }
              }
              case GetTransactionTransactionNotFoundResponse => {
                Logger.debug("Matched Not Found Response")

                LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.TRANSACTION)

                withApiVersion {
                  case Some(VERSION_1) => Future.successful(ErrorBonusPaymentTransactionNotFound.asResult)
                  case Some(VERSION_2) => Future.successful(ErrorTransactionNotFound.asResult)
                }
              }
              case res: GetTransactionResponse => {
                Logger.debug("Matched an error")

                val errors = Map[GetTransactionResponse, ErrorResponse] (
                  GetTransactionAccountNotFoundResponse -> ErrorAccountNotFound,
                  GetTransactionServiceUnavailableResponse -> ErrorServiceUnavailable
                )
                val error = errors.getOrElse(res, ErrorInternalServerError)

                LisaMetrics.incrementMetrics(startTime, error.httpStatusCode, LisaMetricKeys.TRANSACTION)

                Future.successful(error.asResult)
              }
            }
          }
        }
      }
    }

}
