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
                <p>First purchase extension</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
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
    "extensionId": "6789000001"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Superseded first purchase extension</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "2017-05-11",
  "eventType": "Extension one",
  "supersede": {
    "originalEventDate": "2017-05-10",
    "originalExtensionId": "6789000001"
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
    "message": "Extension superseded",
    "extensionId": "6789000002"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Second purchase extension</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-08-10",
  "eventType": "Extension two"
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
    "extensionId": "6789000003"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Superseded second purchase extension</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "2017-08-11",
  "eventType": "Extension two",
  "supersede": {
    "originalEventDate": "2017-08-10",
    "originalExtensionId": "6789000003"
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
    "message": "Extension superseded",
    "extensionId": "6789000004"
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
                    accountId: 1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
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
                <p>Request with a valid payload and an invalid account ID</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1234%3D5678
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
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
            <td>
                <p>Request containing invalid and/or missing data</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "10-05-2017",
  "eventType": "Extension 1"
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
        
        
        
        
        <tr>
            <td>
                <p>The LISA account is already closed</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-08-10",
  "eventType": "Extension one"
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
            <td>
                <p>The LISA account is already cancelled</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 2000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-08-10",
  "eventType": "Extension one"
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
            <td>
                <p>The LISA account is already void</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 3000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-08-10",
  "eventType": "Extension one"
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
                <p>A first extension has not been approved</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 7000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-08-10",
  "eventType": "Extension one"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FIRST_EXTENSION_NOT_APPROVED",
  "message": "A first extension has not been approved"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>First extension already approved</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 8000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FIRST_EXTENSION_ALREADY_APPROVED",
  "message": "A first extension has already been approved"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Second extension already approved</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 9000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-08-10",
  "eventType": "Extension two"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "SECOND_EXTENSION_ALREADY_APPROVED",
  "message": "A second extension has already been approved"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Supersede details mismatch</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 5000000403
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "2017-05-11",
  "eventType": "Extension one",
  "supersede": {
    "originalEventDate": "2017-05-10",
    "originalExtensionId": "6789000001"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "SUPERSEDED_EXTENSION_MISMATCH_ERROR",
  "message": "originalExtensionId and the originalEventDate do not match the information in the original request"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Account not found</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 0000000404
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
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
            <td>
                <p>Fund release not found</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1000000404
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "FUND_RELEASE_NOT_FOUND",
  "message": "The fundReleaseId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Extension already exists</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 0000000409
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "EXTENSION_ALREADY_EXISTS",
  "message": "The investor’s purchase extension has already been requested"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Fund release has been superseded</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 2000000409
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-05-10",
  "eventType": "Extension one"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "FUND_RELEASE_SUPERSEDED",
  "message": "This fund release has already been superseded"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Extension already superseded</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
                    <br>
                    accountId: 1000000409
                </p>
            </td>
            <td>
<pre class="code--block">
{
  "eventDate": "2017-05-11",
  "eventType": "Extension one",
  "supersede": {
    "originalEventDate": "2017-05-10",
    "originalExtensionId": "6789000001"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "SUPERSEDED_EXTENSION_ALREADY_SUPERSEDED",
  "message": "This extension has already been superseded"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>