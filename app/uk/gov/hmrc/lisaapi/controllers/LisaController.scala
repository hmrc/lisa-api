/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.Logging
import play.api.libs.json._
import play.api.mvc._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.internalId
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.utils.ErrorConverter
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

abstract case class LisaController(
                                    cc: ControllerComponents,
                                    lisaMetrics: LisaMetrics,
                                    appContext: AppContext,
                                    authConnector: AuthConnector
                                  ) extends BackendController(cc: ControllerComponents) with LisaConstants
  with AuthorisedFunctions with APIVersioning with LisaActions with Logging {

  override val validateVersion: String => Boolean = str => str == "1.0" || str == "2.0"
  override val validateContentType: String => Boolean = _ == "json"
  lazy val errorConverter: ErrorConverter = ErrorConverter

  protected def withValidLMRN(lisaManager: String)(success: () => Future[Result])(implicit request: Request[AnyContent], startTime: Long): Future[Result] = {
    if (lisaManager.matches("^Z([0-9]{4}|[0-9]{6})$")) {
      success()
    } else {
      lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(BadRequest(ErrorBadRequestLmrn.asJson))
    }
  }

  protected def withValidAccountId(accountId: String)(success: () => Future[Result])(implicit request: Request[AnyContent], startTime: Long): Future[Result] = {
    if (accountId.matches("^[a-zA-Z0-9 :/-]{1,20}$")) {
      success()
    } else {
      lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(BadRequest(ErrorBadRequestAccountId.asJson))
    }
  }

  protected def withValidTransactionId(transactionId: String)(success: () => Future[Result])(implicit request: Request[AnyContent], startTime: Long): Future[Result] = {
    if (transactionId.matches("^[0-9]{1,10}$")) {
      success()
    } else {
      lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(BadRequest(ErrorBadRequestTransactionId.asJson))
    }
  }

  protected def withEnrolment(lisaManager: String)
                             (callback: Option[String] => Future[Result])
                             (implicit request: Request[AnyContent], startTime: Long, ec: ExecutionContext): Future[Result] = {
    authorised(Enrolment("HMRC-LISA-ORG").withIdentifier("ZREF", lisaManager)).retrieve(internalId) {id =>
      callback(id)
    } recoverWith {
      handleFailure
    }
  }

  protected def withValidJson[T](
                                  success: T => Future[Result],
                                  invalid: Option[Seq[(JsPath, Seq[JsonValidationError])] => Future[Result]] = None,
                                  lisaManager: String
                                )(implicit request: Request[AnyContent], reads: Reads[T], startTime: Long, ec: ExecutionContext): Future[Result] = {

    withEnrolment(lisaManager) { _ =>
      request.body.asJson match {
        case Some(json) =>
          Try(json.validate[T]) match {
            case Success(JsSuccess(payload, _)) =>
              Try(success(payload)) match {
                case Success(result) => result
                case Failure(ex: Exception) =>
                  logger.error(s"LisaController An error occurred in Json payload validation ${ex.getMessage}")
                  lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.getMetricKey(request.uri))
                  Future.successful(InternalServerError(ErrorInternalServerError.asJson))
              }
            case Success(JsError(errors)) =>
              invalid map { _(errors)} getOrElse {
                lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
                logger.warn(s"[LisaController][withValidJson] The errors are ${errorConverter.convert(errors)}")
                Future.successful(BadRequest(ErrorBadRequest(errorConverter.convert(errors)).asJson))
              }
            case Failure(e) =>
              logger.error(s"LisaController: An error occurred in lisa-api due to ${e.getMessage} returning internal server error")
              lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.getMetricKey(request.uri))
              Future.successful(InternalServerError(ErrorInternalServerError.asJson))
          }

        case None =>
          lisaMetrics.incrementMetrics(startTime, BAD_REQUEST, LisaMetricKeys.getMetricKey(request.uri))
          Future.successful(BadRequest(EmptyJson.asJson))
      }
    }
  }

  def handleFailure(implicit request: Request[_], startTime: Long): PartialFunction[Throwable, Future[Result]] = {
    case _: InsufficientEnrolments =>
      logger.warn(s"Unauthorised access for ${request.uri}")
      lisaMetrics.incrementMetrics(startTime, UNAUTHORIZED, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(Unauthorized(ErrorInvalidLisaManager.asJson))
    case _: AuthorisationException =>
      logger.warn(s"Unauthorised Exception for ${request.uri}")
      lisaMetrics.incrementMetrics(startTime, UNAUTHORIZED, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(Unauthorized(ErrorUnauthorized.asJson))
    case _ =>
      lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.getMetricKey(request.uri))
      Future.successful(InternalServerError(ErrorInternalServerError.asJson))
  }
}



