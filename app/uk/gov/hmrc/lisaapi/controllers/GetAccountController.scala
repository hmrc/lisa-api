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

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}
import uk.gov.hmrc.lisaapi.models.{GetLisaAccountDoesNotExistResponse, GetLisaAccountServiceUnavailable, GetLisaAccountSuccessResponse}
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}

import scala.concurrent.ExecutionContext

class GetAccountController @Inject() (
  authConnector: AuthConnector,
  appContext: AppContext,
  service: AccountService,
  auditService: AuditService,
  lisaMetrics: LisaMetrics,
  cc: ControllerComponents,
  parse: PlayBodyParsers
)(implicit ec: ExecutionContext)
    extends LisaController(
      cc: ControllerComponents,
      lisaMetrics: LisaMetrics,
      appContext: AppContext,
      authConnector: AuthConnector
    ) {

  def getAccountDetails(lisaManager: String, accountId: String): Action[AnyContent] =
    (validateHeader(parse) andThen validateLMRN(lisaManager) andThen validateAccountId(accountId)).async {
      implicit request =>
        implicit val startTime: Long = System.currentTimeMillis()
        logger.info(
          s"""[GetAccountController][getAccountDetails] started lisaManager : $lisaManager ,
             | accountId : $accountId""".stripMargin)
        withEnrolment(lisaManager) { _ =>
          service.getAccount(lisaManager, accountId).map {
            case response: GetLisaAccountSuccessResponse =>
              auditGetAccount(lisaManager, accountId)
              logger.info(s"""[GetAccountController][getAccountDetails] GetLisaAccountSuccessResponse lisaManager : $lisaManager , accountId : $accountId""")
              lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.ACCOUNT)
              Ok(Json.toJson(response))
            case GetLisaAccountDoesNotExistResponse      =>
              auditGetAccount(lisaManager, accountId, Some(ErrorAccountNotFound.errorCode))
              lisaMetrics.incrementMetrics(startTime, NOT_FOUND, LisaMetricKeys.ACCOUNT)
              logger.error(
                s"""[GetAccountController][getAccountDetails] GetLisaAccountDoesNotExistResponse lisaManager : $lisaManager ,
                   | accountId : $accountId , error : ${ErrorAccountNotFound.asJson}""".stripMargin)
              NotFound(ErrorAccountNotFound.asJson)
            case GetLisaAccountServiceUnavailable        =>
              auditGetAccount(lisaManager, accountId, Some(ErrorServiceUnavailable.errorCode))
              lisaMetrics.incrementMetrics(startTime, SERVICE_UNAVAILABLE, LisaMetricKeys.ACCOUNT)
              logger.error(
                s"""[GetAccountController][getAccountDetails] GetLisaAccountServiceUnavailable lisaManager : $lisaManager ,
                   | accountId : $accountId , error : ${ErrorServiceUnavailable.asJson}""".stripMargin)
              ServiceUnavailable(ErrorServiceUnavailable.asJson)
            case _                                       =>
              auditGetAccount(lisaManager, accountId, Some(ErrorInternalServerError.errorCode))
              lisaMetrics.incrementMetrics(startTime, INTERNAL_SERVER_ERROR, LisaMetricKeys.ACCOUNT)
              logger.error(
                s"""[GetAccountController][getAccountDetails] ErrorInternalServerError lisaManager : $lisaManager ,
                   | accountId : $accountId , error : ${ErrorInternalServerError.asJson}""".stripMargin)
              InternalServerError(ErrorInternalServerError.asJson)
          }
        }
    }

  private def auditGetAccount(lisaManager: String, accountId: String, failureReason: Option[String] = None)(implicit
    hc: HeaderCarrier
  ) = {
    val path      = getAccountEndpointUrl(lisaManager, accountId)
    val auditData = Map(ZREF -> lisaManager, "accountId" -> accountId)

    failureReason map { reason =>
      auditService.audit(
        auditType = "getAccountNotReported",
        path = path,
        auditData = auditData ++ Map("reasonNotReported" -> reason)
      )
    } getOrElse auditService.audit(
      auditType = "getAccountReported",
      path = path,
      auditData = auditData
    )
  }

  private def getAccountEndpointUrl(lisaManagerReferenceNumber: String, accountId: String): String =
    s"/manager/$lisaManagerReferenceNumber/accounts/$accountId"

}
