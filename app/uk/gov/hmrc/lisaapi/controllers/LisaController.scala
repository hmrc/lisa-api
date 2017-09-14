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
import play.api.libs.json._
import play.api.mvc.{AnyContent, Request, Result}
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.lisaapi.utils.ErrorConverter
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.auth.core.Retrievals.internalId
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.metrics.{LisaMetrics, LisaMetricKeys}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait LisaController extends BaseController with HeaderValidator with RunMode with AuthorisedFunctions {

  val authConnector: LisaAuthConnector = LisaAuthConnector
  lazy val errorConverter: ErrorConverter = ErrorConverter

  protected def withValidLMRN(lisaManager: String)(success: Future[Result])(implicit request: Request[AnyContent]): Future[Result] = {
    if (lisaManager.matches("^Z([0-9]{4}|[0-9]{6})$")) {
      success
    }
    else {
      LisaMetrics.incrementMetrics(System.currentTimeMillis,LisaMetricKeys.getErrorKey(BAD_REQUEST,request.uri))
      Future.successful(BadRequest(toJson(ErrorBadRequestLmrn)))
    }
  }

  protected def withValidJson[T](
                                  success: (T) => Future[Result],
                                  invalid: Option[(Seq[(JsPath, Seq[ValidationError])]) => Future[Result]] = None,
                                  lisaManager: String
                                )(implicit request: Request[AnyContent], reads: Reads[T]): Future[Result] = {

    val startTime = System.currentTimeMillis
    authorised((Enrolment("HMRC-LISA-ORG")).withIdentifier("ZREF", lisaManager)).retrieve(internalId) { id =>
      request.body.asJson match {
        case Some(json) =>
          Try(json.validate[T]) match {
            case Success(JsSuccess(payload, _)) => {
              Try(success(payload)) match {
                case Success(result) => result
                case Failure(ex: Exception) => {
                  Logger.error(s"LisaController An error occurred in Json payload validation ${ex.getMessage}")
                  LisaMetrics.incrementMetrics(System.currentTimeMillis,LisaMetricKeys.getErrorKey(INTERNAL_SERVER_ERROR,request.uri))

                  Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
                }
              }
            }
            case Success(JsError(errors)) => {
              invalid match {
                case Some(invalidCallback) => invalidCallback(errors)
                case None => {
                  LisaMetrics.incrementMetrics(startTime,LisaMetricKeys.getErrorKey(BAD_REQUEST,request.uri))
                  Logger.error(s"The errors are ${errors.toString()}")
                  Future.successful(BadRequest(toJson(ErrorBadRequest(errorConverter.convert(errors)))))
                }
              }
            }
            case Failure(e) => Logger.error(s"LisaController: An error occurred in lisa-api due to ${e.getMessage} returning internal server error")
              LisaMetrics.incrementMetrics(System.currentTimeMillis,LisaMetricKeys.getErrorKey(INTERNAL_SERVER_ERROR,request.uri))
              Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
          }

        case None =>   LisaMetrics.incrementMetrics(startTime,LisaMetricKeys.getErrorKey(BAD_REQUEST,request.uri))
                       Future.successful(BadRequest(toJson(EmptyJson)))
      }

    } recoverWith {
      handleFailure
    }
  }

  def handleFailure(implicit request: Request[_]): PartialFunction[Throwable, Future[Result]] = PartialFunction[Throwable, Future[Result]] {
    case _: InsufficientEnrolments => Logger.warn(s"Unauthorised access for ${request.uri}");Future.successful(Unauthorized(Json.toJson(ErrorInvalidLisaManager)))
    case _: AuthorisationException => Logger.warn(s"Unauthorised Exception for ${request.uri}"); Future.successful(Unauthorized(Json.toJson(ErrorUnauthorized)))
    case _ => LisaMetrics.incrementMetrics(System.currentTimeMillis,LisaMetricKeys.getErrorKey(INTERNAL_SERVER_ERROR,request.uri))
              Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
  }
}



