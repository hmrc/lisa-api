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

sealed trait CreateLisaAccountResponse

case class CreateLisaAccountSuccessResponse(accountId: String) extends CreateLisaAccountResponse

case object CreateLisaAccountInvestorNotFoundResponse extends CreateLisaAccountResponse
case object CreateLisaAccountInvestorNotEligibleResponse extends CreateLisaAccountResponse
case object CreateLisaAccountInvestorComplianceCheckFailedResponse extends CreateLisaAccountResponse
case object CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse extends CreateLisaAccountResponse
case object CreateLisaAccountInvestorAccountAlreadyClosedResponse extends CreateLisaAccountResponse
case object CreateLisaAccountInvestorAccountAlreadyCancelledResponse extends CreateLisaAccountResponse
case object CreateLisaAccountInvestorAccountAlreadyVoidResponse extends CreateLisaAccountResponse
case object CreateLisaAccountAlreadyExistsResponse extends CreateLisaAccountResponse
case object CreateLisaAccountErrorResponse extends CreateLisaAccountResponse
case object CreateLisaAccountServiceUnavailableResponse extends CreateLisaAccountResponse
