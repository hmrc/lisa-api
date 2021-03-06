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
    "eventType": "Statutory Submission",
    "eventDate": "2018-04-05",
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
    "eventType": "Funds release",
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
        <p>Funds release which has associated data and successful outcome</p>
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
    "conveyancerReference": "CR12345-6789",
    "propertyDetails": {
      "nameOrNumber": "1",
      "postalCode": "AA11 1AA"
    },
    "supersede": {
      "originalLifeEventId": "3456789000",
      "originalEventDate": "2017-05-10"
    }
  },
  {
    "lifeEventId": "6789000002",
    "eventType": "Extension one",
    "eventDate": "2017-05-11",
    "fundReleaseId": "3456789001",
    "supersede": {
      "originalLifeEventId": "6789000001",
      "originalEventDate": "2017-05-10"
    }
  },
  {
    "lifeEventId": "6789000004",
    "eventType": "Extension two",
    "eventDate": "2017-08-11",
    "fundReleaseId": "3456789001",
    "supersede": {
      "originalLifeEventId": "6789000003",
      "originalEventDate": "2017-08-10"
    }
  },
  {
    "lifeEventId": "5678900002",
    "fundReleaseId": "3456789001",
    "eventDate": "2017-10-10",
    "eventType": "Purchase outcome",
    "propertyPurchaseResult": "Purchase completed",
    "propertyPurchaseValue": 250000,
    "supersede": {
      "originalLifeEventId": "5678900001",
      "originalEventDate": "2017-10-05"
    }
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Funds release with failure outcome</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567891<br>
          lifeEventId: 3456789002
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "3456789002",
    "eventType": "Funds release",
    "eventDate": "2017-05-05",
    "withdrawalAmount": 5000.00,
    "conveyancerReference": "CR12345-6789",
    "propertyDetails": {
      "nameOrNumber": "1",
      "postalCode": "AA11 1AA"
    },
    "supersede": {
      "originalLifeEventId": "3456789000",
      "originalEventDate": "2017-05-10"
    }
  },
  {
    "lifeEventId": "5678900003",
    "fundReleaseId": "3456789002",
    "eventDate": "2017-10-10",
    "eventType": "Purchase outcome",
    "propertyPurchaseResult": "Purchase failed"
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
    "lifeEventId": "6789000001",
    "eventType": "Extension one",
    "eventDate": "2017-05-10",
    "fundReleaseId": "3456789001",
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
    "lifeEventId": "6789000002",
    "eventType": "Extension one",
    "eventDate": "2017-05-11",
    "fundReleaseId": "3456789001",
    "supersede": {
      "originalLifeEventId": "6789000001",
      "originalEventDate": "2017-05-10"
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
    "lifeEventId": "6789000003",
    "eventType": "Extension two",
    "eventDate": "2017-08-10",
    "fundReleaseId": "3456789001",
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
    "lifeEventId": "6789000004",
    "eventType": "Extension two",
    "eventDate": "2017-08-11",
    "fundReleaseId": "3456789001",
    "supersede": {
      "originalLifeEventId": "6789000003",
      "originalEventDate": "2017-08-10"
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
    "lifeEventId": "5678900001",
    "eventType": "Purchase outcome",
    "eventDate": "2017-10-05",
    "fundReleaseId": "3456789001",
    "propertyPurchaseResult": "Purchase completed",
    "propertyPurchaseValue": 250000,
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
    "lifeEventId": "5678900002",
    "eventType": "Purchase outcome",
    "eventDate": "2017-10-10",
    "fundReleaseId": "3456789001",
    "propertyPurchaseResult": "Purchase completed",
    "propertyPurchaseValue": 250000,
    "supersede": {
      "originalLifeEventId": "5678900001",
      "originalEventDate": "2017-10-05"
    }
  }
]
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase outcome failure</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567891<br>
          lifeEventId: 5678900003
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
[
  {
    "lifeEventId": "5678900003",
    "eventType": "Purchase outcome",
    "eventDate": "2017-10-10",
    "fundReleaseId": "3456789002",
    "propertyPurchaseResult": "Purchase failed"
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
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
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
  "message": "Enter accountId in the correct format, like ABC12345"
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
        <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
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
        <p>Life event not found</p>
        <p class="code--block">
          lisaManagerReferenceNumber: <a href="#testing">Use your test user profile</a><br>
          accountId: 1234567890<br>
          lifeEventId: 0000000404
        </p>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_NOT_FOUND",
  "message": "The lifeEventId does not match with HMRC’s records"
}
</pre>
      </td>
    </tr>
  </tbody>
</table>
