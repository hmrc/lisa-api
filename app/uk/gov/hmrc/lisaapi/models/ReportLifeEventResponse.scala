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

trait ReportLifeEventResponse

@deprecated("Still valid for v1, but replaced in v2 by separate closed, void, and cancelled responses.")
case object ReportLifeEventAccountClosedOrVoidResponse extends ReportLifeEventResponse

case class ReportLifeEventSuccessResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventInappropriateResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountClosedResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountVoidResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountCancelledResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountNotFoundResponse extends ReportLifeEventResponse
case object ReportLifeEventAlreadyExistsResponse extends ReportLifeEventResponse
case object ReportLifeEventAlreadySupersededResponse extends ReportLifeEventResponse
case object ReportLifeEventMismatchResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountNotOpenLongEnoughResponse extends ReportLifeEventResponse
case object ReportLifeEventOtherPurchaseOnRecordResponse extends ReportLifeEventResponse
case object ReportLifeEventFundReleaseSupersededResponse extends ReportLifeEventResponse
case object ReportLifeEventFundReleaseNotFoundResponse extends ReportLifeEventResponse
case object ReportLifeEventExtensionOneAlreadyApprovedResponse extends ReportLifeEventResponse
case object ReportLifeEventExtensionTwoAlreadyApprovedResponse extends ReportLifeEventResponse
case object ReportLifeEventExtensionOneNotYetApprovedResponse extends ReportLifeEventResponse
case object ReportLifeEventErrorResponse extends ReportLifeEventResponse