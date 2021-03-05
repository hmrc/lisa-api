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

package uk.gov.hmrc.lisaapi.metrics

import java.util.concurrent.TimeUnit

import com.codahale.metrics.MetricRegistry
import com.google.inject.Inject
import com.kenshoo.play.metrics.Metrics

import scala.util.Try

class LisaMetrics @Inject()(metrics: Metrics) {

  val registry: MetricRegistry = metrics.defaultRegistry

  def timer(diff: Long, unit: TimeUnit, metricType: String):Unit = registry.timer(s"${metricType}").update(diff, unit)

  def incrementMetrics(startTime: Long, status: Int, api: String): Unit = {
    val diff = System.currentTimeMillis() - startTime
    val unit = TimeUnit.MILLISECONDS

    timer(diff, unit, api)
    timer(diff, unit, LisaMetricKeys.lisaMetric(status, api))
  }

}

trait LisaMetricKeys  {
  val DISCOVER = "DISCOVER"
  val INVESTOR = "LISA_INVESTOR"
  val ACCOUNT = "CREATE_OR_TRANSFER_ACCOUNT"
  val CLOSE = "CLOSE_ACCOUNT"
  val REINSTATE = "REINSTATE_ACCOUNT"
  val EVENT = "LIFE_EVENT"
  val BONUS_PAYMENT = "BONUS_PAYMENT"
  val WITHDRAWAL_CHARGE = "WITHDRAWAL_CHARGE"
  val UPDATE_SUBSCRIPTION = "UPDATE_SUBSCRIPTION"
  val TRANSACTION = "TRANSACTION"
  val PROPERTY_PURCHASE = "PROPERTY_PURCHASE"

  val keys = Map(
    "investors" -> INVESTOR,
    "accounts" -> ACCOUNT,
    "close-account" -> CLOSE,
    "transactions" -> BONUS_PAYMENT,
    "events" -> EVENT,
    "discover" -> DISCOVER,
    "payments" -> TRANSACTION,
    "update-subscription" -> UPDATE_SUBSCRIPTION,
    "withdrawal-charge" -> WITHDRAWAL_CHARGE,
    "property-purchase" -> PROPERTY_PURCHASE
  )

  def lisaMetric(status:Int, name:String):String =  s"${name}_${status}"

  def getMetricKey(url:String):String = keys.getOrElse(Try(url.split("/").last).getOrElse("discover"), "UNKNOWN")

}
object LisaMetricKeys extends LisaMetricKeys




