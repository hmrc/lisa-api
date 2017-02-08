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

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Request, Result}
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.services.SandboxService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class SandboxController extends LisaController {
  override implicit val hc: HeaderCarrier = HeaderCarrier()
  override lazy val service = SandboxService

  override implicit def baseUrl(implicit request: Request[AnyContent]): String =
    env match {
      case "Test" | "Dev" => s"http://${request.headers.get(HeaderNames.HOST).getOrElse("unknown")}/sandbox"
      case _ => s"https://${AppContext.baseUrl}/${AppContext.apiContext}"
    }

  def withValidAuthHeader(f: Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    request.headers.get(HeaderNames.AUTHORIZATION) match {
      case Some(token) if token.nonEmpty => f
      case Some(token) => Future.successful(Unauthorized(Json.toJson(InvalidAuthorisationHeader)))
      case None => Future.successful(Unauthorized(Json.toJson(MissingAuthorisationHeader)))
    }
  }

  override def availableEndpoints(lisaManager: String): Action[AnyContent] = ???

  override def createTransferLisaAccount(lisaManager: String): Action[AnyContent] = ???

  override def closeLisaAccount(lisaManger: String, accountId: String): Action[AnyContent] = ???

  override def lifeEvent(lisaManager: String, accountId: String): Action[AnyContent] = ???

  override def requestBonus(lisaManager: String, accountId: String): Action[AnyContent] = ???

  override def createLisaInvestor(lisaManager: String) =  validateAccept(acceptHeaderValidationRules).async {
    implicit request =>
      withValidAuthHeader {
      service.createInvestor(lisaManager).map { invest => Ok("done")
    }
    } recover {
      case _ => Status(ErrorInternalServerError.httpStatusCode)(Json.toJson(ErrorInternalServerError))
    }


  }

}
