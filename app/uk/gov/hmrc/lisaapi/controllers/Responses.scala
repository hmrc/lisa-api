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

package uk.gov.hmrc.lisaapi.controllers

import play.api.Logger
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

case class ErrorResponseWithTransactionId(
                                         httpStatusCode: Int,
                                         errorCode: String,
                                         message: String,
                                         transactionId: String
                                       )

case class ErrorResponseWithAccountId (
                                         override val httpStatusCode: Int,
                                         override val errorCode: String,
                                         override val message: String,
                                         accountId: String
                                       ) extends ErrorResponse(httpStatusCode, errorCode, message)


case class ErrorValidation(
                             errorCode: String,
                             message: String,
                             path: Option[String] = None
                           )

case class ErrorBadRequest(errs: List[ErrorValidation]) extends ErrorResponseWithErrors(400, "BAD_REQUEST", "Bad Request", errors = Some(errs))

case class ErrorForbidden(errs: List[ErrorValidation]) extends ErrorResponseWithErrors(403, "FORBIDDEN", "There is a problem with the request data", errors = Some(errs))

case object ErrorBadRequestLmrn extends ErrorResponse(400, "BAD_REQUEST", "lisaManagerReferenceNumber in the URL is in the wrong format")
case object ErrorBadRequestAccountId extends ErrorResponse(400, "BAD_REQUEST", "accountId in the URL is in the wrong format")

// specific errors for bulk payment url validation
case object ErrorBadRequestStart extends ErrorResponse(400, "BAD_REQUEST", "startDate is in the wrong format")
case object ErrorBadRequestEnd extends ErrorResponse(400, "BAD_REQUEST", "endDate is in the wrong format")
case object ErrorBadRequestStartEnd extends ErrorResponse(400, "BAD_REQUEST", "startDate and endDate are in the wrong format")
case object ErrorBadRequestEndInFuture extends ErrorResponse(403, "FORBIDDEN", "endDate cannot be in the future")
case object ErrorBadRequestEndBeforeStart extends ErrorResponse(403, "FORBIDDEN", "endDate cannot be before startDate")
case object ErrorBadRequestStartBefore6April2017 extends ErrorResponse(403, "FORBIDDEN", "startDate cannot be before 6 April 2017")
case object ErrorBadRequestOverYearBetweenStartAndEnd extends ErrorResponse(403, "FORBIDDEN", "endDate cannot be more than a year after startDate")
// end

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

case object ErrorLifeEventIdNotFound extends ErrorResponse(403, "LIFE_EVENT_NOT_FOUND", "The lifeEventId does not match with HMRC’s records")

case object ErrorInvestorNotEligible extends ErrorResponse(403, "INVESTOR_ELIGIBILITY_CHECK_FAILED", "The investor is not eligible for a LISA account")

case object ErrorInvestorComplianceCheckFailedCreateTransfer extends ErrorResponse(403, "INVESTOR_COMPLIANCE_CHECK_FAILED", "You cannot create or transfer a LISA account because the investor has failed a compliance check")
case object ErrorInvestorComplianceCheckFailedReinstate extends ErrorResponse(403, "INVESTOR_COMPLIANCE_CHECK_FAILED", "You cannot reinstate this account because the investor has failed a compliance check")

case object ErrorAccountCancellationPeriodExceeded extends ErrorResponse(403, "CANCELLATION_PERIOD_EXCEEDED", "You cannot close the account with cancellation as the reason because the cancellation period is over")

case object ErrorAccountWithinCancellationPeriod extends ErrorResponse(403, "ACCOUNT_WITHIN_CANCELLATION_PERIOD", "You cannot close the account with all funds withdrawn as the reason because it is within the cancellation period")

case object ErrorPreviousAccountDoesNotExist extends ErrorResponse(403, "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST", "The transferredFromAccountId and transferredFromLMRN given do not match an account on HMRC’s records")

case object ErrorAccountAlreadyClosedOrVoid extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID", "This LISA account has already been closed or been made void by HMRC")

case object ErrorAccountAlreadyVoided extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_VOID", "The LISA account is already void")

case object ErrorAccountAlreadyClosed extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_CLOSED", "The LISA account is already closed")

case object ErrorAccountAlreadyCancelled extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_CANCELLED", "The LISA account is already cancelled")

case object ErrorAccountAlreadyOpen extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_OPEN", "You cannot reinstate this account because it is already open")

case object ErrorAccountNotFound extends ErrorResponse(404, "INVESTOR_ACCOUNTID_NOT_FOUND", "The accountId does not match HMRC’s records")

case object ErrorBulkTransactionNotFound extends ErrorResponse(404, "TRANSACTION_NOT_FOUND", "No payments or debts exist for this date range")

case object ErrorTransferAccountDataNotProvided extends ErrorResponse(403, "TRANSFER_ACCOUNT_DATA_NOT_PROVIDED", "You must give a transferredFromAccountId, transferredFromLMRN and transferInDate when the creationReason is transferred")

case object ErrorTransferAccountDataProvided extends ErrorResponse(403, "TRANSFER_ACCOUNT_DATA_PROVIDED", "You must only give a transferredFromAccountId, transferredFromLMRN, and transferInDate when the creationReason is transferred")

case object ErrorLifeEventInappropriate extends ErrorResponse(403, "LIFE_EVENT_INAPPROPRIATE", "The life event conflicts with a previous life event reported")

case object ErrorInvalidLisaManager extends ErrorResponse(401,"UNAUTHORIZED", "lisaManagerReferenceNumber path parameter used does not match with an authorised LISA provider in HMRC’s records")

case object ErrorTransactionNotFound extends ErrorResponse(404, "BONUS_PAYMENT_TRANSACTION_NOT_FOUND", "transactionId does not match HMRC’s records")

case object ErrorGenericTransactionNotFound extends ErrorResponse(404, "TRANSACTION_NOT_FOUND", "transactionId does not match HMRC’s records")

case object ErrorWithdrawalNotFound extends ErrorResponse(404, "WITHDRAWAL_CHARGE_TRANSACTION_NOT_FOUND", "transactionId does not match HMRC’s records")

case object ErrorBonusClaimError extends ErrorResponse(403, "BONUS_CLAIM_ERROR", "The bonus amount given is above the maximum annual amount, or the qualifying deposits are above the maximum annual amount or the bonus claim does not equal the correct percentage of qualifying funds")

case object ErrorBonusSupersededAmountMismatch extends ErrorResponse(403, "SUPERSEDED_BONUS_CLAIM_AMOUNT_MISMATCH", "originalTransactionId and the originalBonusDueForPeriod amount do not match the information in the original bonus request")

case object ErrorBonusSupersededOutcomeError extends ErrorResponse(403, "SUPERSEDED_BONUS_REQUEST_OUTCOME_ERROR", "The calculation from your superseded bonus claim is incorrect")

case object ErrorBonusClaimTimescaleExceeded extends ErrorResponse(403, "BONUS_CLAIM_TIMESCALES_EXCEEDED", "The timescale for claiming a bonus has passed. The claim period lasts for 6 years and 14 days")

case object ErrorBonusHelpToBuyNotApplicable extends ErrorResponse(403, "HELP_TO_BUY_NOT_APPLICABLE", "Help to Buy is not applicable on this account")

case object ErrorNoSubscriptions extends ErrorResponse(403, "ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR", "A bonus payment is not possible because the account has no subscriptions for that tax year")

case object ErrorWithdrawalExists extends ErrorResponse(409, "WITHDRAWAL_CHARGE_ALREADY_EXISTS", "A withdrawal charge with these details has already been requested for this investor")

case object ErrorWithdrawalReportingError extends ErrorResponse(403, "WITHDRAWAL_REPORTING_ERROR", "The withdrawal charge does not equal 25% of the withdrawal amount")

case object ErrorWithdrawalAlreadySuperseded extends ErrorResponse(403, "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED", "This withdrawal charge has already been superseded")

case object ErrorWithdrawalSupersededAmountMismatch extends ErrorResponse(403, "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH", "originalTransactionId and the originalWithdrawalChargeAmount do not match the information in the original request")

case object ErrorWithdrawalSupersededOutcomeError extends ErrorResponse(403, "SUPERSEDED_WITHDRAWAL_CHARGE_OUTCOME_ERROR", "The calculation from your superseded withdrawal charge is incorrect")

case object ErrorWithdrawalTimescalesExceeded extends ErrorResponse(403, "WITHDRAWAL_CHARGE_TIMESCALES_EXCEEDED", "The timescale for reporting a withdrawal charge has passed. The claim period lasts for 6 years and 14 days")







case object ErrorAccountNotOpenLongEnough extends ErrorResponse(403, "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH", "The account has not been open for long enough")
case object ErrorFundReleaseOtherPropertyOnRecord extends ErrorResponse(403, "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD", "Another property purchase is already recorded")
case object ErrorFundReleaseMismatch extends ErrorResponse(403, "SUPERSEDED_FUND_RELEASE_MISMATCH_ERROR", "originalFundReleaseId and the originalEventDate do not match the information in the original request")
case object ErrorFundReleaseAlreadyExists extends ErrorResponse(409, "FUND_RELEASE_ALREADY_EXISTS", "The investor’s fund release has already been reported")
case object ErrorFundReleaseAlreadySuperseded extends ErrorResponse(409, "SUPERSEDED_FUND_RELEASE_ALREADY_SUPERSEDED", "This fund release has already been superseded")

case object ErrorLifeEventAlreadyExists extends ErrorResponse(409, "LIFE_EVENT_ALREADY_EXISTS", "The investor’s life event has already been reported")

object ErrorInvestorAlreadyExists {

  def apply(investorId: String) = {
    ErrorResponseWithId(409, "INVESTOR_ALREADY_EXISTS", "The investor already has a record with HMRC", investorId)
  }

}

object ErrorAccountAlreadyExists {

  def apply(accountId: String) = {
    ErrorResponseWithAccountId(409, "INVESTOR_ACCOUNT_ALREADY_EXISTS", "This investor already has a LISA account", accountId)
  }

}

object ErrorBonusClaimAlreadyExists {

  def apply(transactionId: String) = {
    ErrorResponseWithTransactionId(409, "BONUS_CLAIM_ALREADY_EXISTS", "The investor’s bonus payment has already been requested", transactionId)
  }

}

object ErrorBonusClaimAlreadySuperseded {

  def apply(transactionId: String) = {
    ErrorResponseWithTransactionId(409, "BONUS_CLAIM_ALREADY_SUPERSEDED", "This bonus claim has already been superseded", transactionId)
  }

}

case object ErrorLifeEventNotProvided extends ErrorResponse(403, "LIFE_EVENT_NOT_PROVIDED", "lifeEventId is required when the claimReason is a life event")




