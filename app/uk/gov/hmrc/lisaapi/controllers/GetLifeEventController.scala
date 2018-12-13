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

import scala.concurrent.Future

class GetLifeEventController @Inject()(
                                        val authConnector: AuthConnector,
                                        val appContext: AppContext)
  extends LisaController2 {

  override val validateVersion: String => Boolean = _ == "2.0"

  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: String): Action[AnyContent] = validateHeader().async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()

    withValidLMRN(lisaManager) { () =>
      withValidAccountId(accountId) { () =>
        withEnrolment(lisaManager) { (_) =>
          Future.successful(NotImplemented(Json.toJson(ErrorNotImplemented)))
        }
      }
    }
  }

}
