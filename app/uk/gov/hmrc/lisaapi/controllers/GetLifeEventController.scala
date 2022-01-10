/*
 * Copyright 2022 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}

import scala.concurrent.ExecutionContext

class GetLifeEventController @Inject()(
                                        authConnector: AuthConnector,
                                        appContext: AppContext,
                                        lisaMetrics: LisaMetrics,
                                        val service: LifeEventService,
                                        auditService: AuditService,
                                        cc: ControllerComponents,
                                        parse: PlayBodyParsers
                                      )(implicit ec: ExecutionContext) extends LisaController(
  cc: ControllerComponents,
  lisaMetrics: LisaMetrics,
  appContext: AppContext,
  authConnector: AuthConnector
) {

  override val validateVersion: String => Boolean = _ == "2.0"

  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: String): Action[AnyContent] = validateHeader(parse).async { implicit request =>
    implicit val startTime: Long = System.currentTimeMillis()

    withValidLMRN(lisaManager) { () =>
      withValidAccountId(accountId) { () =>
        withEnrolment(lisaManager) { _ =>
          service.getLifeEvent(lisaManager, accountId, lifeEventId) map {
            case Left(error) =>
              auditGetLifeEvent(lisaManager, accountId, lifeEventId, Some(error.errorCode))
              lisaMetrics.incrementMetrics(startTime, error.httpStatusCode, LisaMetricKeys.EVENT)
              error.asResult
            case Right(success) =>
              auditGetLifeEvent(lisaManager, accountId, lifeEventId)
              lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.EVENT)
              Ok(Json.toJson(success))
          }
        }
      }
    }
  }

  private def auditGetLifeEvent(lisaManager: String, accountId: String, lifeEventId: String, failureReason: Option[String] = None)
                                  (implicit hc: HeaderCarrier) = {
    val path = getLifeEventEndpointUrl(lisaManager, accountId, lifeEventId)
    val auditData = Map(
      ZREF -> lisaManager,
      "accountId" -> accountId,
      "lifeEventId" -> lifeEventId
    )

    failureReason map { reason =>
      auditService.audit(
        auditType = "getLifeEventNotReported",
        path = path,
        auditData = auditData ++ Map("reasonNotReported" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "getLifeEventReported",
      path = path,
      auditData = auditData
    )
  }

  private def getLifeEventEndpointUrl(lisaManagerReferenceNumber: String, accountId: String, lifeEventId: String): String =
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountId/events/$lifeEventId"

}