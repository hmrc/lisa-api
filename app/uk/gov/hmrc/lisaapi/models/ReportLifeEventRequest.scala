/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.models

import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Reads, Writes}

import java.time.LocalDate

trait ReportLifeEventRequestBase extends Product

case class ReportLifeEventRequest(eventType: LifeEventType, eventDate: LocalDate) extends ReportLifeEventRequestBase

object ReportLifeEventRequestBase {
  implicit val writes: Writes[ReportLifeEventRequestBase] = Writes[ReportLifeEventRequestBase] {
    case deathTerminalIllness: ReportLifeEventRequest   => ReportLifeEventRequest.desWrites.writes(deathTerminalIllness)
    case fundRelease: RequestFundReleaseRequest         => RequestFundReleaseRequest.desWrites.writes(fundRelease)
    case purchaseExtension: RequestPurchaseExtension    => RequestPurchaseExtension.desWrites.writes(purchaseExtension)
    case purchaseOutcome: RequestPurchaseOutcomeRequest =>
      RequestPurchaseOutcomeRequest.desWrites.writes(purchaseOutcome)
    case annualReturn: AnnualReturn                     => AnnualReturn.desWrites.writes(annualReturn)
  }
}

object ReportLifeEventRequest {
  implicit val userReads: Reads[ReportLifeEventRequest] = (
    (JsPath \ "eventType").read(JsonReads.lifeEventType) and
      (JsPath \ "eventDate").read(JsonReads.notFutureDate)
  )(ReportLifeEventRequest.apply _)

  val desWrites: Writes[ReportLifeEventRequest] = (
    (JsPath \ "eventType").write[String] and
      (JsPath \ "eventDate").write[LocalDate]
  )(unlift(ReportLifeEventRequest.unapply))
}
