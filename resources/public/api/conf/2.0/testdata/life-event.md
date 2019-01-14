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
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Life event created",
    "lifeEventId": "1234567891"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and accountID, but an invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890</p></td>
                        <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
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
            <td><p>Request with a valid payload and LISA Manager reference number, but an invalid account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234%3D5678</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
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
            <td><p>Request containing invalid and/or missing data</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "Invalid Event Type"
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
      "code": "MISSING_FIELD",
      "message": "This field is required",
      "path": "/eventDate"
    },
    {
      "code": "INVALID_FORMAT",
      "message": "Invalid format has been used",
      "path": "/eventType"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an event date before 6 April 2017</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-05"
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
      "message": "The eventDate cannot be before 6 April 2017",
      "path": "/eventDate"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a life event that conflicts with a previously reported event</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 0000000403</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_INAPPROPRIATE",
  "message": "The life event conflicts with a previous life event reported"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1000000403</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
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
            <td><p>Request for an account that has already been cancelled</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 2000000403</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CANCELLED",
  "message": "The LISA account is already cancelled"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been void</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 3000000403</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
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
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "The accountId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
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
            <td><p>Request containing an already reported event</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 0000000409</p></td>
            <td>
<pre class="code--block">
{
  "eventType": "LISA Investor Terminal Ill Health",
  "eventDate": "2017-04-20"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_ALREADY_EXISTS",
  "message": "The investor’s life event has already been reported",
  "lifeEventId": "1234567891"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>
