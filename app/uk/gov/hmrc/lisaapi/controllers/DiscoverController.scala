/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.{LisaMetricKeys, LisaMetrics}

import scala.concurrent.{ExecutionContext, Future}

class DiscoverController @Inject()(
                                    val authConnector: AuthConnector,
                                    val appContext: AppContext,
                                    val lisaMetrics: LisaMetrics)
                                  (implicit ec: ExecutionContext)
  extends LisaController {

  def discover(lisaManagerReferenceNumber: String): Action[AnyContent] =
    (validateHeader andThen
      validateLMRN(lisaManagerReferenceNumber)).async { implicit request =>
      implicit val startTime: Long = System.currentTimeMillis()

      withEnrolment(lisaManagerReferenceNumber) { (_) =>
        val result = withApiVersion {
          case Some(VERSION_1) => Future.successful(Ok(Json.parse(v1(lisaManagerReferenceNumber))))
          case Some(VERSION_2) => Future.successful(Ok(Json.parse(v2(lisaManagerReferenceNumber))))
        }

        lisaMetrics.incrementMetrics(startTime, OK, LisaMetricKeys.DISCOVER)
        result
      }
    }

  private val v1: String => String = (lisaManagerReferenceNumber: String) =>
    s"""{
    "lisaManagerReferenceNumber": "$lisaManagerReferenceNumber",
    "_links":
    {
      "self": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber", "methods": ["GET"]},
      "investors": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/investors", "methods": ["POST"]},
      "accounts": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts", "methods": ["POST"]},
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}", "methods": ["GET"]}
      ],
      "close account": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/close-account", "methods": ["POST"]},
      "life events": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events", "methods": ["POST"]}
      ],
      "bonus payments": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/transactions", "methods": ["POST"]},
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/transactions/{transactionId}", "methods": ["GET"]}
      ],
      "update subscription": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/update-subscription", "methods": ["POST"]},
      "reinstate account": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/reinstate-account", "methods": ["POST"]},
      "bulk payments": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/payments?startDate={startDate}&endDate={endDate}", "methods": ["GET"]},
      "bulk payment breakdown": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/transactions/{transactionId}/payments", "methods": ["GET"]}
    }
  }"""
  private val v2: String => String = (lisaManagerReferenceNumber: String) =>
    s"""{
    "lisaManagerReferenceNumber": "$lisaManagerReferenceNumber",
    "_links":
    {
      "self": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber", "methods": ["GET"]},
      "investors": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/investors", "methods": ["POST"]},
      "accounts": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts", "methods": ["POST"]},
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}", "methods": ["GET"]}
      ],
      "close account": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/close-account", "methods": ["POST"]},
      "reinstate account": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/reinstate-account", "methods": ["POST"]},
      "update subscription": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/update-subscription", "methods": ["POST"]},
      "life events": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events", "methods": ["POST"]},
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events/{lifeEventId}", "methods": ["GET"]}
      ],
      "annual returns": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events/annual-returns", "methods": ["POST"]},
      "bonus payments": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/transactions", "methods": ["POST"]},
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/transactions/{transactionId}", "methods": ["GET"]}
      ],
      "withdrawal charges": [
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/withdrawal-charges", "methods": ["POST"]},
        {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/withdrawal-charges/{transactionId}", "methods": ["GET"]}
      ],
      "property purchase fund release": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events/fund-releases", "methods": ["POST"]},
      "property purchase extension": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events/purchase-extensions", "methods": ["POST"]},
      "property purchase outcome": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/events/purchase-outcomes", "methods": ["POST"]},
      "bulk payments": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/payments?startDate={startDate}&endDate={endDate}", "methods": ["GET"]},
      "bulk payment breakdown": {"href": "/lifetime-isa/manager/$lisaManagerReferenceNumber/accounts/{accountId}/transactions/{transactionId}/payments", "methods": ["GET"]}
    }
  }"""

}