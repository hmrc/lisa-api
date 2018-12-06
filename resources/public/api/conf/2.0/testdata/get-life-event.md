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
        <p>Terminal illness life event</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 1234567891
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "1234567891",
    "eventType" : "LISA Investor Terminal Ill Health",
    "eventDate" : "2017-04-20"
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>lisaManagerReferenceNumber is in the wrong format</p>
        <p class="code--block">
          lisaManagerReferenceNumber: 123456<br>
          accountId: 1234567890<br>
          lifeEventId: 1234567891
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
          <p>accountId is in the wrong format</p>
          <p class="code--block">
            lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
            accountId: 1234%3D5678<br>
            lifeEventId: 1234567891
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
        <p>Account not found</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 0000000404<br>
          lifeEventId: 1234567891
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
        <p>Life event not found</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1000000404<br>
          lifeEventId: 1234567891
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_NOT_FOUND",
  "message": "The lifeEventId does not match HMRC’s records"
}
</pre>
      </td>
    </tr>
  </tbody>
</table>
