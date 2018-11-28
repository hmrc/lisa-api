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
            <td><p>Successful initial return</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
    "lifeEventId": "7890000001"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Successful superseded return</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 65,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 65,
  "supersede": {
    "originalLifeEventId": "7890000001",
    "originalEventDate": "2018-04-05"
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
    "message": "Life event superseded",
    "lifeEventId": "7890000002"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890</p></td>
                        <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
            <td><p>Invalid account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234%3D5678</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
            <td><p>Invalid and/or missing data</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "May 2018",
  "taxYear": "2018",
  "marketValueCash": 0,
  "marketValueStocksAndShares": 10.1,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
      "code": "INVALID_DATA_TYPE",
      "message": "Invalid data type has been used",
      "path": "/taxYear"
    },
    {
      "code": "INVALID_DATA_TYPE",
      "message": "Invalid data type has been used",
      "path": "/marketValueStocksAndShares"
    },
    {
      "code": "INVALID_DATE",
      "message": "Date is invalid",
      "path": "/eventDate"
    },
    {
      "code": "MISSING_FIELD",
      "message": "This field is required",
      "path": "/isaManagerName"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Account cancelled</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 2000000403</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
            <td><p>Account void</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 3000000403</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
            <td>
                <p>Supersede details mismatch</p>
                <p class="code--block">
                lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a>
                <br>
                accountId: 5000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 65,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 65,
  "supersede": {
    "originalLifeEventId": "7890000001",
    "originalEventDate": "2018-04-04"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
    "code": "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR",
    "message": "originalLifeEventId and the originalEventDate do not match the information in the original request"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Account could not be found</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 0000000404</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
            <td><p>Invalid Accept header</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
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
            <td>
                <p>Life event already superseded</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a>
                    <br>
                    accountId: 1000000409
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED",
  "message": "This life event has already been superseded"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Life event already exists</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 0000000409</p></td>
            <td>
<pre class="code--block">
{
  "eventDate": "2018-04-05",
  "isaManagerName": "ISA Manager",
  "taxYear": 2018,
  "marketValueCash": 0,
  "marketValueStocksAndShares": 55,
  "annualSubsCash": 0,
  "annualSubsStocksAndShares": 55
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_ALREADY_EXISTS",
  "message": "The investor’s life event has already been reported"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>
