/*
 * Copyright 2026 HM Revenue & Customs
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

import org.scalatestplus.play.PlaySpec
import uk.gov.hmrc.lisaapi.helpers.BaseTestFixture

class ResponsesSpec extends BaseTestFixture {

  "EmptyJson" must {
    "have correct error code and message" in {
      EmptyJson.errorCode mustBe "BAD_REQUEST"
      EmptyJson.message   mustBe "Can't parse empty json"
    }

    "serialize to JSON" in {
      val json = EmptyJson.asJson
      (json \ "code").as[String]    mustBe "BAD_REQUEST"
      (json \ "message").as[String] mustBe "Can't parse empty json"
    }
  }

  "ErrorValidation" must {
    "create instance with all fields" in {
      val error = ErrorValidation("TEST_CODE", "Test message", Some("/test/path"))
      error.errorCode mustBe "TEST_CODE"
      error.message   mustBe "Test message"
      error.path      mustBe Some("/test/path")
    }

    "create instance without path (using default None)" in {
      val error = ErrorValidation("TEST_CODE", "Test message")
      error.errorCode mustBe "TEST_CODE"
      error.message   mustBe "Test message"
      error.path      mustBe None
    }

    "create instance with explicit None path" in {
      val error = ErrorValidation("TEST_CODE", "Test message", None)
      error.errorCode mustBe "TEST_CODE"
      error.message   mustBe "Test message"
      error.path      mustBe None
    }
  }

  "ErrorResponseWithErrors" must {
    "create instance with validation errors using ErrorBadRequest" in {
      val validationErrors = List(
        ErrorValidation("ERROR1", "First error", Some("/field1")),
        ErrorValidation("ERROR2", "Second error", Some("/field2"))
      )
      val response         = ErrorBadRequest(validationErrors)
      response.errorCode       mustBe "BAD_REQUEST"
      response.message         mustBe "Bad Request"
      response.httpStatusCode  mustBe 400
      response.errors.get.size mustBe 2
      response.errors          mustBe defined
    }

    "create instance with validation errors using ErrorForbidden" in {
      val validationErrors = List(
        ErrorValidation("ERROR1", "First error", Some("/field1"))
      )
      val response         = ErrorForbidden(validationErrors)
      response.errorCode       mustBe "FORBIDDEN"
      response.message         mustBe "There is a problem with the request data"
      response.httpStatusCode  mustBe 403
      response.errors.get.size mustBe 1
      response.errors          mustBe defined
    }

    "serialize ErrorBadRequest to JSON with errors" in {
      val validationErrors = List(ErrorValidation("ERR1", "Error message"))
      val response         = ErrorBadRequest(validationErrors)
      val json             = response.asJson
      (json \ "code").as[String] mustBe "BAD_REQUEST"
    }

    "serialize ErrorForbidden to JSON with errors" in {
      val validationErrors = List(ErrorValidation("ERR1", "Error message"))
      val response         = ErrorForbidden(validationErrors)
      val json             = response.asJson
      (json \ "code").as[String] mustBe "FORBIDDEN"
    }
  }

  "ErrorInvalidLisaManager" must {
    "have correct error code and message" in {
      ErrorInvalidLisaManager.errorCode mustBe "UNAUTHORIZED"
      ErrorInvalidLisaManager.message   mustBe "Enter a real lisaManagerReferenceNumber"
    }

    "serialize to JSON" in {
      val json = ErrorInvalidLisaManager.asJson
      (json \ "code").as[String] mustBe "UNAUTHORIZED"
    }
  }

  "ErrorUnauthorized" must {
    "have correct error code and message" in {
      ErrorUnauthorized.errorCode mustBe "UNAUTHORIZED"
      ErrorUnauthorized.message   mustBe "Bearer token is missing or not authorized"
    }

    "serialize to JSON" in {
      val json = ErrorUnauthorized.asJson
      (json \ "code").as[String] mustBe "UNAUTHORIZED"
    }
  }

  "ErrorBulkTransactionNotFoundV1" must {
    "have correct error code and message" in {
      ErrorBulkTransactionNotFoundV1.errorCode mustBe "PAYMENT_NOT_FOUND"
      ErrorBulkTransactionNotFoundV1.message   mustBe "No bonus payments have been made for this date range"
    }

    "serialize to JSON" in {
      val json = ErrorBulkTransactionNotFoundV1.asJson
      (json \ "code").as[String] mustBe "PAYMENT_NOT_FOUND"
    }
  }

  "ErrorCouldNotProcessWithdrawalRefund" must {
    "have correct error code and message" in {
      ErrorCouldNotProcessWithdrawalRefund.errorCode mustBe "COULD_NOT_PROCESS_WITHDRAWAL_CHARGE_REFUND"
      ErrorCouldNotProcessWithdrawalRefund.message   mustBe "Charge refund has been cancelled by HMRC"
    }

    "serialize to JSON" in {
      val json = ErrorCouldNotProcessWithdrawalRefund.asJson
      (json \ "code").as[String] mustBe "COULD_NOT_PROCESS_WITHDRAWAL_CHARGE_REFUND"
    }
  }

  // Test all error case objects
  "ErrorBadRequestInvalidPayload" must {
    "have correct properties" in {
      ErrorBadRequestInvalidPayload.errorCode      mustBe "INVALID_PAYLOAD"
      ErrorBadRequestInvalidPayload.httpStatusCode mustBe 400
    }
  }

  "ErrorBadRequestLmrn" must {
    "have correct properties" in {
      ErrorBadRequestLmrn.errorCode mustBe "BAD_REQUEST"
      ErrorBadRequestLmrn.message     must include("lisaManagerReferenceNumber")
    }
  }

  "ErrorBadRequestAccountId" must {
    "have correct properties" in {
      ErrorBadRequestAccountId.errorCode mustBe "BAD_REQUEST"
      ErrorBadRequestAccountId.message     must include("accountId")
    }
  }

  "ErrorBadRequestTransactionId" must {
    "have correct properties" in {
      ErrorBadRequestTransactionId.errorCode mustBe "BAD_REQUEST"
      ErrorBadRequestTransactionId.message     must include("transactionId")
    }
  }

  "ErrorBadRequestStart" must {
    "have correct properties" in {
      ErrorBadRequestStart.errorCode mustBe "BAD_REQUEST"
      ErrorBadRequestStart.message     must include("startDate")
    }
  }

  "ErrorBadRequestEnd" must {
    "have correct properties" in {
      ErrorBadRequestEnd.errorCode mustBe "BAD_REQUEST"
      ErrorBadRequestEnd.message     must include("endDate")
    }
  }

  "ErrorBadRequestStartEnd" must {
    "have correct properties" in {
      ErrorBadRequestStartEnd.errorCode mustBe "BAD_REQUEST"
      ErrorBadRequestStartEnd.message     must include("startDate and endDate")
    }
  }

  "ErrorBadRequestEndInFuture" must {
    "have correct properties" in {
      ErrorBadRequestEndInFuture.errorCode      mustBe "FORBIDDEN"
      ErrorBadRequestEndInFuture.httpStatusCode mustBe 403
    }
  }

  "ErrorBadRequestEndBeforeStart" must {
    "have correct properties" in {
      ErrorBadRequestEndBeforeStart.errorCode mustBe "FORBIDDEN"
      ErrorBadRequestEndBeforeStart.message     must include("cannot be before startDate")
    }
  }

  "ErrorBadRequestStartBefore6April2017" must {
    "have correct properties" in {
      ErrorBadRequestStartBefore6April2017.errorCode mustBe "FORBIDDEN"
      ErrorBadRequestStartBefore6April2017.message     must include("6 April 2017")
    }
  }

  "ErrorBadRequestOverYearBetweenStartAndEnd" must {
    "have correct properties" in {
      ErrorBadRequestOverYearBetweenStartAndEnd.errorCode mustBe "FORBIDDEN"
      ErrorBadRequestOverYearBetweenStartAndEnd.message     must include("more than a year")
    }
  }

  "ErrorApiNotAvailable" must {
    "have correct properties" in {
      ErrorApiNotAvailable.errorCode      mustBe "API_NOT_AVAILABLE"
      ErrorApiNotAvailable.httpStatusCode mustBe 403
    }
  }

  "ErrorGenericBadRequest" must {
    "have correct properties" in {
      ErrorGenericBadRequest.errorCode mustBe "BAD_REQUEST"
      ErrorGenericBadRequest.message   mustBe "Bad Request"
    }
  }

  "ErrorAcceptHeaderInvalid" must {
    "have correct properties" in {
      ErrorAcceptHeaderInvalid.errorCode      mustBe "ACCEPT_HEADER_INVALID"
      ErrorAcceptHeaderInvalid.httpStatusCode mustBe 406
    }
  }

  "ErrorAcceptHeaderVersionInvalid" must {
    "have correct properties" in {
      ErrorAcceptHeaderVersionInvalid.errorCode mustBe "ACCEPT_HEADER_INVALID"
      ErrorAcceptHeaderVersionInvalid.message     must include("invalid version")
    }
  }

  "ErrorAcceptHeaderContentInvalid" must {
    "have correct properties" in {
      ErrorAcceptHeaderContentInvalid.errorCode mustBe "ACCEPT_HEADER_INVALID"
      ErrorAcceptHeaderContentInvalid.message     must include("invalid content type")
    }
  }

  "ErrorInternalServerError" must {
    "have correct properties" in {
      ErrorInternalServerError.errorCode      mustBe "INTERNAL_SERVER_ERROR"
      ErrorInternalServerError.httpStatusCode mustBe 500
    }
  }

  "ErrorServiceUnavailable" must {
    "have correct properties" in {
      ErrorServiceUnavailable.errorCode      mustBe "SERVER_ERROR"
      ErrorServiceUnavailable.httpStatusCode mustBe 503
    }
  }

  "ErrorInvestorNotFound" must {
    "have correct properties" in {
      ErrorInvestorNotFound.errorCode      mustBe "INVESTOR_NOT_FOUND"
      ErrorInvestorNotFound.httpStatusCode mustBe 403
    }
  }

  "ErrorLifeEventIdNotFound" must {
    "have correct properties" in {
      ErrorLifeEventIdNotFound.errorCode      mustBe "LIFE_EVENT_NOT_FOUND"
      ErrorLifeEventIdNotFound.httpStatusCode mustBe 404
    }
  }

  "ErrorInvestorNotEligible" must {
    "have correct properties" in {
      ErrorInvestorNotEligible.errorCode      mustBe "INVESTOR_ELIGIBILITY_CHECK_FAILED"
      ErrorInvestorNotEligible.httpStatusCode mustBe 403
    }
  }

  "ErrorInvestorComplianceCheckFailedCreateTransfer" must {
    "have correct properties" in {
      ErrorInvestorComplianceCheckFailedCreateTransfer.errorCode mustBe "INVESTOR_COMPLIANCE_CHECK_FAILED"
      ErrorInvestorComplianceCheckFailedCreateTransfer.message     must include("create or transfer")
    }
  }

  "ErrorInvestorComplianceCheckFailedReinstate" must {
    "have correct properties" in {
      ErrorInvestorComplianceCheckFailedReinstate.errorCode mustBe "INVESTOR_COMPLIANCE_CHECK_FAILED"
      ErrorInvestorComplianceCheckFailedReinstate.message     must include("reinstate")
    }
  }

  "ErrorAccountCancellationPeriodExceeded" must {
    "have correct properties" in {
      ErrorAccountCancellationPeriodExceeded.errorCode      mustBe "CANCELLATION_PERIOD_EXCEEDED"
      ErrorAccountCancellationPeriodExceeded.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountWithinCancellationPeriod" must {
    "have correct properties" in {
      ErrorAccountWithinCancellationPeriod.errorCode      mustBe "ACCOUNT_WITHIN_CANCELLATION_PERIOD"
      ErrorAccountWithinCancellationPeriod.httpStatusCode mustBe 403
    }
  }

  "ErrorPreviousAccountDoesNotExist" must {
    "have correct properties" in {
      ErrorPreviousAccountDoesNotExist.errorCode      mustBe "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST"
      ErrorPreviousAccountDoesNotExist.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountAlreadyClosedOrVoid" must {
    "have correct properties" in {
      ErrorAccountAlreadyClosedOrVoid.errorCode      mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID"
      ErrorAccountAlreadyClosedOrVoid.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountAlreadyVoided" must {
    "have correct properties" in {
      ErrorAccountAlreadyVoided.errorCode      mustBe "INVESTOR_ACCOUNT_ALREADY_VOID"
      ErrorAccountAlreadyVoided.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountAlreadyClosed" must {
    "have correct properties" in {
      ErrorAccountAlreadyClosed.errorCode      mustBe "INVESTOR_ACCOUNT_ALREADY_CLOSED"
      ErrorAccountAlreadyClosed.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountAlreadyCancelled" must {
    "have correct properties" in {
      ErrorAccountAlreadyCancelled.errorCode      mustBe "INVESTOR_ACCOUNT_ALREADY_CANCELLED"
      ErrorAccountAlreadyCancelled.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountAlreadyOpen" must {
    "have correct properties" in {
      ErrorAccountAlreadyOpen.errorCode mustBe "INVESTOR_ACCOUNT_ALREADY_OPEN"
      ErrorAccountAlreadyOpen.message     must include("already open")
    }
  }

  "ErrorAccountNotFound" must {
    "have correct properties" in {
      ErrorAccountNotFound.errorCode      mustBe "INVESTOR_ACCOUNTID_NOT_FOUND"
      ErrorAccountNotFound.httpStatusCode mustBe 404
    }
  }

  "ErrorBulkTransactionNotFoundV2" must {
    "have correct properties" in {
      ErrorBulkTransactionNotFoundV2.errorCode      mustBe "TRANSACTION_NOT_FOUND"
      ErrorBulkTransactionNotFoundV2.httpStatusCode mustBe 404
    }
  }

  "ErrorTransferAccountDataNotProvided" must {
    "have correct properties" in {
      ErrorTransferAccountDataNotProvided.errorCode      mustBe "TRANSFER_ACCOUNT_DATA_NOT_PROVIDED"
      ErrorTransferAccountDataNotProvided.httpStatusCode mustBe 403
    }
  }

  "ErrorTransferAccountDataProvided" must {
    "have correct properties" in {
      ErrorTransferAccountDataProvided.errorCode      mustBe "TRANSFER_ACCOUNT_DATA_PROVIDED"
      ErrorTransferAccountDataProvided.httpStatusCode mustBe 403
    }
  }

  "ErrorLifeEventInappropriate" must {
    "have correct properties" in {
      ErrorLifeEventInappropriate.errorCode      mustBe "LIFE_EVENT_INAPPROPRIATE"
      ErrorLifeEventInappropriate.httpStatusCode mustBe 403
    }
  }

  "ErrorBonusPaymentTransactionNotFound" must {
    "have correct properties" in {
      ErrorBonusPaymentTransactionNotFound.errorCode      mustBe "BONUS_PAYMENT_TRANSACTION_NOT_FOUND"
      ErrorBonusPaymentTransactionNotFound.httpStatusCode mustBe 404
    }
  }

  "ErrorTransactionNotFound" must {
    "have correct properties" in {
      ErrorTransactionNotFound.errorCode      mustBe "TRANSACTION_NOT_FOUND"
      ErrorTransactionNotFound.httpStatusCode mustBe 404
    }
  }

  "ErrorWithdrawalNotFound" must {
    "have correct properties" in {
      ErrorWithdrawalNotFound.errorCode      mustBe "WITHDRAWAL_CHARGE_TRANSACTION_NOT_FOUND"
      ErrorWithdrawalNotFound.httpStatusCode mustBe 404
    }
  }

  "ErrorBonusClaimError" must {
    "have correct properties" in {
      ErrorBonusClaimError.errorCode      mustBe "BONUS_CLAIM_ERROR"
      ErrorBonusClaimError.httpStatusCode mustBe 403
    }
  }

  "ErrorBonusSupersededAmountMismatch" must {
    "have correct properties" in {
      ErrorBonusSupersededAmountMismatch.errorCode      mustBe "SUPERSEDED_BONUS_CLAIM_AMOUNT_MISMATCH"
      ErrorBonusSupersededAmountMismatch.httpStatusCode mustBe 403
    }
  }

  "ErrorBonusSupersededOutcomeError" must {
    "have correct properties" in {
      ErrorBonusSupersededOutcomeError.errorCode      mustBe "SUPERSEDED_BONUS_REQUEST_OUTCOME_ERROR"
      ErrorBonusSupersededOutcomeError.httpStatusCode mustBe 403
    }
  }

  "ErrorBonusClaimTimescaleExceeded" must {
    "have correct properties" in {
      ErrorBonusClaimTimescaleExceeded.errorCode mustBe "BONUS_CLAIM_TIMESCALES_EXCEEDED"
      ErrorBonusClaimTimescaleExceeded.message     must include("6 years and 14 days")
    }
  }

  "ErrorBonusHelpToBuyNotApplicable" must {
    "have correct properties" in {
      ErrorBonusHelpToBuyNotApplicable.errorCode      mustBe "HELP_TO_BUY_NOT_APPLICABLE"
      ErrorBonusHelpToBuyNotApplicable.httpStatusCode mustBe 403
    }
  }

  "ErrorNoSubscriptions" must {
    "have correct properties" in {
      ErrorNoSubscriptions.errorCode      mustBe "ACCOUNT_ERROR_NO_SUBSCRIPTIONS_THIS_TAX_YEAR"
      ErrorNoSubscriptions.httpStatusCode mustBe 403
    }
  }

  "ErrorWithdrawalReportingError" must {
    "have correct properties" in {
      ErrorWithdrawalReportingError.errorCode mustBe "WITHDRAWAL_REPORTING_ERROR"
      ErrorWithdrawalReportingError.message     must include("20%")
    }
  }

  "ErrorWithdrawalSupersededAmountMismatch" must {
    "have correct properties" in {
      ErrorWithdrawalSupersededAmountMismatch.errorCode      mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH"
      ErrorWithdrawalSupersededAmountMismatch.httpStatusCode mustBe 403
    }
  }

  "ErrorWithdrawalSupersededOutcomeError" must {
    "have correct properties" in {
      ErrorWithdrawalSupersededOutcomeError.errorCode      mustBe "SUPERSEDED_WITHDRAWAL_CHARGE_OUTCOME_ERROR"
      ErrorWithdrawalSupersededOutcomeError.httpStatusCode mustBe 403
    }
  }

  "ErrorWithdrawalTimescalesExceeded" must {
    "have correct properties" in {
      ErrorWithdrawalTimescalesExceeded.errorCode mustBe "WITHDRAWAL_CHARGE_TIMESCALES_EXCEEDED"
      ErrorWithdrawalTimescalesExceeded.message     must include("6 years and 14 days")
    }
  }

  "ErrorLifeEventMismatch" must {
    "have correct properties" in {
      ErrorLifeEventMismatch.errorCode      mustBe "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR"
      ErrorLifeEventMismatch.httpStatusCode mustBe 403
    }
  }

  "ErrorAccountNotOpenLongEnough" must {
    "have correct properties" in {
      ErrorAccountNotOpenLongEnough.errorCode      mustBe "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH"
      ErrorAccountNotOpenLongEnough.httpStatusCode mustBe 403
    }
  }

  "ErrorFundReleaseOtherPropertyOnRecord" must {
    "have correct properties" in {
      ErrorFundReleaseOtherPropertyOnRecord.errorCode      mustBe "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD"
      ErrorFundReleaseOtherPropertyOnRecord.httpStatusCode mustBe 403
    }
  }

  "ErrorInvalidDataProvided" must {
    "have correct properties" in {
      ErrorInvalidDataProvided.errorCode      mustBe "INVALID_DATA_PROVIDED"
      ErrorInvalidDataProvided.httpStatusCode mustBe 403
    }
  }

  "ErrorExtensionOneNotApproved" must {
    "have correct properties" in {
      ErrorExtensionOneNotApproved.errorCode      mustBe "FIRST_EXTENSION_NOT_APPROVED"
      ErrorExtensionOneNotApproved.httpStatusCode mustBe 403
    }
  }

  "ErrorFundReleaseNotFound" must {
    "have correct properties" in {
      ErrorFundReleaseNotFound.errorCode      mustBe "FUND_RELEASE_NOT_FOUND"
      ErrorFundReleaseNotFound.httpStatusCode mustBe 404
    }
  }

  "ErrorLifeEventNotProvided" must {
    "have correct properties" in {
      ErrorLifeEventNotProvided.errorCode      mustBe "LIFE_EVENT_NOT_PROVIDED"
      ErrorLifeEventNotProvided.httpStatusCode mustBe 403
    }
  }

  // Test case classes with parameters
  "ErrorLifeEventAlreadyExists" must {
    "create with lifeEventId" in {
      val error = ErrorLifeEventAlreadyExists("12345")
      error.errorCode      mustBe "LIFE_EVENT_ALREADY_EXISTS"
      error.lifeEventID    mustBe "12345"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorLifeEventAlreadySuperseded" must {
    "create with lifeEventId" in {
      val error = ErrorLifeEventAlreadySuperseded("67890")
      error.errorCode      mustBe "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED"
      error.lifeEventID    mustBe "67890"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorFundReleaseSuperseded" must {
    "create with lifeEventId" in {
      val error = ErrorFundReleaseSuperseded("fund123")
      error.errorCode      mustBe "FUND_RELEASE_SUPERSEDED"
      error.lifeEventID    mustBe "fund123"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorExtensionOneAlreadyApproved" must {
    "create with lifeEventId" in {
      val error = ErrorExtensionOneAlreadyApproved("ext123")
      error.errorCode      mustBe "FIRST_EXTENSION_ALREADY_APPROVED"
      error.lifeEventID    mustBe "ext123"
      error.httpStatusCode mustBe 403
    }
  }

  "ErrorExtensionTwoAlreadyApproved" must {
    "create with lifeEventId" in {
      val error = ErrorExtensionTwoAlreadyApproved("ext456")
      error.errorCode      mustBe "SECOND_EXTENSION_ALREADY_APPROVED"
      error.lifeEventID    mustBe "ext456"
      error.httpStatusCode mustBe 403
    }
  }

  "ErrorResponseWithTransactionId" must {
    "create with transaction id" in {
      val error = ErrorResponseWithTransactionId(403, "TEST_CODE", "Test message", "txn123")
      error.errorCode      mustBe "TEST_CODE"
      error.transactionId  mustBe "txn123"
      error.httpStatusCode mustBe 403
    }
  }

  "ErrorResponseWithAccountId" must {
    "create with account id" in {
      val error = ErrorResponseWithAccountId(409, "TEST_CODE", "Test message", "acc123")
      error.errorCode      mustBe "TEST_CODE"
      error.accountId      mustBe "acc123"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorResponseWithId" must {
    "create with id" in {
      val error = ErrorResponseWithId(409, "TEST_CODE", "Test message", "id123")
      error.errorCode      mustBe "TEST_CODE"
      error.id             mustBe "id123"
      error.httpStatusCode mustBe 409
    }
  }

  // Test object factory methods
  "ErrorWithdrawalAlreadySuperseded" must {
    "create with transaction id" in {
      val error = ErrorWithdrawalAlreadySuperseded("txn123")
      error.errorCode      mustBe "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED"
      error.transactionId  mustBe "txn123"
      error.httpStatusCode mustBe 403
    }
  }

  "ErrorWithdrawalExists" must {
    "create with transaction id" in {
      val error = ErrorWithdrawalExists("txn456")
      error.errorCode      mustBe "WITHDRAWAL_CHARGE_ALREADY_EXISTS"
      error.transactionId  mustBe "txn456"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorInvestorAlreadyExists" must {
    "create with investor id" in {
      val error = ErrorInvestorAlreadyExists("inv123")
      error.errorCode      mustBe "INVESTOR_ALREADY_EXISTS"
      error.id             mustBe "inv123"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorAccountAlreadyExists" must {
    "create with account id" in {
      val error = ErrorAccountAlreadyExists("acc789")
      error.errorCode      mustBe "INVESTOR_ACCOUNT_ALREADY_EXISTS"
      error.accountId      mustBe "acc789"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorBonusClaimAlreadyExists" must {
    "create with transaction id" in {
      val error = ErrorBonusClaimAlreadyExists("txn789")
      error.errorCode      mustBe "BONUS_CLAIM_ALREADY_EXISTS"
      error.transactionId  mustBe "txn789"
      error.httpStatusCode mustBe 409
    }
  }

  "ErrorBonusClaimAlreadySuperseded" must {
    "create with transaction id" in {
      val error = ErrorBonusClaimAlreadySuperseded("txn999")
      error.errorCode      mustBe "BONUS_CLAIM_ALREADY_SUPERSEDED"
      error.transactionId  mustBe "txn999"
      error.httpStatusCode mustBe 409
    }
  }

  // Test asResult method
  "ErrorResponse" must {
    "convert to Result with correct status code" in {
      val result = ErrorInternalServerError.asResult
      result.header.status mustBe 500
    }
  }

}
