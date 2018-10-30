/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait RequestPurchaseOutcomeRequest

case class RequestPurchaseOutcomeStandardRequest (
  fundReleaseId: FundReleaseId,
  eventDate: DateTime,
  propertyPurchaseResult: String,
  propertyPurchaseValue: Amount
) extends RequestPurchaseOutcomeRequest

case class RequestPurchaseOutcomeSupersededRequest (
  fundReleaseId: FundReleaseId,
  eventDate: DateTime,
  propertyPurchaseResult: String,
  propertyPurchaseValue: Amount,
  supersede: PurchaseOutcomeSupersede
) extends RequestPurchaseOutcomeRequest

case class PurchaseOutcomeSupersede (
  originalPurchaseOutcomeId: LifeEventId,
  originalEventDate: DateTime
)

object PurchaseOutcomeSupersede {
  implicit val dateReads: Reads[DateTime] = JsonReads.notFutureDate
  implicit val dateWrites: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd")

  implicit val reads: Reads[PurchaseOutcomeSupersede] = (
    (JsPath \ "originalPurchaseOutcomeId").read(JsonReads.fundReleaseId) and
    (JsPath \ "originalEventDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(PurchaseOutcomeSupersede.apply _)

  implicit val writes = Json.writes[PurchaseOutcomeSupersede]
}

object RequestPurchaseOutcomeRequest {
  implicit val dateReads: Reads[DateTime] = JsonReads.notFutureDate
  implicit val dateWrites: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd")

  val initialReads: Reads[RequestPurchaseOutcomeStandardRequest] = (
    (JsPath \ "fundReleaseId").read(JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(JsonReads.propertyPurchaseResult) and
    (JsPath \ "propertyPurchaseValue").read(JsonReads.nonNegativeAmount)
  )(RequestPurchaseOutcomeStandardRequest.apply _)

  val supersedeReads: Reads[RequestPurchaseOutcomeSupersededRequest] = (
    (JsPath \ "fundReleaseId").read(JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(JsonReads.propertyPurchaseResult) and
    (JsPath \ "propertyPurchaseValue").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "supersede").read[PurchaseOutcomeSupersede]
  )(RequestPurchaseOutcomeSupersededRequest.apply _)

  val initialWrites = Json.writes[RequestPurchaseOutcomeStandardRequest]
  val supersedeWrites = Json.writes[RequestPurchaseOutcomeSupersededRequest]

  implicit val reads: Reads[RequestPurchaseOutcomeRequest] = Reads[RequestPurchaseOutcomeRequest] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]

    supersede match {
      case Some(_) => supersedeReads.reads(json)
      case _ => initialReads.reads(json)
    }
  }

  implicit val writes: Writes[RequestPurchaseOutcomeRequest] = Writes[RequestPurchaseOutcomeRequest] {
    case s: RequestPurchaseOutcomeSupersededRequest => supersedeWrites.writes(s)
    case i: RequestPurchaseOutcomeStandardRequest => initialWrites.writes(i)
  }
}