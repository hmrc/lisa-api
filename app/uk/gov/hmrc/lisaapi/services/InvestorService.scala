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
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global

trait InvestorService  {
  val desConnector: DesConnector

  val INVESTOR_NOT_FOUND = 63214
  val INVESTOR_ALREADY_EXISTS = 63215

  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaInvestorResponse] = {
    val response = desConnector.createInvestor(lisaManager, request)
    val httpStatusOk = 200

    response map {
      case (`httpStatusOk`, Some(data)) => {
        (data.rdsCode, data.investorId) match {
          case (None, Some(investorId)) => CreateLisaInvestorSuccessResponse(investorId)
          case (Some(INVESTOR_NOT_FOUND), _) => CreateLisaInvestorNotFoundResponse
          case (Some(INVESTOR_ALREADY_EXISTS), _) => CreateLisaInvestorAlreadyExistsResponse
          case (_, _) => CreateLisaInvestorErrorResponse
        }
      }
      case (_, _) => CreateLisaInvestorErrorResponse
    }
  }

}

object InvestorService extends InvestorService {
  override val desConnector: DesConnector = DesConnector
}