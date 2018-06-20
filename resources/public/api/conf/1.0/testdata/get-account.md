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
        <p>Request with a valid LISA Manager reference number and account ID (open account)</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1234567890
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "New",
  "accountId": "1234567890",
  "firstSubscriptionDate": "2017-04-06",
  "accountStatus": "OPEN",
  "subscriptionStatus": "ACTIVE"
}
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number and account ID (transferred account)</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1234567891
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "investorId": "9876543210",
  "creationReason": "Transferred",
  "accountId": "1234567891",
  "firstSubscriptionDate": "2017-04-06",
  "accountStatus": "OPEN",
  "subscriptionStatus": "AVAILABLE",
  "transferAccount": {
    "transferredFromAccountId": "8765432100",
    "transferredFromLMRN": "Z654321",
    "transferInDate": "2017-04-06"
  }
}
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number and account ID (voided account)</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1000000200
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "accountId": "1000000200",
  "investorId": "9876543210",
  "creationReason": "New",
  "firstSubscriptionDate": "2017-04-06",
  "accountStatus": "VOID",
  "subscriptionStatus": "VOID"
}
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number and account ID (closed account)</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 2000000200
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "accountId": "2000000200",
  "investorId": "9876543210",
  "creationReason": "New",
  "firstSubscriptionDate": "2017-04-06",
  "accountStatus": "CLOSED",
  "subscriptionStatus": "VOID",
  "accountClosureReason": "All funds withdrawn",
  "closureDate": "2017-10-25"
}
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid account ID, but an invalid LISA Manager reference number</p>
        <p class="code--block">
          lisaManagerReferenceNumber: 123456<br>
          accountId: 1234567890
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
        <p>Request with a valid LISA Manager reference number, but an invalid account ID</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1234%3D5678
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
            <p>Request containing an account ID that does not exist</p>
            <p class="code--block">
                lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                accountId: 0000000404
            </p>
        </td>
        <td>
          <p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
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
        <p>Request with an invalid 'Accept' header</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
          <br>accountId: 1234567890<br>
          <br>
          Accept: application/vnd.hmrc.1.0
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
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