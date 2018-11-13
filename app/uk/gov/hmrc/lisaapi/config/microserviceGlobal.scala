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

package uk.gov.hmrc.lisaapi.config

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._
import play.api._
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results.{NotFound, Status}
import play.api.mvc.{Handler, RequestHeader, Result}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.LisaConstants
import uk.gov.hmrc.lisaapi.connectors.ServiceLocatorConnector
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.microservice.bootstrap.DefaultMicroserviceGlobal
import uk.gov.hmrc.play.microservice.filters.{AuditFilter, LoggingFilter, MicroserviceFilterSupport}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait ServiceLocatorRegistration extends GlobalSettings with RunMode {

  val registrationEnabled: Boolean
  val slConnector: ServiceLocatorConnector
  implicit val hc: HeaderCarrier

  override def onStart(app: Application): Unit = {
    super.onStart(app)
    registrationEnabled match {
      case true => {
        Logger.info("Starting Registration"); slConnector.register
      }
      case false => Logger.warn("Registration in Service Locator is disabled")
    }
  }
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}


object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
  override val auditConnector = MicroserviceAuditConnector

  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode with MicroserviceFilterSupport with ServiceLocatorRegistration with LisaConstants {
  override lazy val registrationEnabled = AppContext.registrationEnabled
  override val auditConnector = MicroserviceAuditConnector
  override val loggingFilter = MicroserviceLoggingFilter

  override val microserviceAuditFilter = MicroserviceAuditFilter

  override val authFilter = None

  override val slConnector: ServiceLocatorConnector = ServiceLocatorConnector

  override implicit val hc: HeaderCarrier = HeaderCarrier()

  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")

  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
    super.onError(request, ex) map (res => {
      res.header.status
      match {
        case UNAUTHORIZED => ErrorUnauthorized.asResult
        case _ => ErrorInternalServerError.asResult
      }
    })
  }

  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
    Future.successful(Status(ErrorGenericBadRequest.httpStatusCode)(Json.toJson(ErrorGenericBadRequest)))
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = Future.successful(NotFound(Json.toJson(ErrorNotFound)))

  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    val versionRegex = """application\/vnd.hmrc.(\d.\d)\+json""".r
    val version = request.headers.get("Accept") collect { case versionRegex(version) => version }
    version match {
      case Some(VERSION_1) =>
        v1.Routes.routes.lift(request)
      case Some(VERSION_2) =>
        v2.Routes.routes.lift(request)
      case _ =>
        super.onRouteRequest(request)
    }
  }

}
