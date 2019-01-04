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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, GetLisaAccountServiceUnavailable, GetLisaAccountSuccessResponse}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class GetAccountController extends LisaController with LisaConstants {

  val service: AccountService = AccountService
  val auditService: AuditService = AuditService

  def getAccountDetails(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader() andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withEnrolment(lisaManager) { (_) =>
        service.getAccount(lisaManager, accountId).map {
          case response: GetLisaAccountSuccessResponse =>
            LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.ACCOUNT)
            Ok(Json.toJson(response))

          case GetLisaAccountDoesNotExistResponse =>
            LisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.ACCOUNT)
            NotFound(Json.toJson(ErrorAccountNotFound))

          case GetLisaAccountServiceUnavailable =>
            LisaMetrics.incrementMetrics(startTime, SERVICE_UNAVAILABLE, LisaMetricKeys.ACCOUNT)
            ServiceUnavailable(Json.toJson(ErrorServiceUnavailable))

          case _ =>
            LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.ACCOUNT)
            InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    }

}
