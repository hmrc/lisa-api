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
            <td><p>Request for a paid transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0123456789</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "0123456789",
  "paymentStatus": "Paid",
  "paymentDate": "2017-06-20",
  "paymentAmount": 1000,
  "paymentReference": "002630000993",
  "bonusDueForPeriod": 10000
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pending transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "0000000200",
  "paymentStatus": "Pending",
  "bonusDueForPeriod": 1000
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pending payment with a due date</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 3000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "3000000200",
  "paymentStatus": "Pending",
  "paymentDueDate": "2017-06-20",
  "bonusDueForPeriod": 1000
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a cancelled transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 1000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "1000000200",
  "paymentStatus": "Cancelled",
  "bonusDueForPeriod": 1000
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for a void transaction</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 2000000200</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "transactionId": "2000000200",
  "paymentStatus": "Void",
  "bonusDueForPeriod": 1000
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
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid LISA Manager reference number and Transaction ID, but an invalid account ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234%3D5678<br>transactionId: 0123456789</p></td>
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
            <td><p>Request for a transaction that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0000000404</p></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "BONUS_PAYMENT_TRANSACTION_NOT_FOUND",
  "message": "transactionId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567899<br>transactionId: 1000000404</p></td>
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
            <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0123456789<br><br>Accept: application/vnd.hmrc.1.0</p></td>
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