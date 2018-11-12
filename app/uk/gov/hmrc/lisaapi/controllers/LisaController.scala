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

import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.Json.toJson
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.api.controllers.HeaderValidator
import uk.gov.hmrc.auth.core.retrieve.Retrievals.internalId
import uk.gov.hmrc.auth.core.{AuthorisationException, AuthorisedFunctions, Enrolment, InsufficientEnrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.utils.ErrorConverter
import uk.gov.hmrc.play.config.RunMode
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._
import uk.gov.hmrc.play.microservice.controller.BaseController

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait LisaController extends BaseController with LisaConstants with HeaderValidator with RunMode with AuthorisedFunctions {

  override val validateVersion: String => Boolean = str => str == "1.0" || str == "2.0"
  val authConnector: LisaAuthConnector = LisaAuthConnector
  lazy val errorConverter: ErrorConverter = ErrorConverter

  protected def withValidLMRN(lisaManager: String)(success: () => Future[Result])(implicit request: Request[AnyContent], startTime: Long): Future[Result] = {
    if (lisaManager.matches("^Z([0-9]{4}|[0-9]{6})$")) {
      success()
    }
    else {
      LisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(BadRequest(toJson(ErrorBadRequestLmrn)))
    }
  }

  protected def withValidAccountId(accountId: String)(success: () => Future[Result])(implicit request: Request[AnyContent], startTime: Long): Future[Result] = {
    if (accountId.matches("^[a-zA-Z0-9 :/-]{1,20}$")) {
      success()
    }
    else {
      LisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(BadRequest(toJson(ErrorBadRequestAccountId)))
    }
  }

  protected def withEnrolment(lisaManager: String)
                             (callback: (Option[String]) => Future[Result])
                             (implicit request: Request[AnyContent], startTime: Long): Future[Result] = {
    authorised(Enrolment("HMRC-LISA-ORG").withIdentifier("ZREF", lisaManager)).retrieve(internalId) {id =>
      callback(id)
    } recoverWith {
      handleFailure
    }
  }

  protected def withValidJson[T](
                                  success: (T) => Future[Result],
                                  invalid: Option[(Seq[(JsPath, Seq[ValidationError])]) => Future[Result]] = None,
                                  lisaManager: String
                                )(implicit request: Request[AnyContent], reads: Reads[T], startTime: Long): Future[Result] = {

    withEnrolment(lisaManager) { _ =>
      request.body.asJson match {
        case Some(json) =>
          Try(json.validate[T]) match {
            case Success(JsSuccess(payload, _)) => {
              Try(success(payload)) match {
                case Success(result) => result
                case Failure(ex: Exception) => {
                  Logger.error(s"LisaController An error occurred in Json payload validation ${ex.getMessage}")
                  LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.getMetricKey(request.uri))

                  Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
                }
              }
            }
            case Success(JsError(errors)) => {
              invalid match {
                case Some(invalidCallback) => invalidCallback(errors)
                case None => {
                  LisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
                  Logger.error(s"The errors are ${errors.toString()}")
                  Future.successful(BadRequest(toJson(ErrorBadRequest(errorConverter.convert(errors)))))
                }
              }
            }
            case Failure(e) =>
              Logger.error(s"LisaController: An error occurred in lisa-api due to ${e.getMessage} returning internal server error")
              LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.getMetricKey(request.uri))
              Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
          }

        case None =>
          LisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
          Future.successful(BadRequest(toJson(EmptyJson)))
      }
    }
  }

  def handleFailure(implicit request: Request[_], startTime: Long): PartialFunction[Throwable, Future[Result]] = PartialFunction[Throwable, Future[Result]] {
    case _: InsufficientEnrolments =>
      Logger.warn(s"Unauthorised access for ${request.uri}")
      LisaMetrics.incrementMetrics(startTime, UNAUTHORIZED, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(Unauthorized(Json.toJson(ErrorInvalidLisaManager)))
    case _: AuthorisationException =>
      Logger.warn(s"Unauthorised Exception for ${request.uri}")
      LisaMetrics.incrementMetrics(startTime, UNAUTHORIZED, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(Unauthorized(Json.toJson(ErrorUnauthorized)))
    case _ =>
      LisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(InternalServerError(toJson(ErrorInternalServerError)))
  }

  def todo(id: String,accountId:String,transactionId:String): Action[AnyContent] = Action.async { _ =>
    Future.successful(NotImplemented(Json.toJson(ErrorNotImplemented)))
  }

  private[controllers] def withApiVersion(pf: PartialFunction[Option[String], Future[Result]])
                                      (implicit request: Request[AnyContent]): Future[Result] = {
    pf.orElse[Option[String], Future[Result]]{
      case Some(_) =>
        Logger.info("request header contains an unsupported api version")
        Future.successful(NotFound(Json.toJson(ErrorNotFound)))
      case None =>
        Logger.info("request header contains an incorrect or empty api version")
        Future.successful(NotAcceptable(Json.toJson(ErrorAcceptHeaderInvalid)))
    }(getAPIVersionFromRequest)
  }

  private[controllers] def getAPIVersionFromRequest(implicit request: Request[AnyContent]): Option[String] = {
    val reg = """application\/vnd\.hmrc\.(\d.\d)\+json""".r
    request.headers.get(ACCEPT).flatMap(value => Option(value) collect { case reg(value) => value })
  }
}



