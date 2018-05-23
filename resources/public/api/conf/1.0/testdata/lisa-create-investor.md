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
            <td><p>Request with a valid payload and LISA Manager reference number</p> <p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorNINO": "AA123456A",
  "firstName": "First Name",
  "lastName": "Last Name",
  "dateOfBirth": "1985-03-25"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "investorId": "9876543210",
    "message": "Investor created"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and an invalid LISA Manager reference number</p> <p class="code--block">lisaManagerReferenceNumber: 123456</p></td>
            <td>
<pre class="code--block">
{
  "investorNINO": "AA123456A",
  "firstName": "First Name",
  "lastName": "Last Name",
  "dateOfBirth": "1985-03-25"
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
            <td><p>Request containing invalid and/or missing data</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorNINO": "A1234567A",
  "firstName": true,
  "dateOfBirth": "25-03-1985"
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
      "path": "/lastName"
    },
    {
      "code": "INVALID_DATE",
      "message": "Date is invalid",
      "path": "/dateOfBirth"
    },
    {
      "code": "INVALID_FORMAT",
      "message": "Invalid format has been used",
      "path": "/investorNINO"
    },
    {
      "code": "INVALID_DATA_TYPE",
      "message": "Invalid data type has been used",
      "path": "/firstName"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request containing investor details which do not match HMRC’s records</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorNINO": "AA111111A",
  "firstName": "First Name",
  "lastName": "Last Name",
  "dateOfBirth": "1985-03-25"
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
           <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br><br>Accept: application/vnd.hmrc.1.0</p></td>
           <td>
<pre class="code--block">
{
  "investorNINO": "AA123456A",
  "firstName": "First Name",
  "lastName": "Last Name",
  "dateOfBirth": "1985-03-25"
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
            <td><p>Request containing a pre-existing investor’s details</p> <p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td>
<pre class="code--block">
{
  "investorNINO": "AA222222A",
  "firstName": "First Name",
  "lastName": "Last Name",
  "dateOfBirth": "1985-03-25"
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ALREADY_EXISTS",
  "message": "The investor already has a record with HMRC",
  "id": "1234567890"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>