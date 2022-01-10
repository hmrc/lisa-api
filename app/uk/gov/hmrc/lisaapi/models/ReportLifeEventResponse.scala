/*
 * Copyright 2022 HM Revenue & Customs
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

@deprecated("Still valid for v1, but replaced in v2 by separate closed, void, and cancelled responses.", "v2")
case object ReportLifeEventAccountClosedOrVoidResponse extends ReportLifeEventResponse

case object ReportLifeEventInvalidPayload extends ReportLifeEventResponse
case class ReportLifeEventSuccessResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventInappropriateResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountClosedResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountVoidResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountCancelledResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountNotFoundResponse extends ReportLifeEventResponse
case class ReportLifeEventAlreadyExistsResponse(lifeEventId: String) extends ReportLifeEventResponse
case class ReportLifeEventAlreadySupersededResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventMismatchResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountNotOpenLongEnoughResponse extends ReportLifeEventResponse
case object ReportLifeEventOtherPurchaseOnRecordResponse extends ReportLifeEventResponse
case class ReportLifeEventFundReleaseSupersededResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventFundReleaseNotFoundResponse extends ReportLifeEventResponse
case class ReportLifeEventExtensionOneAlreadyApprovedResponse(lifeEventId: String) extends ReportLifeEventResponse
case class ReportLifeEventExtensionTwoAlreadyApprovedResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventExtensionOneNotYetApprovedResponse extends ReportLifeEventResponse
case object ReportLifeEventServiceUnavailableResponse extends ReportLifeEventResponse
case object ReportLifeEventErrorResponse extends ReportLifeEventResponse