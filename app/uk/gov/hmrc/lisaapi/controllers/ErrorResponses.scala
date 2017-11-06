/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.controllers

import play.api.libs.json.Json

sealed abstract class ErrorResponse(
                                     val httpStatusCode: Int,
                                     val errorCode: String,
                                     val message: String
                                   )

sealed abstract class ErrorResponseWithErrors(
                                     val httpStatusCode: Int,
                                     val errorCode: String,
                                     val message: String,
                                     val errors: Option[List[ErrorValidation]] = None
                                   )

case class ErrorResponseWithId(
                                httpStatusCode: Int,
                                errorCode: String,
                                message: String,
                                id: String
                              )

case class ErrorResponseWithLifeEventId(
                                         httpStatusCode: Int,
                                         errorCode: String,
                                         message: String,
                                         lifeEventID: String
                                       )

case class ErrorResponseWithAccountId(
                                         httpStatusCode: Int,
                                         errorCode: String,
                                         message: String,
                                         accountId: String
                                       )

case class ErrorValidation(
                             errorCode: String,
                             message: String,
                             path: Option[String] = None
                           )

case class ErrorBadRequest(errs: List[ErrorValidation]) extends ErrorResponseWithErrors(400, "BAD_REQUEST", "Bad Request", errors = Some(errs))

case class ErrorForbidden(errs: List[ErrorValidation]) extends ErrorResponseWithErrors(403, "FORBIDDEN", "Forbidden", errors = Some(errs))

case object ErrorBadRequestLmrn extends ErrorResponse(400, "BAD_REQUEST", "lisaManagerReferenceNumber in the URL is in the wrong format")

case object ErrorNotImplemented extends ErrorResponse(501, "NOT_IMPLEMENTED", "Not implemented")

case object ErrorUnauthorized extends ErrorResponse(401, "UNAUTHORIZED", "Bearer token is missing or not authorized")

case object ErrorNotFound extends ErrorResponse(404, "NOT_FOUND", "Resource was not found")

case object ErrorGenericBadRequest extends ErrorResponse(400, "BAD_REQUEST", "Bad Request")

case object ErrorAcceptHeaderInvalid extends ErrorResponse(406, "ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")

case object ErrorInternalServerError extends ErrorResponse(500, "INTERNAL_SERVER_ERROR", "Internal server error")

case object InvalidAuthorisationHeader extends ErrorResponse(403, "AUTH_HEADER_INVALID", "The value provided for Authorization header is invalid")

case object InvalidAcceptHeader extends ErrorResponse(401, "ACCEPT_HEADER_INVALID", "The accept header is missing or invalid")

case object MissingAuthorisationHeader extends ErrorResponse(401, "AUTH_HEADER_MISSING", "The Authorization header is missing")

case object EmptyJson extends ErrorResponse(400, "BAD_REQUEST", "Can't parse empty json")

case object ErrorInvestorNotFound extends ErrorResponse(403, "INVESTOR_NOT_FOUND", "The investor details given do not match with HMRC’s records")

case object ErrorInvestorNotEligible extends ErrorResponse(403, "INVESTOR_ELIGIBILITY_CHECK_FAILED", "The investor is not eligible for a LISA account")

case object ErrorInvestorComplianceCheckFailed extends ErrorResponse(403, "INVESTOR_COMPLIANCE_CHECK_FAILED", "The investor has failed a compliance check - they may have breached ISA guidelines or regulations")

case object ErrorAccountCancellationPeriodExceeded extends ErrorResponse(403, "CANCELLATION_PERIOD_EXCEEDED", "The account cannot be closed with a closure reason of 'Cancellation' as the cancellation period has been exceeded")

case object ErrorAccountWithinCancellationPeriod extends ErrorResponse(403, "ACCOUNT_WITHIN_CANCELLATION_PERIOD", "The account cannot be closed with a closure reason of 'All funds withdrawn' as this  is still within the cancellation period")

case object ErrorAccountBonusPaymentRequired extends ErrorResponse(403, "BONUS_REPAYMENT_REQUIRED", "The account cannot be closed with a closure reason of 'Cancellation' as a bonus payment needs to be repaid")

case object ErrorPreviousAccountDoesNotExist extends ErrorResponse(403, "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST", "The transferredFromAccountId and transferredFromLMRN given don’t match with an account on HMRC’s records")

case object ErrorAccountAlreadyClosedOrVoid extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID", "The LISA account has already been closed or voided.")

case object ErrorAccountNotFound extends ErrorResponse(404, "INVESTOR_ACCOUNTID_NOT_FOUND", "The accountId given does not match with HMRC’s records")

case object ErrorTransferAccountDataNotProvided extends ErrorResponse(403, "TRANSFER_ACCOUNT_DATA_NOT_PROVIDED", "The transferredFromAccountId, transferredFromLMRN and transferInDate are not provided and are required for transfer of an account.")

case object ErrorTransferAccountDataProvided extends ErrorResponse(403, "TRANSFER_ACCOUNT_DATA_PROVIDED", "transferredFromAccountId, transferedFromLMRN, and transferInDate fields should only be completed when the creationReason is \"Transferred\".")

case object ErrorLifeEventInappropriate extends ErrorResponse(403, "LIFE_EVENT_INAPPROPRIATE","The life event conflicts with previous life event reported")

case object ErrorInvalidLisaManager extends ErrorResponse(401,"UNAUTHORIZED","The lisaManagerReferenceNumber path parameter you've used doesn't match with an authorised LISA provider in HMRC's records.")

object ErrorInvestorAlreadyExists {

  def apply(investorId: String) = {
    ErrorResponseWithId(409, "INVESTOR_ALREADY_EXISTS", "The investor already has a record with HMRC", investorId)
  }

}

object ErrorLifeEventAlreadyExists {

  def apply(lifeEventID: String) = {
    ErrorResponseWithLifeEventId(409,"LIFE_EVENT_ALREADY_EXISTS","The investor’s life event has already been reported", lifeEventID)
  }

}

object ErrorAccountAlreadyExists {

  def apply(accountId: String) = {
    ErrorResponseWithAccountId(409,"INVESTOR_ACCOUNT_ALREADY_EXISTS","The LISA account already exists", accountId)
  }

}

case object ErrorLifeEventNotProvided extends ErrorResponse(403,"LIFE_EVENT_NOT_PROVIDED","lifeEventId is required when the claimReason is \"Life Event\"")

