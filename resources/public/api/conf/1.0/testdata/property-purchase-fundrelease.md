<table>
    <col width="20%">
    <col width="40%">
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
            <td>
                <p>Fund release created</p>
                <p class="code--block">
                    <strong>lisaManagerReferenceNumber:</strong><br>
                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    <br>
                    <strong>accountId:</strong><br>1234567890
                </p>
            </td>
            <td>
<pre class="code--block">
{
   "eventDate": "2017-05-10",
   "withdrawalAmount": 4000.00,
   "conveyancerReference": "CR12345-6789",
   "propertyDetails": {
       "nameOrNumber": "1",
       "postalCode": "AA11 1AA",
      }
   }
   
</pre>
</td>
            <td>
                <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Fund release created",
    "fundReleaseId": "3456789000"
  }
}
</pre>
            </td>
        </tr>
        <tr>
        <tbody>
                <tr>
                    <td>
                        <p>Fund release superseded</p>
                        <p class="code--block">
                            <strong>lisaManagerReferenceNumber:</strong><br>
                            <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                            <br>
                            <strong>accountId:</strong><br>1234567890
                        </p>
                    </td>
                    <td>
        <pre class="code--block">
        {
          "eventDate": "2017-06-05",
          "withdrawalAmount": 4000.00,
          "supersede": {
            "originalFundReleaseId": "3456789000",
            "originalEventDate": "2017-05-10"
          }
        }
        </pre>
        </td>
                    <td>
                        <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
        <pre class="code--block">
        {
          "status": 201,
          "success": true,
          "data": {
            "message": "Fund release superseded",
            "transactionId": "3456789001"
          }
        }
        </pre>
                    </td>
                </tr>
                <tr>
                            <td>
                                <p>Invalid LISA Manager Reference Number</p>
                                <p class="code--block">
                                    <strong>lisaManagerReferenceNumber:</strong><br> 123456
                                    <br>
                                    <br>
                                    <strong>accountId:</strong><br>1234567890
                                </p>
                            </td>
                            <td>
                <pre class="code--block">
                {
                   "eventDate": "2017-05-10",
                   "withdrawalAmount": 4000.00,
                   "conveyancerReference": "CR12345-6789",
                   "propertyDetails": {
                       "nameOrNumber": "1",
                       "postalCode": "AA11 1AA",
                      }
                   }
                </pre>
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