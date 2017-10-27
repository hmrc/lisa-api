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
            <td><p>Request with a valid payload, LISA Manager Reference Number and Account ID</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 8765432100</p></td>
            <td><p> empty payload </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block"> {<br>
                                         "status": 200,<br>
                                         "success": true,<br>
                                         "data": {<br>
                                           "message": "LISA Account reinstated",<br>
                                           "accountId": "8765432100"<br>
                                         }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and AccountID, but an invalid LISA Manager Reference Number</p><p class ="code--block">lisaManagerReferenceNumber: A12345<br>accountId: 1234567890</p></td>
            <td><p> empty payload </p>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                    "code": "BAD_REQUEST",<br>
                    "message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
                  }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that is open or active</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 8765432100</p></td>
                <td><p> empty payload </p>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> 
                    {<br>
                        "code": "INVESTOR_ACCOUNT_CANNOT_BE_REINSTATED",<br>
                        "message": "Investor Account may have been transferred or is active."<br>
                   }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an Account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234562</p></td>
                <td><p> empty payload </p>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId given does not match with HMRC’s records"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td><p> empty payload </p>
            <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCEPT_HEADER_INVALID",<br>
                                            "message": "The accept header is missing or invalid"<br>
                                       }
                </p>
            </td>
        </tr>
    </tbody>
</table>