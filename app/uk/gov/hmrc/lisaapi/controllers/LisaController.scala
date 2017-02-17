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

import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait LisaController extends BaseController with HeaderValidator with RunMode {
  implicit val hc: HeaderCarrier

  implicit def baseUrl(implicit request: Request[AnyContent]): String = env match {
    case "Dev" => s"http://${request.headers.get(HeaderNames.HOST).getOrElse("unknown")}"
    case _ => s"https://${AppContext.baseUrl}/${AppContext.apiContext}"
  }

  protected def withValidJson[T](f: (T) => Future[Result])(implicit request: Request[AnyContent], reads: Reads[T]): Future[Result] =
    request.body.asJson match {
      case Some(json) =>
        Try(json.validate[T]) match {
          case Success(JsSuccess(payload, _)) => f(payload)
          case Success(JsError(errs)) => {
            Logger.info("The errors are " + errs.toString())
            Future.successful(BadRequest(toJson(MissingID)))
          }

          case Failure(e) => Future.successful(BadRequest(toJson(InvalidID)))
        }
      case None =>
        Future.successful(BadRequest(toJson(EmptyJson)))
    }

}



