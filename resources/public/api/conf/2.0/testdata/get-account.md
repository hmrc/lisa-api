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
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "accountId": "1234567890",
  "investorId": "9876543210",
  "creationReason": "New",
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
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567891
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "accountId": "1234567891",
  "investorId": "9876543210",
  "creationReason": "Transferred",
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
        <p>Request with a valid LISA Manager reference number and account ID (Current year funds transferred)</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567892
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "accountId": "1234567892",
  "investorId": "9876543210",
  "creationReason": "Current year funds transferred",
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
        <p>Request with a valid LISA Manager reference number and account ID (Previous year funds transferred)</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567893
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "accountId": "1234567893",
  "investorId": "9876543210",
  "creationReason": "Previous year funds transferred",
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
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
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
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
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
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
}
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number, but an invalid account ID</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234%3D5678
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter accountId in the correct format, like ABC12345"
}
</pre>
      </td>
    </tr>
    <tr>
        <td>
            <p>Request containing an account ID that does not exist</p>
            <p class="code--block">
                lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
                accountId: 0000000404
            </p>
        </td>
        <td>
          <p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "Enter a real accountId"
}
</pre>
        </td>
    </tr>
    <tr>
      <td>
        <p>Request with an invalid 'Accept' header</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a>
          <br>accountId: 1234567890<br>
          <br>
          Accept: application/vnd.hmrc.1.0
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "MATCHING_RESOURCE_NOT_FOUND",
  "message": "A resource with the name in the request can not be found in the API"
}
</pre>
      </td>
    </tr>
  </tbody>
</table>