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
        <p>Request with a valid LISA Manager Reference Number and Account ID</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1234567890
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
            "investorId": "9876543210",<br>
            "accountId": "8765432100",<br>
            "creationReason": "New",<br>
            "firstSubscriptionDate": "2011-03-23",<br>
            "accountStatus": "OPEN"<br>
          }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager Reference Number and Account ID (transferred acount)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 0000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
            "investorId": "9876543210",<br>
            "accountId": "8765432105",<br>
            "creationReason": "Transferred",<br>
            "firstSubscriptionDate": "2011-03-23",<br>
            "accountStatus": "OPEN",<br>
            "transferredFromAccountId": "8765432102",<br>
            "transferredFromLMRN": "Z543333",<br>
            "transferInDate": "2015-12-13"<br>
          }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager Reference Number and Account ID (voided acount)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 1000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
            "investorId": "0000000408",<br>
            "accountId": "8765432106",<br>
            "creationReason": "New",<br>
            "firstSubscriptionDate": "2011-03-23",<br>
            "accountStatus": "VOID"<br>
          }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager Reference Number and Account ID (closed acount)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 2000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
            "investorId": "0000000403",<br>
            "accountId": "A1234561",<br>
            "creationReason": "New",<br>
            "firstSubscriptionDate": "2011-03-23",<br>
            "accountStatus": "CLOSED",<br>
            "accountClosureReason": "All funds withdrawn",<br>
            "closureDate": "2017-01-03"<br>
          }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid LISA Manager Reference Number and Account ID (closed acount)</p>
        <p class ="code--block">
          lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
          accountId: 2000000200
        </p>
      </td>
      <td></td>
      <td>
        <p>HTTP status: <code class="code--slim">200 (OK)</code></p>
        <p class ="code--block"> {<br>
            "investorId": "0000000403",<br>
            "accountId": "A1234561",<br>
            "creationReason": "New",<br>
            "firstSubscriptionDate": "2011-03-23",<br>
            "accountStatus": "CLOSED",<br>
            "accountClosureReason": "All funds withdrawn",<br>
            "closureDate": "2017-01-03"<br>
          }
        </p>
      </td>
    </tr>
    <tr>
      <td>
        <p>Request with a valid Account ID, but an invalid LISA Manager Reference Number</p>
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