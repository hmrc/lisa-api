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

package uk.gov.hmrc.lisaapi.services

import play.api.Logger
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des.{DesLifeEventResponse, DesResponse, DesSuccessResponse}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait LifeEventService {
  val desConnector: DesConnector

  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequest)(implicit hc: HeaderCarrier): Future[ReportLifeEventResponse] = {
    val response = desConnector.reportLifeEvent(lisaManager, accountId, request)

    response map { result =>

      result._1 match {
        case 201 => result._2 match {
          case Some(_) => {
            Logger.debug("DesResponse object returned for status 201")
            ReportLifeEventSuccessResponse(result._2.get.asInstanceOf[DesSuccessResponse].id.toString)
          }
          case None => {
            Logger.debug("Status 201 matched without DesResponse object")
            ReportLifeEventErrorResponse
          }
        }
        case 403 => ReportLifeEventInappropriateResponse
        case 409 => ReportLifeEventAlreadyExistsResponse
        case _ => ReportLifeEventErrorResponse
      }
    } recover {
      case ce: ClassCastException => {
        Logger.debug("Throwing Cast exception error " + ce.getMessage)
        ReportLifeEventErrorResponse
      }
      case e: Exception =>  ReportLifeEventErrorResponse
    }
  }
}

object LifeEventService extends LifeEventService{
  override val desConnector: DesConnector = DesConnector
}
