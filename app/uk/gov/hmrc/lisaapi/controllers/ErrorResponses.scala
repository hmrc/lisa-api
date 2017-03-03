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

case class ErrorResponseWithId(
                                httpStatusCode: Int,
                                errorCode: String,
                                message: String,
                                id: String
                              )

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

case object ErrorPreviousAccountDoesNotExist extends ErrorResponse(403, "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST", "The transferredFromAccountID and transferredFromLMRN given don’t match with an account on HMRC’s records")

case object ErrorAccountAlreadyExists extends ErrorResponse(409, "INVESTOR_ACCOUNT_ALREADY_EXISTS", "The LISA account already exists")

case object ErrorAccountAlreadyClosed extends ErrorResponse(403, "INVESTOR_ACCOUNT_ALREADY_CLOSED", "The LISA account is already closed")

case object ErrorAccountNotFound extends ErrorResponse(404, "INVESTOR_ACCOUNTID_NOT_FOUND", "The accountID given does not match with HMRC’s records")

object ErrorInvestorAlreadyExists {

  def apply(investorId: String) = {
    ErrorResponseWithId(409, "INVESTOR_ALREADY_EXISTS", "The investor already has a record with HMRC ", investorId)
  }

}