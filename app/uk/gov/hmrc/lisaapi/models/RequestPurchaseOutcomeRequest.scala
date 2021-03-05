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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait RequestPurchaseOutcomeRequest extends ReportLifeEventRequestBase {
  val eventDate: DateTime
}

case class RequestPurchaseOutcomeCompletedRequest(
  fundReleaseId: FundReleaseId,
  eventDate: DateTime,
  propertyPurchaseResult: String,
  propertyPurchaseValue: Amount
) extends RequestPurchaseOutcomeRequest

case class RequestPurchaseOutcomeFailedRequest(
  fundReleaseId: FundReleaseId,
  eventDate: DateTime,
  propertyPurchaseResult: String
) extends RequestPurchaseOutcomeRequest

case class RequestPurchaseOutcomeSupersededCompletedRequest(
  eventDate: DateTime,
  propertyPurchaseResult: String,
  propertyPurchaseValue: Amount,
  supersede: PurchaseOutcomeSupersede
) extends RequestPurchaseOutcomeRequest

case class RequestPurchaseOutcomeSupersededFailedRequest(
  eventDate: DateTime,
  propertyPurchaseResult: String,
  supersede: PurchaseOutcomeSupersede
) extends RequestPurchaseOutcomeRequest

case class PurchaseOutcomeSupersede (
  originalLifeEventId: LifeEventId,
  originalEventDate: DateTime
)

object PurchaseOutcomeSupersede {
  implicit val dateReads: Reads[DateTime] = JsonReads.notFutureDate

  implicit val reads: Reads[PurchaseOutcomeSupersede] = (
    (JsPath \ "originalLifeEventId").read(JsonReads.lifeEventId) and
    (JsPath \ "originalEventDate").read(JsonReads.notFutureDate).map(new DateTime(_))
  )(PurchaseOutcomeSupersede.apply _)
}

object RequestPurchaseOutcomeRequest {

  val initialCompletedReads: Reads[RequestPurchaseOutcomeCompletedRequest] = (
    (JsPath \ "fundReleaseId").read(JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(Reads.pattern("Purchase completed".r, "error.formatting.propertyPurchaseResult")) and
    (JsPath \ "propertyPurchaseValue").read(JsonReads.nonNegativeAmount)
  )(RequestPurchaseOutcomeCompletedRequest.apply _)

  val initialFailedReads: Reads[RequestPurchaseOutcomeFailedRequest] = (
    (JsPath \ "fundReleaseId").read(JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(Reads.pattern("Purchase failed".r, "error.formatting.propertyPurchaseResult"))
  )(RequestPurchaseOutcomeFailedRequest.apply _)

  val initialCompletedWrites: Writes[RequestPurchaseOutcomeCompletedRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String] and
    (JsPath \ "fundsReleaseLifeEventID").write[String] and
    (JsPath \ "propertyDetails").write[JsObject]
  ){req: RequestPurchaseOutcomeCompletedRequest => (
    "Purchase Result",
    req.eventDate.toString("yyyy-MM-dd"),
    req.fundReleaseId,
    Json.obj(
      "purchaseResult" -> req.propertyPurchaseResult,
      "purchaseValue" -> req.propertyPurchaseValue
    )
  )}

  val initialFailedWrites: Writes[RequestPurchaseOutcomeFailedRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String] and
    (JsPath \ "fundsReleaseLifeEventID").write[String] and
    (JsPath \ "propertyDetails").write[JsObject]
  ){req: RequestPurchaseOutcomeFailedRequest => (
    "Purchase Result",
    req.eventDate.toString("yyyy-MM-dd"),
    req.fundReleaseId,
    Json.obj(
      "purchaseResult" -> req.propertyPurchaseResult
    )
  )}

  val supersedeCompletedReads: Reads[RequestPurchaseOutcomeSupersededCompletedRequest] = (
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(Reads.pattern("Purchase completed".r, "error.formatting.propertyPurchaseResult")) and
    (JsPath \ "propertyPurchaseValue").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "supersede").read[PurchaseOutcomeSupersede]
  )(RequestPurchaseOutcomeSupersededCompletedRequest.apply _)

  val supersedeFailedReads: Reads[RequestPurchaseOutcomeSupersededFailedRequest] = (
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(Reads.pattern("Purchase failed".r, "error.formatting.propertyPurchaseResult")) and
    (JsPath \ "supersede").read[PurchaseOutcomeSupersede]
  )(RequestPurchaseOutcomeSupersededFailedRequest.apply _)

  val supersedeCompletedWrites: Writes[RequestPurchaseOutcomeSupersededCompletedRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String] and
    (JsPath \ "propertyDetails").write[JsObject] and
    (JsPath \ "supersededLifeEventID").write[String] and
    (JsPath \ "supersededLifeEventDate").write[String]
  ){req: RequestPurchaseOutcomeSupersededCompletedRequest => (
    "Purchase Result",
    req.eventDate.toString("yyyy-MM-dd"),
    Json.obj(
      "purchaseResult" -> req.propertyPurchaseResult,
      "purchaseValue" -> req.propertyPurchaseValue
    ),
    req.supersede.originalLifeEventId,
    req.supersede.originalEventDate.toString("yyyy-MM-dd")
  )}

  val supersedeFailedWrites: Writes[RequestPurchaseOutcomeSupersededFailedRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String] and
    (JsPath \ "propertyDetails").write[JsObject] and
    (JsPath \ "supersededLifeEventID").write[String] and
    (JsPath \ "supersededLifeEventDate").write[String]
  ){req: RequestPurchaseOutcomeSupersededFailedRequest => (
    "Purchase Result",
    req.eventDate.toString("yyyy-MM-dd"),
    Json.obj(
      "purchaseResult" -> req.propertyPurchaseResult
    ),
    req.supersede.originalLifeEventId,
    req.supersede.originalEventDate.toString("yyyy-MM-dd")
  )}

  implicit val reads: Reads[RequestPurchaseOutcomeRequest] = Reads[RequestPurchaseOutcomeRequest] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]
    val purchaseResult = (json \ "propertyPurchaseResult").asOpt[String]

    (supersede, purchaseResult) match {
      case (Some(_), Some("Purchase completed")) => supersedeCompletedReads.reads(json)
      case (Some(_), _) => supersedeFailedReads.reads(json)
      case (None, Some("Purchase completed")) => initialCompletedReads.reads(json)
      case (None, _) => initialFailedReads.reads(json)
    }
  }

  val desWrites: Writes[RequestPurchaseOutcomeRequest] = Writes[RequestPurchaseOutcomeRequest] {
    case s: RequestPurchaseOutcomeSupersededCompletedRequest => supersedeCompletedWrites.writes(s)
    case s: RequestPurchaseOutcomeSupersededFailedRequest => supersedeFailedWrites.writes(s)
    case i: RequestPurchaseOutcomeCompletedRequest => initialCompletedWrites.writes(i)
    case i: RequestPurchaseOutcomeFailedRequest => initialFailedWrites.writes(i)
  }
}