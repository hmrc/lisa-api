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

case class FundReleasePropertyDetails(nameOrNumber: String, postalCode: String)

object FundReleasePropertyDetails {
  implicit val formats = Json.format[FundReleasePropertyDetails]
}

case class FundReleaseSupersedeDetails(originalFundReleaseId: LifeEventId, originalEventDate: DateTime)

object FundReleaseSupersedeDetails {
  implicit val dateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd")
  implicit val dateWrites: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd")
  implicit val formats = Json.format[FundReleaseSupersedeDetails]
}

trait RequestFundReleaseRequest {
  val eventDate: DateTime
  val withdrawalAmount: Amount
}

case class InitialFundReleaseRequest(eventDate: DateTime, withdrawalAmount: Amount, conveyancerReference: String, propertyDetails: FundReleasePropertyDetails) extends RequestFundReleaseRequest

case class SupersedeFundReleaseRequest(eventDate: DateTime, withdrawalAmount: Amount, supersede: FundReleaseSupersedeDetails) extends RequestFundReleaseRequest

object RequestFundReleaseRequest {
  implicit val dateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd")
  implicit val dateWrites: Writes[DateTime] = Writes.jodaDateWrites("yyyy-MM-dd")

  val initialReads = Json.reads[InitialFundReleaseRequest]

  implicit val initialWrites: Writes[InitialFundReleaseRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[DateTime] and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "conveyancerReference").write[String] and
    (JsPath \ "propertyDetails").write[FundReleasePropertyDetails]
  ){req: InitialFundReleaseRequest => (
    "Funds Release",
    req.eventDate,
    req.withdrawalAmount,
    req.conveyancerReference,
    req.propertyDetails
  )}

  val supersedeReads = Json.reads[SupersedeFundReleaseRequest]

  implicit val supersedeWrites: Writes[SupersedeFundReleaseRequest] = (
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[DateTime] and
    (JsPath \ "withdrawalAmount").write[Amount] and
    (JsPath \ "supersededLifeEventDate").write[DateTime] and
    (JsPath \ "supersededLifeEventID").write[LifeEventId]
  ){req: SupersedeFundReleaseRequest => (
    "Funds Release",
    req.eventDate,
    req.withdrawalAmount,
    req.supersede.originalEventDate,
    req.supersede.originalFundReleaseId
  )}

  implicit val traitReads: Reads[RequestFundReleaseRequest] = Reads[RequestFundReleaseRequest] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]

    supersede match {
      case Some(_) => supersedeReads.reads(json)
      case _ => initialReads.reads(json)
    }
  }

  implicit val traitWrites: Writes[RequestFundReleaseRequest] = Writes[RequestFundReleaseRequest] {
    case s: SupersedeFundReleaseRequest => supersedeWrites.writes(s)
    case i: InitialFundReleaseRequest => initialWrites.writes(i)
  }
}