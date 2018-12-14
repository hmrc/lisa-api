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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.services.LifeEventService

import scala.concurrent.ExecutionContext.Implicits.global

class GetLifeEventController extends LisaController with LisaConstants {

  override val validateVersion: String => Boolean = _ == "2.0"

  val service: LifeEventService = LifeEventService

  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: String): Action[AnyContent] = validateHeader().async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()

    withValidLMRN(lisaManager) { () =>
      withValidAccountId(accountId) { () =>
        withEnrolment(lisaManager) { (_) =>
          service.getLifeEvent(lisaManager, accountId, lifeEventId) map {
            case Left(error) => {
              LisaMetrics.incrementMetrics(startTime, error.httpStatusCode, LisaMetricKeys.EVENT)
              error.asResult
            }
            case Right(success) => {
              LisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.EVENT)
              Ok(Json.toJson(success))
            }
          }
        }
      }
    }
  }

}