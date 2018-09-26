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
            <td><p>Request for a paid transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0123456789</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "0123456789",
  "transactionType": "Payment",
  "paymentStatus": "Paid",
  "paymentDate": "2017-05-20",
  "paymentAmount": 1000,
  "paymentReference": "002630000993"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pending transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "0000000200",
  "paymentStatus": "Pending"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pending transaction that has a due date</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 3000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "3000000200",
  "transactionType": "Payment",
  "paymentStatus": "Pending",
  "paymentDueDate": "2017-06-20"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a cancelled transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 1000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "1000000200",
  "paymentStatus": "Cancelled",
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a void transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 2000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "2000000200",
  "paymentStatus": "Void"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid account ID and Transaction ID, but an invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890<br>transactionId: 0123456789</p></td>
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
            <td><p>Request with a valid LISA Manager reference number and Transaction ID, but an invalid account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234%3D5678<br>transactionId: 0123456789</p></td>
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
            <td><p>Request for a transaction that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0000000404</p></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
<pre class="code--block">
{
  "code": "TRANSACTION_NOT_FOUND",
  "message": "transactionId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567899<br>transactionId: 1000000404</p></td>
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
            <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0123456789<br><br>Accept: application/vnd.hmrc.1.0</p></td>
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