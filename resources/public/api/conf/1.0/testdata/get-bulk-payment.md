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
                <p>Request for payments where some are found</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-05-20<br>
                    endDate: 2017-10-20
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
    "lisaManagerReferenceNumber": "Z123456",
    "payments": [
        {
            "transactionType": "Payment",
            "status": "Paid",
            "paymentAmount": 10000,
            "paymentDate": "2017-06-01",
            "paymentReference": "1040000872"
        },
        {
            "transactionType": "Payment",
            "status": "Pending",
            "paymentAmount": 12000,
            "dueDate": "2017-07-04"
        },
        {
            "transactionType": "Debt",
            "status": "Collected",
            "paymentAmount": 1000,
            "paymentDate": "2017-08-04",
            "paymentReference": "1040000985"
        },
        {
            "transactionType": "Debt",
            "status": "Due",
            "paymentAmount": 1100,
            "dueDate": "2017-09-04"
        }
    ]
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request for payments where none are found</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-06<br>
                    endDate: 2017-04-06
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "TRANSACTION_NOT_FOUND",
  "message": "No payments or debts exist for this date range"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an invalid LISA Manager reference number</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: 123456<br>
                </p>
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
                <p>Request with startDate in the wrong format</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 20-05-2017<br>
                    endDate: 2017-05-20
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "startDate is in the wrong format"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with endDate in the wrong format</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-05-20<br>
                    endDate: 20-05-2017
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "endDate is in the wrong format"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with startDate and endDate in the wrong format</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 20-05-2017<br>
                    endDate: 20-05-2017
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "startDate and endDate are in the wrong format"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with and endDate in the future</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: (today's date)<br>
                    endDate: (any date in the future)
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FORBIDDEN",
  "message": "endDate cannot be in the future"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an endDate before the startDate</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-12-20<br>
                    endDate: 2017-12-19
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FORBIDDEN",
  "message": "endDate cannot be before startDate"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an endDate before the startDate</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-05<br>
                    endDate: 2017-04-06
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FORBIDDEN",
  "message": "startDate cannot be before 6 April 2017"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an endDate over a year after startDate</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-06<br>
                    endDate: 2018-04-07
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FORBIDDEN",
  "message": "endDate cannot be more than a year after startDate"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an invalid 'Accept' header</p>
                <p class="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-06<br>
                    endDate: 2017-05-05
                    <br>
                    Accept: application/vnd.hmrc.1.0
                </p>
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
    </tbody>
</table>