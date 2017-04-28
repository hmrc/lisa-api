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

package uk.gov.hmrc.lisaapi.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class DiscoverController extends LisaController {


  def discover(lisaManagerReferenceNumber: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    val result = s"""{
      "lisaManagerReferenceNumber" : "${lisaManagerReferenceNumber}",
      "_links" :
        {
          "self" : {"href" : "/lifetime-isa/manager/${lisaManagerReferenceNumber}"},
          "investors" : {"href" : "/lifetime-isa/manager/${lisaManagerReferenceNumber}/investors"},
          "create or transfer Account" : {"href" : "/lifetime-isa/manager/${lisaManagerReferenceNumber}/accounts"},
          "close Account" : {"href" : "/lifetime-isa/manager/${lisaManagerReferenceNumber}/accounts/{accountId}"},
          "life events" : {"href" : "/lifetime-isa/manager/${lisaManagerReferenceNumber}/accounts/{accountId}/events"},
          "bonus payments" : {"href" : "/lifetime-isa/manager/${lisaManagerReferenceNumber}/accounts/{accountId}/transactions"}
        }
    }"""

    Future.successful(Ok(Json.parse(result)))
  }

}