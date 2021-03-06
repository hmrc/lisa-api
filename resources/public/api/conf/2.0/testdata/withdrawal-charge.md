<table>
    <col width="20%">
    <col width="40%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Request Payload</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <p>Unauthorised withdrawal transaction created</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Unauthorised withdrawal transaction created",
    "transactionId": "2345678901"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Unauthorised withdrawal transaction created - late notification</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567891
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "success": true,
  "status": 201,
  "data": {
    "transactionId": "2345678902",
    "message": "Unauthorised withdrawal transaction created - late notification"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Unauthorised withdrawal transaction superseded</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 2000.00,
  "withdrawalChargeAmount": 500.00,
  "withdrawalChargeAmountYTD": 750.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "automaticRecoveryAmount": 500.00,
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250.00,
    "transactionResult": 250.00,
    "reason": "Additional withdrawal"
  }
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "success": true,
  "status": 201,
  "data": {
    "transactionId": "2345678903",
    "message": "Unauthorised withdrawal transaction superseded"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Invalid and/or missing data</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": 6,
  "claimPeriodEndDate": "1st May",
  "withdrawalAmount": 1000.001,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal.",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Bad Request",
  "errors": [
    {
      "code": "INVALID_DATA_TYPE",
      "message": "Invalid data type has been used",
      "path": "/claimPeriodStartDate"
    },
    {
      "code": "INVALID_DATE",
      "message": "Date is invalid",
      "path": "/claimPeriodEndDate"
    },
    {
      "code": "INVALID_MONETARY_AMOUNT",
      "message": "Amount cannot be negative, and can only have up to 2 decimal places",
      "path": "/withdrawalAmount"
    },
    {
      "code": "MISSING_FIELD",
      "message": "This field is required",
      "path": "/withdrawalChargeAmount"
    },
    {
      "code": "INVALID_FORMAT",
      "message": "Invalid format has been used",
      "path": "/withdrawalReason"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Invalid LISA Manager Reference Number</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br> 123456
                    <br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Invalid Account ID</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234%3D5678
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter accountId in the correct format, like ABC12345"
}
</pre>
            </td>
        </tr>

        <tr>
            <td>
                <p>Invalid monetary amounts and/or invalid dates</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-05",
  "claimPeriodEndDate": "2018-12-06",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 251.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
    "code": "FORBIDDEN",
    "message": "There is a problem with the request data",
    "errors": [
        {
            "code": "INVALID_DATE",
            "message": "The claimPeriodStartDate must be the 6th day of the month",
            "path": "/claimPeriodStartDate"
        },
        {
            "code": "INVALID_DATE",
            "message": "The claimPeriodEndDate must be the 5th day of the month which occurs after the claimPeriodStartDate",
            "path": "/claimPeriodEndDate"
        },
        {
            "code": "INVALID_MONETARY_AMOUNT",
            "message": "automaticRecoveryAmount cannot be more than withdrawalChargeAmount",
            "path": "/automaticRecoveryAmount"
        },
        {
            "code": "AMOUNT_MISMATCH",
            "message": "automaticRecoveryAmount and withdrawalChargeAmount must be the same",
            "path": "/automaticRecoveryAmount"
        }
    ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>This LISA account is already cancelled</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CANCELLED",
  "message": "The LISA account is already cancelled"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>This LISA account is already void</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>2000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_VOID",
  "message": "The LISA account is already void"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>A superseded withdrawal report cannot be matched to the original</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>3000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 2000.00,
  "withdrawalChargeAmount": 500.00,
  "withdrawalChargeAmountYTD": 750.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "automaticRecoveryAmount": 500.00,
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250.00,
    "transactionResult": 250.00,
    "reason": "Additional withdrawal"
  }
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "SUPERSEDED_WITHDRAWAL_CHARGE_ID_AMOUNT_MISMATCH",
  "message": "originalTransactionId and the originalWithdrawalChargeAmount do not match the information in the original request"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>The withdrawal charge has already been superseded</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>4000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 2000.00,
  "withdrawalChargeAmount": 500.00,
  "withdrawalChargeAmountYTD": 750.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "automaticRecoveryAmount": 500.00,
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250.00,
    "transactionResult": 250.00,
    "reason": "Additional withdrawal"
  }
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "WITHDRAWAL_CHARGE_ALREADY_SUPERSEDED",
  "message": "This withdrawal charge has already been superseded",
  "transactionId": "2345678901"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>The calculation from your superseded withdrawal charge is incorrect</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>5000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 2000.00,
  "withdrawalChargeAmount": 500.00,
  "withdrawalChargeAmountYTD": 750.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "automaticRecoveryAmount": 500.00,
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250.00,
    "transactionResult": 250.00,
    "reason": "Additional withdrawal"
  }
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "SUPERSEDED_WITHDRAWAL_CHARGE_OUTCOME_ERROR",
  "message": "The calculation from your superseded withdrawal charge is incorrect"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>The withdrawal charge as a percentage of the withdrawal amount is incorrect. 
                For withdrawals made between 06/03/2020 and 05/04/2021 the withdrawal charge is 20%. For all other withdrawals it is 25%.</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>6000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 2000.00,
  "withdrawalChargeAmount": 500.00,
  "withdrawalChargeAmountYTD": 750.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "automaticRecoveryAmount": 500.00,
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250.00,
    "transactionResult": 250.00,
    "reason": "Additional withdrawal"
  }
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "WITHDRAWAL_REPORTING_ERROR",
  "message": "The withdrawal charge does not equal 25% of the withdrawal amount"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Account ID does not exist</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>0000000404
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "Enter a real accountId"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>The accept header is invalid</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                    <br>
                    <br>
                    <strong>Accept:</strong><br> application/vnd.hmrc.1.0
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
<pre class="code--block">
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "The accept header is missing or invalid"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Withdrawal charge has already been reported</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>0000000409
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "automaticRecoveryAmount": 250.00
}
</pre>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "WITHDRAWAL_CHARGE_ALREADY_EXISTS",
  "message": "A withdrawal charge with these details has already been requested for this investor",
  "transactionId": "2345678901"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>
