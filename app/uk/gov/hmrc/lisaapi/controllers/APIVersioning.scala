/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.http.HeaderNames.ACCEPT
import play.api.mvc._
import uk.gov.hmrc.lisaapi.config.AppContext

import scala.concurrent.{ExecutionContext, Future}

trait APIVersioning extends Logging {

  val validateVersion: String => Boolean
  val validateContentType: String => Boolean
  lazy val v2endpointsEnabled: Boolean = appContext.v2endpointsEnabled

  protected def appContext: AppContext

  def isEndpointEnabled(endpoint: String, parse: PlayBodyParsers)(implicit
    ec: ExecutionContext
  ): ActionBuilder[Request, AnyContent] = new ActionBuilder[Request, AnyContent] {
    override def parser: BodyParser[AnyContent]                                                           = parse.defaultBodyParser
    override protected def executionContext: ExecutionContext                                             = ec
    override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] =
      if (appContext.endpointIsDisabled(endpoint)) {
        logger.info(s"[APIVersioning][isEndpointEnabled] User attempted to use an endpoint which is not available ($endpoint)")
        Future.successful(ErrorApiNotAvailable.asResult)
      } else {
        block(request)
      }
  }

  def validateHeader(parse: PlayBodyParsers)(implicit ec: ExecutionContext): ActionBuilder[Request, AnyContent] =
    new ActionBuilder[Request, AnyContent] {
      override def parser: BodyParser[AnyContent]                                           = parse.defaultBodyParser
      override protected def executionContext: ExecutionContext                             = ec
      override def invokeBlock[A](request: Request[A], block: Request[A] => Future[Result]): Future[Result] =
        extractAcceptHeader(request) match {
          case Some(AcceptHeader(version, content)) =>
            val version2NotEnabled = version == "2.0" && !v2endpointsEnabled
            if (version2NotEnabled) {
              logger.info(s"[APIVersioning][validateHeader] Request accept header has invalid version: $version")
              Future.successful(ErrorAcceptHeaderVersionInvalid.asResult)
            } else {
              (validateVersion(version), validateContentType(content)) match {
                case (true, true) => block(request)
                case (false, _)   =>
                  logger.info(s"[APIVersioning][validateHeader] Request accept header has invalid version: $version")
                  Future.successful(ErrorAcceptHeaderVersionInvalid.asResult)
                case (_, false)   =>
                  logger.info(s"[APIVersioning][validateHeader] Request accept header has invalid content type: $content")
                  Future.successful(ErrorAcceptHeaderContentInvalid.asResult)
              }
            }
          case _                                    =>
            logger.info("[APIVersioning][validateHeader] Request accept header is missing or invalid")
            Future.successful(ErrorAcceptHeaderInvalid.asResult)
        }
    }

  def withApiVersion[A](
    pf: PartialFunction[Option[String], Future[Result]]
  )(implicit request: Request[A]): Future[Result] =
    pf.orElse[Option[String], Future[Result]] {
      case Some(version) =>
        logger.info(s"[APIVersioning][withApiVersion] Request accept header has unimplemented version: $version")
        Future.successful(ErrorAcceptHeaderVersionInvalid.asResult)
      case None          =>
        logger.info("[APIVersioning][withApiVersion] Request accept header is missing or invalid")
        Future.successful(ErrorAcceptHeaderInvalid.asResult)
    }(extractAcceptHeader(request).map(_.version))

  def getAPIVersionFromRequest(implicit request: Request[AnyContent]): Option[String] =
    extractAcceptHeader(request).map(header => header.version)

  def extractAcceptHeader[A](req: Request[A]): Option[AcceptHeader] = {
    val versionRegex = """^application/vnd\.hmrc\.(\d\.\d)\+(.*)$""".r
    req.headers.get(ACCEPT).flatMap {
      case versionRegex(version, contentType) => Some(AcceptHeader(version, contentType))
      case _                                  => None
    }
  }

}

case class AcceptHeader(version: String, contentType: String)
