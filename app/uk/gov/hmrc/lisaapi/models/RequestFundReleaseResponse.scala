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

trait RequestFundReleaseResponse

case class RequestFundReleaseSuccessResponse(id: String) extends RequestFundReleaseResponse

case object RequestFundReleaseErrorResponse extends RequestFundReleaseResponse

case object RequestFundReleaseAccountClosedResponse extends RequestFundReleaseResponse
case object RequestFundReleaseAccountCancelledResponse extends RequestFundReleaseResponse
case object RequestFundReleaseAccountVoidResponse extends RequestFundReleaseResponse
case object RequestFundReleaseAccountNotFoundResponse extends RequestFundReleaseResponse
case object RequestFundReleaseAccountNotOpenLongEnoughResponse extends RequestFundReleaseResponse
case object RequestFundReleaseLifeEventAlreadyExistsResponse extends RequestFundReleaseResponse
case object RequestFundReleaseLifeEventAlreadySupersededResponse extends RequestFundReleaseResponse
case object RequestFundReleaseMismatchResponse extends RequestFundReleaseResponse
case object RequestFundReleaseOtherPurchaseOnRecordResponse extends RequestFundReleaseResponse