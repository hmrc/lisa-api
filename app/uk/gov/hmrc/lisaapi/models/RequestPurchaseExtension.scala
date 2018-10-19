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

abstract class RequestPurchaseExtension extends Product {
  val eventDate: DateTime
  val eventType: LifeEventType
}

case class RequestStandardPurchaseExtension(fundReleaseId: LifeEventId, eventDate: DateTime, eventType: LifeEventType) extends RequestPurchaseExtension
case class RequestSupersededPurchaseExtension(eventDate: DateTime, eventType: LifeEventType, supersede: RequestExtensionSupersedeDetails) extends RequestPurchaseExtension
case class RequestExtensionSupersedeDetails(originalEventDate: DateTime, originalExtensionId: LifeEventId)

object RequestPurchaseExtension {
  implicit val dateReads: Reads[DateTime] = JsonReads.notFutureDate
  implicit val dateWrites: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd")
  //

  implicit val supersedeDetailReads: Reads[RequestExtensionSupersedeDetails] = (
    (JsPath \ "originalEventDate").read(JsonReads.notFutureDate) and
    (JsPath \ "originalExtensionId").read[LifeEventId](JsonReads.extensionId)
  )(RequestExtensionSupersedeDetails.apply _)

  val standardReads: Reads[RequestStandardPurchaseExtension] = (
    (JsPath \ "fundReleaseId").read[LifeEventId](JsonReads.fundReleaseId) and
    (JsPath \ "eventDate").read(JsonReads.notFutureDate) and
    (JsPath \ "eventType").read(Reads.pattern("^(Extension one|Extension two)$".r, "error.formatting.extensionType"))
  )(RequestStandardPurchaseExtension.apply _)

  implicit val standardWrites: Writes[RequestStandardPurchaseExtension] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[DateTime] and
    (JsPath \ "fundsReleaseLifeEventID").write[String]
  ){req: RequestStandardPurchaseExtension => (
    req.eventType,
    req.eventDate,
    req.fundReleaseId
  )}

  val supersededReads: Reads[RequestSupersededPurchaseExtension] = (
    (JsPath \ "eventDate").read(JsonReads.notFutureDate) and
    (JsPath \ "eventType").read(Reads.pattern("^(Extension one|Extension two)$".r, "error.formatting.extensionType")) and
    (JsPath \ "supersede").read[RequestExtensionSupersedeDetails]
  )(RequestSupersededPurchaseExtension.apply _)

  implicit val supersededWrites: Writes[RequestSupersededPurchaseExtension] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[DateTime] and
    (JsPath \ "supersededLifeEventDate").write[DateTime] and
    (JsPath \ "supersededLifeEventID").write[String]
  ){req: RequestSupersededPurchaseExtension => (
    req.eventType,
    req.eventDate,
    req.supersede.originalEventDate,
    req.supersede.originalExtensionId
  )}

  implicit val reads: Reads[RequestPurchaseExtension] = Reads[RequestPurchaseExtension] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]

    supersede match {
      case Some(_) => supersededReads.reads(json)
      case _ => standardReads.reads(json)
    }
  }

  implicit val traitWrites: Writes[RequestPurchaseExtension] = Writes[RequestPurchaseExtension] {
    case std: RequestStandardPurchaseExtension => standardWrites.writes(std)
    case sup: RequestSupersededPurchaseExtension => supersededWrites.writes(sup)
  }

}