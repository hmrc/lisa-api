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
import play.api.http.{HttpErrorHandler, LazyHttpErrorHandler}
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.api.controllers.DocumentationController
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.config.APIAccessConfig
import uk.gov.hmrc.lisaapi.domain.APIAccess
import uk.gov.hmrc.lisaapi.views._

class Documentation @Inject()(httpErrorHandler: HttpErrorHandler) extends DocumentationController(httpErrorHandler) {

  override def documentation(version: String, endpointName: String): Action[AnyContent] = {
    at(s"/public/api/documentation/$version", s"${endpointName.replaceAll(" ", "-")}.xml")
  }

  override def definition(): Action[AnyContent] = Action {
    Ok(txt.definition(
      AppContext.apiContext,
      AppContext.v1apiStatus,
      AppContext.v2apiStatus,
      buildAccess(),
      AppContext.v1endpointsEnabled,
      AppContext.v2endpointsEnabled
    ))
  }

  def raml(version: String, file: String): Action[AnyContent] = {
    at(s"/public/api/conf/$version", file)
  }

  private def buildAccess() = {
    val access = APIAccessConfig(AppContext.access)
    APIAccess(access.accessType, access.whiteListedApplicationIds)
  }
}

object Documentation extends Documentation(LazyHttpErrorHandler)
