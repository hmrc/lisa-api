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
            <td>
                <p>Create request with a valid payload and LISA Manager reference number</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 0123456789
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "1111111111",
  "eventDate": "2017-05-10",
  "eventType": "Purchase extension 1"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Extension created",
    "extensionId": "2222222222"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with a valid payload and an invalid LISA Manager reference number</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: 123456
                    <br>
                    accountId: 0123456789
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "1111111111",
  "eventDate": "2017-05-10",
  "eventType": "Purchase extension 1"
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
                <p>Request containing invalid and/or missing data</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 0123456789
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "10-05-2017",
  "eventType": "Purchase extension one"
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
      "path": "/eventDate"
    },
    {
      "code": "INVALID_FORMAT",
      "message": "Invalid format has been used",
      "path": "/eventType"
    },
    {
      "code": "MISSING_FIELD",
      "message": "This field is required",
      "path": "/fundReleaseId"
    }
  ]
}
</pre>
            </td>
        </tr>
    </tbody>
</table>