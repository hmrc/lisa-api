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

trait PropertyPurchaseResponse

case class PropertyPurchaseSuccessResponse(id: String) extends PropertyPurchaseResponse

//common
case object PropertyPurchaseErrorResponse extends PropertyPurchaseResponse
case object PropertyPurchaseAccountClosedResponse extends PropertyPurchaseResponse
case object PropertyPurchaseAccountCancelledResponse extends PropertyPurchaseResponse
case object PropertyPurchaseAccountVoidResponse extends PropertyPurchaseResponse
case object PropertyPurchaseAccountNotFoundResponse extends PropertyPurchaseResponse
case object PropertyPurchaseLifeEventAlreadyExistsResponse extends PropertyPurchaseResponse
case object PropertyPurchaseLifeEventAlreadySupersededResponse extends PropertyPurchaseResponse
case object PropertyPurchaseMismatchResponse extends PropertyPurchaseResponse

// fund release
case object PropertyPurchaseAccountNotOpenLongEnoughResponse extends PropertyPurchaseResponse
case object PropertyPurchaseOtherPurchaseOnRecordResponse extends PropertyPurchaseResponse

// extension
case object PropertyPurchaseExtensionOneAlreadyApprovedResponse extends PropertyPurchaseResponse
case object PropertyPurchaseExtensionOneNotYetApprovedResponse extends PropertyPurchaseResponse
case object PropertyPurchaseExtensionTwoAlreadyApprovedResponse extends PropertyPurchaseResponse
case object PropertyPurchaseFundReleaseNotFoundResponse extends PropertyPurchaseResponse
case object PropertyPurchaseFundReleaseSupersededResponse extends PropertyPurchaseResponse
