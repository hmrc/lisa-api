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
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesTransactionResponse}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BonusPaymentService {
  val desConnector: DesConnector

  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier): Future[RequestBonusPaymentResponse] = {
    val response = desConnector.requestBonusPayment(lisaManager, accountId, request)

    response map {
      case (_, successResponse: DesTransactionResponse) => RequestBonusPaymentSuccessResponse(successResponse.transactionId)
      case (status: Int, errorResponse: DesFailureResponse) => RequestBonusPaymentErrorResponse(status, errorResponse)
    }
  }

}

object BonusPaymentService extends BonusPaymentService {
  override val desConnector: DesConnector = DesConnector
}
