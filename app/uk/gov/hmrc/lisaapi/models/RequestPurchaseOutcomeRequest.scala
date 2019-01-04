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

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait RequestPurchaseOutcomeRequest extends ReportLifeEventRequestBase {
  val eventDate: DateTime
}

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

  val initialReads: Reads[RequestPurchaseOutcomeStandardRequest] = (
    (JsPath \ "fundReleaseId").read(JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(JsonReads.propertyPurchaseResult) and
    (JsPath \ "propertyPurchaseValue").read(JsonReads.nonNegativeAmount)
  )(RequestPurchaseOutcomeStandardRequest.apply _)

  val initialWrites: Writes[RequestPurchaseOutcomeStandardRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String] and
    (JsPath \ "fundsReleaseLifeEventID").write[String] and
    (JsPath \ "propertyDetails").write[JsObject]
  ){req: RequestPurchaseOutcomeStandardRequest => (
    "Purchase Result",
    req.eventDate.toString("yyyy-MM-dd"),
    req.fundReleaseId,
    Json.obj(
      "purchaseResult" -> req.propertyPurchaseResult,
      "purchaseValue" -> req.propertyPurchaseValue
    )
  )}

  val supersedeReads: Reads[RequestPurchaseOutcomeSupersededRequest] = (
    (JsPath \ "fundReleaseId").read(JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "propertyPurchaseResult").read(JsonReads.propertyPurchaseResult) and
    (JsPath \ "propertyPurchaseValue").read(JsonReads.nonNegativeAmount) and
    (JsPath \ "supersede").read[PurchaseOutcomeSupersede]
  )(RequestPurchaseOutcomeSupersededRequest.apply _)

  val supersedeWrites: Writes[RequestPurchaseOutcomeSupersededRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String] and
    (JsPath \ "fundsReleaseLifeEventID").write[String] and
    (JsPath \ "propertyDetails").write[JsObject] and
    (JsPath \ "supersededLifeEventID").write[String] and
    (JsPath \ "supersededLifeEventDate").write[String]
  ){req: RequestPurchaseOutcomeSupersededRequest => (
    "Purchase Result",
    req.eventDate.toString("yyyy-MM-dd"),
    req.fundReleaseId,
    Json.obj(
      "purchaseResult" -> req.propertyPurchaseResult,
      "purchaseValue" -> req.propertyPurchaseValue
    ),
    req.supersede.originalLifeEventId,
    req.supersede.originalEventDate.toString("yyyy-MM-dd")
  )}

  implicit val reads: Reads[RequestPurchaseOutcomeRequest] = Reads[RequestPurchaseOutcomeRequest] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]

    supersede match {
      case Some(_) => supersedeReads.reads(json)
      case _ => initialReads.reads(json)
    }
  }

  val desWrites: Writes[RequestPurchaseOutcomeRequest] = Writes[RequestPurchaseOutcomeRequest] {
    case s: RequestPurchaseOutcomeSupersededRequest => supersedeWrites.writes(s)
    case i: RequestPurchaseOutcomeStandardRequest => initialWrites.writes(i)
  }
}