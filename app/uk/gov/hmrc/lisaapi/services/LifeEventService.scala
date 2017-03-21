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

import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models.des.DesResponse
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


trait LifeEventService {
  val desConnector: DesConnector
  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequest)(implicit hc: HeaderCarrier) : Future[ReportLifeEventResponse] = {
    val response = desConnector.reportLifeEvent(lisaManager, accountId, request)

    response map {result =>
      result._2 match {
        case Some(_) =>
              result._1 match {
                case 201 => ReportLifeEventSuccessResponse
              }
        case None => ReportLifeEventErrorResponse
      }
    }
  }
}

object LifeEventService extends LifeEventService{
  override val desConnector: DesConnector = DesConnector
}
