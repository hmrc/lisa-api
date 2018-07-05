<table>
    <col width="25%">
    <col width="35%">
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
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 9876543210</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "status": 200,
  "success": true,
  "data": {
    "message": "LISA account closed",
    "accountId": "9876543210"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 9876543210</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "Cancellation",
  "closureDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "status": 200,
  "success": true,
  "data": {
    "message": "LISA Account Closed",
    "accountId": "9876543210"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and account ID, but an invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: A12345<br>accountId: 9876543210</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
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
            <td>
                <p>Request with a valid payload and LISA Manager reference number, but an invalid account ID</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>accountId: 1234%3D5678
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "accountId in the URL is in the wrong format"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 9876543210</p></td>
            <td>
<pre class="code--block">
{
  "closureDate": "3000"
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
      "path": "/closureDate"
    },
    {
      "code": "MISSING_FIELD",
      "message": "This field is required",
      "path": "/accountClosureReason"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a closure date before 6 April 2017</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 9876543210</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-04-05"
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
      "message": "The closureDate cannot be before 6 April 2017",
      "path": "/closureDate"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been voided</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234560</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
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
            <td><p>Request for an account that has already been closed</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234561</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
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
            <td><p>Request to close an account with cancellation as the reason when the cancellation period is over</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234568</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "Cancellation",
  "closureDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "CANCELLATION_PERIOD_EXCEEDED",
  "message": "You cannot close the account with cancellation as the reason because the cancellation period is over"
}
</pre>
            </td>
        </tr>
        </tr>
         <tr>
            <td><p>Request to close an account with all funds withdrawn as the reason and it is still within the cancellation period</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234569</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "ACCOUNT_WITHIN_CANCELLATION_PERIOD",
  "message": "You cannot close the account with all funds withdrawn as the reason because it is within the cancellation period"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234562</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "The accountId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 9876543210<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
<pre class="code--block">
{
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-05-20"
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
    </tbody>
</table>