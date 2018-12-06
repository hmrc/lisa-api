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
        <p>Annual return of information which has been superseded</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 7890000001
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "7890000001",
    "eventDate": "2018-04-05",
    "eventType": "Statutory Submission",
    "lisaManagerName": "Company Name",
    "taxYear": 2018,
    "marketValueCash": 0,
    "marketValueStocksAndShares": 55,
    "annualSubsCash": 0,
    "annualSubsStocksAndShares": 55,
    "supersededBy": "7890000002"
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Annual return of information which supersedes another</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 7890000002
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "7890000002",
    "eventType": "Statutory Submission",
    "eventDate": "2018-04-05",
    "lisaManagerName": "Company Name",
    "taxYear": 2018,
    "marketValueCash": 0,
    "marketValueStocksAndShares": 65,
    "annualSubsCash": 0,
    "annualSubsStocksAndShares": 65,
    "supersede": {
      "originalLifeEventId": "7890000001",
      "originalEventDate": "2018-04-05"
    }
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Funds release which has been superseded</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 3456789000
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "3456789000",
    "eventType": "Funds release"
    "eventDate": "2017-05-10",
    "withdrawalAmount": 4000.00,
    "conveyancerReference": "CR12345-6789",
    "propertyDetails": {
      "nameOrNumber": "1",
      "postalCode": "AA11 1AA"
    },
    "supersededBy": "3456789001"
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Funds release which has associated data</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 3456789001
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "3456789001",
    "eventType": "Funds release",
    "eventDate": "2017-05-05",
    "withdrawalAmount": 5000.00,
    "supersede": {
      "originalLifeEventId": "3456789000",
      "originalEventDate": "2017-05-10"
    }
  },
  {
    "lifeEventId": "6789000002",
    "eventDate": "2017-05-11",
    "eventType": "Extension one",
    "supersede": {
      "originalEventDate": "2017-05-10",
      "originalLifeEventId": "6789000001"
    }
  },
  {
    "lifeEventId": "6789000004",
    "eventDate": "2017-08-11",
    "eventType": "Extension two",
    "supersede": {
      "originalEventDate": "2017-08-10",
      "originalLifeEventId": "6789000003"
    }
  },
  {
    "lifeEventId": "5678900002",
    "fundReleaseId": "3456789001",
    "eventDate": "2017-06-10",
    "eventType": "Purchase outcome",
    "propertyPurchaseResult": "Purchase completed",
    "propertyPurchaseValue": 250000,
    "supersede": {
      "originalLifeEventId": "5678900001",
      "originalEventDate": "2017-05-05"
    }
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase extension one which has been superseded</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 6789000001
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "6789000001"
    "fundReleaseId": "3456789001",
    "eventDate": "2017-05-10",
    "eventType": "Extension one",
    "supersededBy": "6789000002"
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase extension one which supersedes another</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 6789000002
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "6789000001"
    "eventDate": "2017-05-11",
    "eventType": "Extension one",
    "supersede": {
      "originalEventDate": "2017-05-10",
      "originalLifeEventId": "6789000001"
    }
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase extension two which has been superseded</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 6789000003
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "6789000003"
    "fundReleaseId": "3456789001",
    "eventDate": "2017-08-10",
    "eventType": "Extension two"
    "supersededBy": "6789000004"
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase extension two which supersedes another</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 6789000004
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "6789000004"
    "eventDate": "2017-08-11",
    "eventType": "Extension two",
    "supersede": {
      "originalEventDate": "2017-08-10",
      "originalLifeEventId": "6789000003"
    }
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase outcome which has been superseded</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 5678900001
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "5678900001"
    "fundReleaseId": "3456789001",
    "eventDate": "2017-05-10",
    "eventType": "Purchase outcome",
    "propertyPurchaseResult": "Purchase completed",
    "propertyPurchaseValue": 250000
    "supersededBy": "5678900002"
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase outcome which supersedes another</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 5678900002
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "5678900002"
    "fundReleaseId": "3456789001",
    "eventDate": "2017-05-10",
    "eventType": "Purchase outcome",
    "propertyPurchaseResult": "Purchase completed",
    "propertyPurchaseValue": 250000,
    "supersede": {
      "originalLifeEventId": "5678900001",
      "originalEventDate": "2017-05-10"
    }
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
