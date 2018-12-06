<table>
    <col width="40%">
    <col width="60%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>
                <p>Withdrawal transaction which has been superseded</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                    <br>
                    <strong>transactionId:</strong><br>2345678901
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "automaticRecoveryAmount": 250,
  "withdrawalAmount": 1000,
  "withdrawalChargeAmount": 250,
  "withdrawalChargeAmountYTD": 500,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "supersededBy": "2345678903"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Withdrawal transaction which has not been superseded</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                    <br>
                    <strong>transactionId:</strong><br>2345678902
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "automaticRecoveryAmount": 250,
  "withdrawalAmount": 1000,
  "withdrawalChargeAmount": 250,
  "withdrawalChargeAmountYTD": 500,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Withdrawal transaction which supersedes another</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                    <br>
                    <strong>transactionId:</strong><br>2345678903
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "claimPeriodStartDate": "2017-12-06",
  "claimPeriodEndDate": "2018-01-05",
  "automaticRecoveryAmount": 250,
  "withdrawalAmount": 2000,
  "withdrawalChargeAmount": 500,
  "withdrawalChargeAmountYTD": 750,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250,
    "transactionResult": 250,
    "reason": "Additional withdrawal"
  }
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
                    <br>
                    <strong>transactionId:</strong><br>2345678901
                </p>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "lisaManagerReferenceNumber in the URL is in the wrong format"
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
                    <br>
                    <strong>transactionId:</strong><br>2345678901
                </p>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "accountId in the URL is in the wrong format"
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
                    <strong>accountId:</strong><br>1234567890
                    <br>
                    <strong>transactionId:</strong><br>1000000404
                </p>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "accountId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Transaction ID does not exist</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="#testing">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                    <br>
                    <strong>transactionId:</strong><br>0000000404
                </p>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "WITHDRAWAL_CHARGE_TRANSACTION_NOT_FOUND",
  "message": "transactionId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>