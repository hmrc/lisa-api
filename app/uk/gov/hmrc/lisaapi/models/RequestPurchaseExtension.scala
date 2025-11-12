/*
 * Copyright 2025 HM Revenue & Customs
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
import play.api.libs.json._

import java.time.LocalDate

abstract class RequestPurchaseExtension extends ReportLifeEventRequestBase {
  val eventDate: LocalDate
  val eventType: LifeEventType
}

case class RequestStandardPurchaseExtension(fundReleaseId: LifeEventId, eventDate: LocalDate, eventType: LifeEventType)
    extends RequestPurchaseExtension
case class RequestSupersededPurchaseExtension(
  eventDate: LocalDate,
  eventType: LifeEventType,
  supersede: RequestExtensionSupersedeDetails
) extends RequestPurchaseExtension
case class RequestExtensionSupersedeDetails(originalEventDate: LocalDate, originalLifeEventId: LifeEventId)

object RequestPurchaseExtension {
  implicit val dateReads: Reads[LocalDate]   = JsonReads.notFutureDate

  implicit val supersedeDetailReads: Reads[RequestExtensionSupersedeDetails] = (
    (JsPath \ "originalEventDate").read(JsonReads.notFutureDate) and
      (JsPath \ "originalLifeEventId").read[LifeEventId](JsonReads.lifeEventId)
  )(RequestExtensionSupersedeDetails.apply _)

  val standardReads: Reads[RequestStandardPurchaseExtension] = (
    (JsPath \ "fundReleaseId").read[LifeEventId](JsonReads.fundReleaseId) and
      (JsPath \ "eventDate").read(JsonReads.notFutureDate) and
      (JsPath \ "eventType").read(Reads.pattern("^(Extension one|Extension two)$".r, "error.formatting.extensionType"))
  )(RequestStandardPurchaseExtension.apply _)

  val standardWrites: Writes[RequestStandardPurchaseExtension] = (
    (JsPath \ "eventType").write[String] and
      (JsPath \ "eventDate").write[LocalDate] and
      (JsPath \ "fundsReleaseLifeEventID").write[String]
  ) { req: RequestStandardPurchaseExtension =>
    (
      req.eventType,
      req.eventDate,
      req.fundReleaseId
    )
  }

  val supersededReads: Reads[RequestSupersededPurchaseExtension] = (
    (JsPath \ "eventDate").read(JsonReads.notFutureDate) and
      (JsPath \ "eventType").read(
        Reads.pattern("^(Extension one|Extension two)$".r, "error.formatting.extensionType")
      ) and
      (JsPath \ "supersede").read[RequestExtensionSupersedeDetails]
  )(RequestSupersededPurchaseExtension.apply _)

  val supersededWrites: Writes[RequestSupersededPurchaseExtension] = (
    (JsPath \ "eventType").write[String] and
      (JsPath \ "eventDate").write[LocalDate] and
      (JsPath \ "supersededLifeEventDate").write[LocalDate] and
      (JsPath \ "supersededLifeEventID").write[String]
  ) { req: RequestSupersededPurchaseExtension =>
    (
      req.eventType,
      req.eventDate,
      req.supersede.originalEventDate,
      req.supersede.originalLifeEventId
    )
  }

  implicit val reads: Reads[RequestPurchaseExtension] = Reads[RequestPurchaseExtension] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]

    supersede match {
      case Some(_) => supersededReads.reads(json)
      case _       => standardReads.reads(json)
    }
  }

  val desWrites: Writes[RequestPurchaseExtension] = Writes[RequestPurchaseExtension] {
    case std: RequestStandardPurchaseExtension   => standardWrites.writes(std)
    case sup: RequestSupersededPurchaseExtension => supersededWrites.writes(sup)
  }

}
