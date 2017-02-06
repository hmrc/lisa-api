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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.lisaapi.services.{LisaService, SandboxService}
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait LisaController extends BaseController with HeaderValidator {
  implicit val hc: HeaderCarrier
  lazy val service: LisaService = ???

  def createLisaInvestor(investor: String) =  validateAccept(acceptHeaderValidationRules).async {
    service.createInvestor(investor).map { invest => Ok("done")
    } recover {
      case _ => Status(ErrorInternalServerError.httpStatusCode)(Json.toJson(ErrorInternalServerError))
    }
  }
}

object SandboxController extends LisaController {
  override implicit val hc: HeaderCarrier = HeaderCarrier()
  override lazy val service = SandboxService
}

