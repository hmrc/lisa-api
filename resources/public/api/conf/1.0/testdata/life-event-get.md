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
            <td><p>Request with a valid LISA Manager reference number, account ID and life event ID</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234567890<br>lifeEventId: 1234567891</p></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "lifeEventID": "1234567891",
  "eventType" : "LISA Investor Terminal Ill Health",
  "eventDate" : "2017-04-20"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid account ID and life event ID, but an invalid LISA Manager reference number</p><p class="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890<br>lifeEventId: 1234567891</p></td>
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
            <td><p>Request containing an account ID that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 0000000404<br>lifeEventId: 1234567891</p></td>
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
            <td><p>Request containing an account ID that does not exist</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1000000404<br>lifeEventId: 1234567890</p></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_NOT_FOUND",
  "message": "The lifeEventId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing">Use your test user profile</a><br>accountId: 1234567890<br>lifeEventId: 1234567891<br><br>Accept: application/vnd.hmrc.1.0</p></td>
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