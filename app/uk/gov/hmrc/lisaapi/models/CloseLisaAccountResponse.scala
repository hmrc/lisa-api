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

sealed trait CloseLisaAccountResponse

case class CloseLisaAccountSuccessResponse(accountId: String) extends CloseLisaAccountResponse
case object CloseLisaAccountErrorResponse extends CloseLisaAccountResponse
case object CloseLisaAccountAlreadyVoidResponse extends CloseLisaAccountResponse
case object CloseLisaAccountAlreadyClosedResponse extends CloseLisaAccountResponse
case object CloseLisaAccountNotFoundResponse extends CloseLisaAccountResponse
case object CloseLisaAccountCancellationPeriodExceeded extends CloseLisaAccountResponse
case object CloseLisaAccountWithinCancellationPeriod extends CloseLisaAccountResponse