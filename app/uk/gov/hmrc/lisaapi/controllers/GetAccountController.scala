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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, GetLisaAccountSuccessResponse}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class GetAccountController @Inject()(
                                      val authConnector: AuthConnector,
                                      val appContext: AppContext,
                                      service: AccountService,
                                      auditService: AuditService,
                                      val lisaMetrics: LisaMetrics
                                    )
  extends LisaController {

  def getAccountDetails(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader() andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()
      withEnrolment(lisaManager) { (_) =>
        service.getAccount(lisaManager, accountId).map {
          case response: GetLisaAccountSuccessResponse =>
            lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.ACCOUNT)
            Ok(Json.toJson(response))

          case GetLisaAccountDoesNotExistResponse =>
            lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.ACCOUNT)
            NotFound(Json.toJson(ErrorAccountNotFound))

          case _ =>
            lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.ACCOUNT)
            InternalServerError(Json.toJson(ErrorInternalServerError))
        }
      }
    }

}
