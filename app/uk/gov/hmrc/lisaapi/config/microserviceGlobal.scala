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

//object MicroserviceAuditFilter extends AuditFilter with AppName with MicroserviceFilterSupport {
//  override val auditConnector = MicroserviceAuditConnector
//
//  override def controllerNeedsAuditing(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsAuditing
//}
//
//object MicroserviceLoggingFilter extends LoggingFilter with MicroserviceFilterSupport {
//  override def controllerNeedsLogging(controllerName: String) = ControllerConfiguration.paramsForController(controllerName).needsLogging
//}

//object MicroserviceGlobal extends DefaultMicroserviceGlobal with RunMode with MicroserviceFilterSupport with ServiceLocatorRegistration with LisaConstants {
//  override lazy val registrationEnabled = AppContext.registrationEnabled
//  override val auditConnector = MicroserviceAuditConnector
//  override val loggingFilter = MicroserviceLoggingFilter
//
//  override val microserviceAuditFilter = MicroserviceAuditFilter
//
//  override val authFilter = None
//
//  override val slConnector: ServiceLocatorConnector = ServiceLocatorConnector
//
//  override implicit val hc: HeaderCarrier = HeaderCarrier()
//
//  override def microserviceMetricsConfig(implicit app: Application): Option[Configuration] = app.configuration.getConfig(s"microservice.metrics")
//
//  override def onError(request: RequestHeader, ex: Throwable): Future[Result] = {
//    super.onError(request, ex) map (res => {
//      res.header.status
//      match {
//        case UNAUTHORIZED => ErrorUnauthorized.asResult
//        case _ => ErrorInternalServerError.asResult
//      }
//    })
//  }
//
//  override def onBadRequest(request: RequestHeader, error: String): Future[Result] = {
//    Future.successful(Status(ErrorGenericBadRequest.httpStatusCode)(Json.toJson(ErrorGenericBadRequest)))
//  }
//
//  override def onHandlerNotFound(request: RequestHeader): Future[Result] = Future.successful(NotFound(Json.toJson(ErrorNotFound)))
//
//}
