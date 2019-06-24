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
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "accountId": "1234567890"
}
</pre>
            </td>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "status": 200,
  "success": true,
  "data": {
    "message": "This account has been reinstated",
    "accountId": "1234567890"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and account ID, but an invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: A12345</p></td>
            <td>
<pre class="code--block">
{
  "accountId": "1234567890"
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
            <td>
                <p>Request with a invalid payload</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a>
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "accountId": "1234=5678"
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
        "code": "INVALID_FORMAT",
        "message": "Invalid format has been used",
        "path": "/accountId"
      }
    ]
  }
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that is open or active</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "accountId": "2000000403"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_OPEN",
  "message": "The account already has a status of Open"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that is closed with a closure reason as transferred out</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "accountId": "0000000403"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",
  "message": "You cannot reinstate this account because it was closed with a closure reason of transferred out"
}
</pre>
            </td>
        </tr>
         <tr>
            <td><p>Request for an account that is closed with a closure reason as cancelled</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "accountId": "1000000403"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CANCELLED",
  "message": "You cannot reinstate this account because it was closed with a closure reason of cancellation"
}
</pre>
            </td>
        </tr>
         <tr>
            <td><p>Request for an account that is closed with a closure reason as cancelled</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "accountId": "3000000403"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_COMPLIANCE_CHECK_FAILED",
  "message": "You cannot reinstate this account because the investor has failed a compliance check"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "accountId": "0000000404"
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
    </tbody>
</table>