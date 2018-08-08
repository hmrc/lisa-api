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
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
  "withdrawalAmount": 1000.00,
  "withdrawalChargeAmount": 250.00,
  "withdrawalChargeAmountYTD": 500.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Regular withdrawal",
  "supersededById": "2345678903"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Withdrawal transaction which has not been superseded</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567891
                    <br>
                    <strong>transactionId:</strong><br>2345678902
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
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
  "withdrawalAmount": 2000.00,
  "withdrawalChargeAmount": 500.00,
  "withdrawalChargeAmountYTD": 750.00,
  "fundsDeductedDuringWithdrawal": true,
  "withdrawalReason": "Superseded withdrawal",
  "supersede": {
    "originalTransactionId": "2345678901",
    "originalWithdrawalChargeAmount": 250.00,
    "transactionResult": 250.00,
    "reason": "Additional withdrawal",
    "automaticRecoveryAmount": 250.00
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
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>0000000404
                    <br>
                    <strong>transactionId:</strong><br>2345678901
                </p>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "The accountId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Transaction ID does not exist</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1000000404
                    <br>
                    <strong>transactionId:</strong><br>2345678901
                </p>
            </td>
            <td>
                <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "WITHDRAWAL_CHARGE_TRANSACTION_NOT_FOUND",
  "message": "The transactionId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>