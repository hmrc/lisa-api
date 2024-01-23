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
import play.api.libs.json._

import java.time.LocalDate

case class FundReleasePropertyDetails(nameOrNumber: String, postalCode: String)

object FundReleasePropertyDetails {

  private val nameOrNumberRegex = "^[A-Za-z0-9 :/-]{1,35}$"

  private val postalCodeRegex = "^[A-Za-z0-9 ]{1,8}$"

  private def toJsError(errorType: String): JsError =
    JsError(JsonValidationError(errorType))

  private def getNameOrNumberErrorMessage(nameOrNumber: String): JsError =
    nameOrNumber match {
      case name if name.isEmpty     => toJsError("emptyNameOrNumber")
      case name if name.length > 35 => toJsError("tooLongNameOrNumber")
      case _                        => toJsError("invalidNameOrNumber")
    }

  private def nameOrNumberValidator: Reads[String] = (nameOrNumberJsValue: JsValue) => {
    val nameOrNumber = nameOrNumberJsValue.as[String]
    if (nameOrNumber.matches(nameOrNumberRegex)) JsSuccess(nameOrNumber) else getNameOrNumberErrorMessage(nameOrNumber)
  }

  private def postalCodeValidator: Reads[String] = (postalCode: JsValue) =>
    postalCode.as[String] match {
      case postalCode if postalCode.matches(postalCodeRegex) => JsSuccess(postalCode)
      case postalCode if postalCode.isEmpty                  => toJsError("emptyPostalCode")
      case _                                                 => toJsError("invalidPostalCode")
    }

  implicit val reads: Reads[FundReleasePropertyDetails] = (
    (JsPath \ "nameOrNumber").read[String](nameOrNumberValidator) and
      (JsPath \ "postalCode").read[String](postalCodeValidator)
  )(FundReleasePropertyDetails.apply _)

  implicit val writes: Writes[FundReleasePropertyDetails] = Json.writes[FundReleasePropertyDetails]
}

case class FundReleaseSupersedeDetails(originalLifeEventId: LifeEventId, originalEventDate: LocalDate)

object FundReleaseSupersedeDetails {
  implicit val formats: OFormat[FundReleaseSupersedeDetails] = Json.format[FundReleaseSupersedeDetails]
}

trait RequestFundReleaseRequest extends ReportLifeEventRequestBase {
  val eventDate: LocalDate
  val withdrawalAmount: Amount
}

case class InitialFundReleaseRequest(
  eventDate: LocalDate,
  withdrawalAmount: Amount,
  conveyancerReference: Option[String],
  propertyDetails: Option[FundReleasePropertyDetails]
) extends RequestFundReleaseRequest

case class SupersedeFundReleaseRequest(
  eventDate: LocalDate,
  withdrawalAmount: Amount,
  supersede: FundReleaseSupersedeDetails
) extends RequestFundReleaseRequest

object RequestFundReleaseRequest {
  implicit val dateReads: Reads[LocalDate]   = JsonReads.notFutureDate

  val initialReads: Reads[InitialFundReleaseRequest] = Json.reads[InitialFundReleaseRequest]

  implicit val initialWrites: Writes[InitialFundReleaseRequest] = (
    (JsPath \ "eventType").write[String] and
      (JsPath \ "eventDate").write[LocalDate] and
      (JsPath \ "withdrawalAmount").write[Amount] and
      (JsPath \ "conveyancerReference").writeNullable[String] and
      (JsPath \ "propertyDetails").writeNullable[FundReleasePropertyDetails]
  ) { req: InitialFundReleaseRequest =>
    (
      "Funds Release",
      req.eventDate,
      req.withdrawalAmount,
      req.conveyancerReference,
      req.propertyDetails
    )
  }

  val supersedeReads: Reads[SupersedeFundReleaseRequest] = Json.reads[SupersedeFundReleaseRequest]

  implicit val supersedeWrites: Writes[SupersedeFundReleaseRequest] = (
    (JsPath \ "eventType").write[String] and
      (JsPath \ "eventDate").write[LocalDate] and
      (JsPath \ "withdrawalAmount").write[Amount] and
      (JsPath \ "supersededLifeEventDate").write[LocalDate] and
      (JsPath \ "supersededLifeEventID").write[LifeEventId]
  ) { req: SupersedeFundReleaseRequest =>
    (
      "Funds Release",
      req.eventDate,
      req.withdrawalAmount,
      req.supersede.originalEventDate,
      req.supersede.originalLifeEventId
    )
  }

  implicit val traitReads: Reads[RequestFundReleaseRequest] = Reads[RequestFundReleaseRequest] { json =>
    val supersede = (json \ "supersede").asOpt[JsValue]

    supersede match {
      case Some(_) => supersedeReads.reads(json)
      case _       => initialReads.reads(json)
    }
  }

  val desWrites: Writes[RequestFundReleaseRequest] = Writes[RequestFundReleaseRequest] {
    case s: SupersedeFundReleaseRequest => supersedeWrites.writes(s)
    case i: InitialFundReleaseRequest   => initialWrites.writes(i)
  }
}
