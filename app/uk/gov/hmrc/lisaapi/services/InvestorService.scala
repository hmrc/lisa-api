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

package uk.gov.hmrc.lisaapi.services

import com.google.inject.Inject
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesUnavailableResponse}

import scala.concurrent.{ExecutionContext, Future}

class InvestorService @Inject()(desConnector: DesConnector)(implicit ec: ExecutionContext)  {

  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier) : Future[CreateLisaInvestorResponse] = {
    desConnector.createInvestor(lisaManager, request) map {
      case successResponse: CreateLisaInvestorSuccessResponse => successResponse
      case existsResponse: CreateLisaInvestorAlreadyExistsResponse => existsResponse
      case DesUnavailableResponse => CreateLisaInvestorServiceUnavailableResponse
      case error: DesFailureResponse =>
        error.code match {
          case "INVESTOR_NOT_FOUND" => CreateLisaInvestorInvestorNotFoundResponse
          case _ =>
            Logger.warn(s"Create investor returned error code ${error.code}")
            CreateLisaInvestorErrorResponse
        }
    }
  }

}
