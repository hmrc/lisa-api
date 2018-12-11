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
import play.api.libs.json.{JsPath, Json, Reads, Writes}

case class GetLifeEventItemPropertyDetails(nameOrNumber: String, postalCode: String)
object GetLifeEventItemPropertyDetails {
  implicit val formats = Json.format[GetLifeEventItemPropertyDetails]
}

case class GetLifeEventItemSupersede(originalLifeEventId: LifeEventId, originalEventDate: DateTime)
object GetLifeEventItemSupersede {
  implicit val dateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd")
  implicit val formats = Json.format[GetLifeEventItemSupersede]
}

case class GetLifeEventItem(
  lifeEventId: LifeEventId,
  eventType: LifeEventType,
  eventDate: DateTime,
  lisaManagerName: Option[String] = None,
  taxYear: Option[Int] = None,
  marketValueCash: Option[Int] = None,
  marketValueStocksAndShares: Option[Int] = None,
  annualSubsCash: Option[Int] = None,
  annualSubsStocksAndCash: Option[Int] = None,
  withdrawalAmount: Option[Amount] = None,
  conveyancerReference: Option[String] = None,
  propertyDetails: Option[GetLifeEventItemPropertyDetails] = None,
  fundReleaseId: Option[FundReleaseId] = None,
  propertyPurchaseValue: Option[Amount] = None,
  propertyPurchaseResult: Option[String] = None,
  supersede: Option[GetLifeEventItemSupersede] = None,
  supersededBy: Option[LifeEventId] = None
)

object GetLifeEventItem {
  implicit val dateReads: Reads[DateTime] = Reads.jodaDateReads("yyyy-MM-dd")

  implicit val reads: Reads[GetLifeEventItem] = (
    (JsPath \ "lifeEventId").read(JsonReads.lifeEventId) and
    (JsPath \ "lifeEventType").read[String] and
    (JsPath \ "lifeEventDate").read(JsonReads.notFutureDate).map(new DateTime(_)) and
    (JsPath \ "isaManagerName").readNullable[String] and
    (JsPath \ "taxYear").readNullable[String].map(_.map(_.toInt)) and
    (JsPath \ "marketValueCash").readNullable[Int] and
    (JsPath \ "marketValueStocksAndShares").readNullable[Int] and
    (JsPath \ "annualSubsCash").readNullable[Int] and
    (JsPath \ "annualSubsStocksAndShares").readNullable[Int] and
    (JsPath \ "withdrawalAmount").readNullable[Amount] and
    (JsPath \ "conveyancerReference").readNullable[String] and
    (JsPath \ "propertyDetails").readNullable[GetLifeEventItemPropertyDetails] and
    (JsPath \ "fundReleaseId").readNullable[FundReleaseId] and
    (JsPath \ "propertyPurchaseValue").readNullable[Amount] and
    (JsPath \ "propertyPurchaseResult").readNullable[String] and
    (JsPath \ "supersedeLifeEventId").readNullable[LifeEventId] and
    (JsPath \ "supersedeLifeEventDate").readNullable[String].map(_.map(new DateTime(_))) and
    (JsPath \ "supersededBy").readNullable[LifeEventId]
  )((lifeEventId,
     lifeEventType,
     lifeEventDate,
     isaManagerName,
     taxYear,
     marketValueCash,
     marketValueStocksAndShares,
     annualSubsCash,
     annualSubsStocksAndShares,
     withdrawalAmount,
     conveyancerReference,
     propertyDetails,
     fundReleaseId,
     propertyPurchaseValue,
     propertyPurchaseResult,
     supersedeLifeEventId,
     supersedeLifeEventDate,
     supersededBy
  ) =>
    GetLifeEventItem(
      lifeEventId = lifeEventId,
      eventType = lifeEventType match {
        case "TERMINAL_ILLNESS" => "LISA Investor Terminal Ill Health"
        case "DEATH" => "LISA Investor Death"
        case "PURCHASE_FUNDS_RELEASE" => "Funds release"
        case "EXTENSION_ONE" => "Extension one"
        case "EXTENSION_TWO" => "Extension two"
        case "PURCHASE_RESULT" => "Purchase outcome"
        case "STATUTORY_SUBMISSION" => "Statutory Submission"
      },
      eventDate = lifeEventDate
    )
  )

  implicit val writes: Writes[GetLifeEventItem] = (
    (JsPath \ "lifeEventId").write[String] and
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd")) and
    (JsPath \ "lisaManagerName").writeNullable[String] and
    (JsPath \ "taxYear").writeNullable[Int] and
    (JsPath \ "marketValueCash").writeNullable[Int] and
    (JsPath \ "marketValueStocksAndShares").writeNullable[Int] and
    (JsPath \ "annualSubsCash").writeNullable[Int] and
    (JsPath \ "annualSubsStocksAndCash").writeNullable[Int] and
    (JsPath \ "withdrawalAmount").writeNullable[Amount] and
    (JsPath \ "conveyancerReference").writeNullable[String] and
    (JsPath \ "propertyDetails").writeNullable[GetLifeEventItemPropertyDetails] and
    (JsPath \ "fundReleaseId").writeNullable[FundReleaseId] and
    (JsPath \ "propertyPurchaseValue").writeNullable[Amount] and
    (JsPath \ "propertyPurchaseResult").writeNullable[String] and
    (JsPath \ "supersede").writeNullable[GetLifeEventItemSupersede] and
    (JsPath \ "supersededBy").writeNullable[LifeEventId]
  )((event: GetLifeEventItem) => {
    (
      event.lifeEventId,
      event.eventType,
      event.eventDate,
      event.lisaManagerName,
      event.taxYear,
      event.marketValueCash,
      event.marketValueStocksAndShares,
      event.annualSubsCash,
      event.annualSubsStocksAndCash,
      event.withdrawalAmount,
      event.conveyancerReference,
      event.propertyDetails,
      event.fundReleaseId,
      event.propertyPurchaseValue,
      event.propertyPurchaseResult,
      event.supersede,
      event.supersededBy
    )
  })
}