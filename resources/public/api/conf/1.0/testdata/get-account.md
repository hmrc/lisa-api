<table>
  <col width="25%">
  <col width="35%">
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
        <p>Request with a valid LISA Manager reference number and account ID (open account)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1234567890
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
                "accountId": "1234567890",<br>
                "investorId": "9876543210",<br>
                "creationReason": "New",<br>
                "firstSubscriptionDate": "2017-04-06",<br>
                "accountStatus": "OPEN",<br>
                "subscriptionStatus": "ACTIVE"<br>
            }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number and account ID (transferred account)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 0000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
                 "accountId": "0000000200",<br>
                 "investorId": "9876543210",<br>
                 "creationReason": "Transferred",<br>
                 "firstSubscriptionDate": "2017-04-06",<br>
                 "accountStatus": "OPEN",<br>
                 "subscriptionStatus": "AVAILABLE",<br>
                 "transferAccount": {<br>
                     "transferredFromAccountId": "8765432102",<br>
                     "transferredFromLMRN": "Z543333",<br>
                     "transferInDate": "2017-04-06"<br>
                 }<br>
             }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number and account ID (voided account)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
                 "accountId": "1000000200",<br>
                 "investorId": "9876543210",<br>
                 "creationReason": "New",<br>
                 "firstSubscriptionDate": "2017-04-06",<br>
                 "accountStatus": "VOID",<br>
                 "subscriptionStatus": "VOID"<br>
             }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number and account ID (closed account)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 2000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
                 "accountId": "2000000200",<br>
                 "investorId": "9876543210",<br>
                 "creationReason": "New",<br>
                 "firstSubscriptionDate": "2017-04-06",<br>
                 "accountStatus": "CLOSED",<br>
                 "accountClosureReason": "All funds withdrawn",<br>
                 "closureDate": "2017-10-25"<br>
             }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid account ID, but an invalid LISA Manager reference number</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: 123456<br>
          accountId: 1234567890
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
        <p class ="code--block"> {<br>
          "code": "BAD_REQUEST",<br>
          "message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
        }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager reference number, but an invalid account ID</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1234%3D5678
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
        <p class ="code--block"> {<br>
          "code": "BAD_REQUEST",<br>
          "message": "accountId in the URL is in the wrong format"<br>
        }
        </p>
      </td>
    </tr>
    <tr>
        <td>
            <p>Request containing an account ID that does not exist</p>
            <p class ="code--block">
                lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                accountId: 0000000404
            </p>
        </td>
        <td></td>
        <td>
          <p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
          <p class ="code--block"> {<br>
            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
            "message": "The accountId does not match HMRC’s records"<br>
          }
          </p>
        </td>
    </tr>
    <tr>
      <td>
        <p>Request with an invalid 'Accept' header</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a>
          <br>accountId: 1234567890<br>
          <br>
          Accept: application/vnd.hmrc.1.0
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
        <p class ="code--block"> {<br>
          "code": "ACCEPT_HEADER_INVALID",<br>
          "message": "The accept header is missing or invalid"<br>
        }
        </p>
      </td>
    </tr>
  </tbody>
</table>