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
import play.api.data.validation.ValidationError
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsError, JsPath, JsSuccess, Reads}
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.lisaapi.utils.ErrorConverter
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.auth.core.Retrievals._
import uk.gov.hmrc.auth.core.{AuthorisedFunctions, Enrolment}
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait LisaController extends BaseController with HeaderValidator with RunMode  with AuthorisedFunctions {

  val authConnector: LisaAuthConnector = LisaAuthConnector
  lazy val errorConverter: ErrorConverter = ErrorConverter

  protected def withValidJson[T](
                                  success: (T) => Future[Result],
                                  invalid: Option[(Seq[(JsPath, Seq[ValidationError])]) => Future[Result]] = None,
                                  lisaManager: String
                                )(implicit request: Request[AnyContent], reads: Reads[T]): Future[Result] = {
    authorised((Enrolment("HMRC-LISA-ORG")).withIdentifier("ZREF", lisaManager)).retrieve(internalId) { id =>

      request.body.asJson match {
        case Some(json) =>
          Try(json.validate[T]) match {
            case Success(JsSuccess(payload, _)) => {

              Try(success(payload)) match {
                case Success(result) => result
                case Failure(ex: Exception) => {
                  Logger.error(s"LisaController An error occurred in Json payload validation ${ex.getMessage}")
                  Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
                }
              }
            }
            case Success(JsError(errors)) => {
              invalid match {
                case Some(invalidCallback) => invalidCallback(errors)
                case None => {
                  Logger.error(s"The errors are ${errors.toString()}")
                  Future.successful(BadRequest(toJson(ErrorBadRequest(errorConverter.convert(errors)))))
                }
              }
            }
            case Failure(e) => Logger.error(s"LisaController: An error occurred in lisa-api due to ${e.getMessage} returning internal server error")
              Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
          }
        case None => Future.successful(BadRequest(toJson(EmptyJson)))
      }

    }
  }
}



