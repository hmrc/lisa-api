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

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


object SandboxService extends LisaService {

  override def createInvestor(lisaManager: String, lisaInvestor: LisaInvestor)(implicit hc: HeaderCarrier) = {
    Future(Json.parse(
      s"""{
         |  "data": {
         |    "investorID": "9876543210",
         |    "message": "Investor Created."
         |  },
         |  "success": true,
         |  "status": 201
         |}""".stripMargin
    ))
  }

  override def availableEndpoints(lisaManager: String): Future[JsValue] = {
    Future(Json.parse(
      s"""{
       "lisaManagerReferenceNumber" : "${lisaManager}",
        "lisaProviderName" : "Example Building Society",
        "_links" :
          {
            "self" : {"href" : "/lifetime-isa/manager/{LISAManagerReferenceNumber}"},
            "investors" : {"href" : "/lifetime-isa/manager/{lisaManagerReferenceNumber}/investors"},
            "accounts" : {"href" : " /lifetime-isa/manager/{lisaManagerReferenceNumber}/accounts"},
            "update Account" : {"href" : " /lifetime-isa/manager/{lisaManagerReferenceNumber}/accounts/{accountID}"},
            "life events" : {"href" : " /lifetime-isa/manager/{lisaManagerReferenceNumber}/accounts/{accountID}/events"},
            "transactions" : {"href" : " /lifetime-isa/manager/{lisaManagerReferenceNumber}/accounts/{accountID}/transactions"}
          }
      }"""
    ))
  }

  override def createTransferAccount(lisaManager: String, lisaAccount: LisaAccount): Future[JsValue] = {
    Future(Json.parse(
      s"""{{
         |  "data": {
         |    "accountId": "87654321",
         |    "message": "Investor Account Created Late Notification"
         |  },
         |  "success": true,
         |  "status": 201
         |}""".stripMargin
    ))
  }
}
