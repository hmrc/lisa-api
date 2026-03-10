/*
 * Copyright 2026 HM Revenue & Customs
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

import com.codahale.metrics.MetricRegistry
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture
import uk.gov.hmrc.play.bootstrap.metrics.Metrics

import java.util.concurrent.TimeUnit

class LisaMetricsSpec extends BaseTestFixture with MockitoSugar {

  "LisaMetrics" must {

    "record timer metrics" in {
      val mockMetrics  = mock[Metrics]
      val mockRegistry = mock[MetricRegistry]
      val mockTimer    = mock[com.codahale.metrics.Timer]

      when(mockMetrics.defaultRegistry).thenReturn(mockRegistry)
      when(mockRegistry.timer("test-metric")).thenReturn(mockTimer)

      val lisaMetrics = new LisaMetrics(mockMetrics)

      lisaMetrics.timer(100L, TimeUnit.MILLISECONDS, "test-metric")

      verify(mockTimer).update(100L, TimeUnit.MILLISECONDS)
    }

    "increment metrics with status code" in {
      val mockMetrics  = mock[Metrics]
      val mockRegistry = mock[MetricRegistry]
      val mockTimer1   = mock[com.codahale.metrics.Timer]
      val mockTimer2   = mock[com.codahale.metrics.Timer]

      when(mockMetrics.defaultRegistry).thenReturn(mockRegistry)
      when(mockRegistry.timer("test-api")).thenReturn(mockTimer1)
      when(mockRegistry.timer("test-api_200")).thenReturn(mockTimer2)

      val lisaMetrics = new LisaMetrics(mockMetrics)
      val startTime   = System.currentTimeMillis() - 100

      lisaMetrics.incrementMetrics(startTime, 200, "test-api")

      verify(mockRegistry).timer("test-api")
      verify(mockRegistry).timer("test-api_200")
    }
  }

  "LisaMetricKeys" must {

    "return correct metric key for known URLs" in {
      LisaMetricKeys.getMetricKey("http://localhost/investors")           mustBe LisaMetricKeys.INVESTOR
      LisaMetricKeys.getMetricKey("http://localhost/accounts")            mustBe LisaMetricKeys.ACCOUNT
      LisaMetricKeys.getMetricKey("http://localhost/close-account")       mustBe LisaMetricKeys.CLOSE
      LisaMetricKeys.getMetricKey("http://localhost/transactions")        mustBe LisaMetricKeys.BONUS_PAYMENT
      LisaMetricKeys.getMetricKey("http://localhost/events")              mustBe LisaMetricKeys.EVENT
      LisaMetricKeys.getMetricKey("http://localhost/discover")            mustBe LisaMetricKeys.DISCOVER
      LisaMetricKeys.getMetricKey("http://localhost/payments")            mustBe LisaMetricKeys.TRANSACTION
      LisaMetricKeys.getMetricKey("http://localhost/update-subscription") mustBe LisaMetricKeys.UPDATE_SUBSCRIPTION
      LisaMetricKeys.getMetricKey("http://localhost/withdrawal-charge")   mustBe LisaMetricKeys.WITHDRAWAL_CHARGE
      LisaMetricKeys.getMetricKey("http://localhost/property-purchase")   mustBe LisaMetricKeys.PROPERTY_PURCHASE
    }

    "return UNKNOWN for unknown URLs" in {
      LisaMetricKeys.getMetricKey("http://localhost/unknown-endpoint") mustBe "UNKNOWN"
    }

    "return UNKNOWN for invalid URLs" in {
      LisaMetricKeys.getMetricKey("") mustBe "UNKNOWN"
    }

    "create lisa metric with status code" in {
      LisaMetricKeys.lisaMetric(200, "test-api")    mustBe "test-api_200"
      LisaMetricKeys.lisaMetric(404, "another-api") mustBe "another-api_404"
    }
  }

}
