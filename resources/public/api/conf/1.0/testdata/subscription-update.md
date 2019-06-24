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
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"<br>
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
  {
    "data": {
      "message": "Successfully updated the firstSubscriptionDate for the LISA account",
      "code": "UPDATED",
      "accountId": "1234567890"
    }
    "success": true,
    "status": 200
  }
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567891</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "data": {
    "message": "Successfully updated the firstSubscriptionDate for the LISA account and changed the account status to void because the investor has another account with an earlier firstSubscriptionDate",
    "code": "UPDATED_AND_ACCOUNT_VOID",
    "accountId": "1234567891"
  }
  "success": true,
  "status": 200
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and account ID, but an invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: A12345<br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and LISA Manager reference number, but an invalid and account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234%3D5678</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter accountId in the correct format, like ABC12345"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "3000-01-01"
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
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a first subscription date before 6 April 2017</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-04-05"
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
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 0000000901</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
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
            <td><p>Request for an account that has already been void</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 0000000902</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
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
            <td><p>Request containing an account ID that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 0000000404</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "Enter a real accountId"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
<pre class="code--block">
{
  "firstSubscriptionDate": "2017-05-20"
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