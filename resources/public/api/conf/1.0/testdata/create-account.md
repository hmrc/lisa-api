<table>
    <col width="25%">
    <col width="40%">
    <col width="35%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Request Payload</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Create request with a valid payload and LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Account created",
    "accountId": "1234567890"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Transfer request with a valid payload and LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "Transferred",
  "accountId": "1234567891",
  "firstSubscriptionDate": "2017-04-06",
  "transferAccount": {
    "transferredFromAccountId": "8765432100",
    "transferredFromLMRN": "Z654321",
    "transferInDate": "2017-04-06"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Account transferred",
    "accountId": "1234567891"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and an invalid LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: A123456</p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "lisaManagerReferenceNumber in the URL is in the wrong format"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543",
  "creationReason": "New",
  "firstSubscriptionDate": "2011"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Bad Request",
  "errors": [
    {
      "code": "INVALID_DATE",
      "message": "Date is invalid",
      "path": "/firstSubscriptionDate"
    },
    {
      "code": "INVALID_FORMAT",
      "message": "Invalid format has been used",
      "path": "/investorId"
    },
    {
      "code": "MISSING_FIELD",
      "message": "This field is required",
      "path": "/accountId"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing dates before 6 April 2017</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "Transferred",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2016-04-06",
  "transferAccount": {
    "transferredFromAccountId": "8765432100",
    "transferredFromLMRN": "Z654321",
    "transferInDate": "2016-04-06"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FORBIDDEN",
  "message": "There is a problem with the request data",
  "errors": [
    {
      "code": "INVALID_DATE",
      "message": "The firstSubscriptionDate cannot be before 6 April 2017",
      "path": "/firstSubscriptionDate"
    },
    {
      "code": "INVALID_DATE",
      "message": "The transferInDate cannot be before 6 April 2017",
      "path": "/transferAccount/transferInDate"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing investor details which cannot be found</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "1234567890",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_NOT_FOUND",
  "message": "The investor details given do not match with HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an investor who is not eligible for a LISA account</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "1234567891",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ELIGIBILITY_CHECK_FAILED",
  "message": "The investor is not eligible for a LISA account"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an investor who has not passed the compliance check</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "1234567892",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_COMPLIANCE_CHECK_FAILED",
  "message": "You cannot create or transfer a LISA account because the investor has failed a compliance check"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Transfer request containing transfer details which cannot be found in HMRC's records</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "1234567889",
  "creationReason": "Transferred",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06",
  "transferAccount": {
    "transferredFromAccountId": "8765432100",
    "transferredFromLMRN": "Z654321",
    "transferInDate": "2017-04-06"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST",
  "message": "The transferredFromAccountId and transferredFromLMRN given do not match an account on HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Transfer request without transfer details</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "Transferred",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "TRANSFER_ACCOUNT_DATA_NOT_PROVIDED",
  "message": "You must give a transferredFromAccountId, transferredFromLMRN and transferInDate when the creationReason is transferred"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Create request containing transfer details</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06",
  "transferAccount": {
    "transferredFromAccountId": "8765432100",
    "transferredFromLMRN": "Z654321",
    "transferInDate": "2017-04-06"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "TRANSFER_ACCOUNT_DATA_PROVIDED",
  "message": "You must only give a transferredFromAccountId, transferredFromLMRN, and transferInDate when the creationReason is transferred"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a LISA account which has already been closed</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "0000000403",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",
  "message": "The LISA account is already closed"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a LISA account which has already been voided</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "1000000403",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_VOID",
  "message": "The LISA account is already void"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
<pre class="code--block">
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "The accept header is missing or invalid"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pre-existing account</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorId": "1234567899",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_EXISTS",
  "message": "This investor already has a LISA account",
  "accountId": "1234567890"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>