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
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 8765432100</p></td>
            <td></td>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block"> {<br>
                                         "status": 200,<br>
                                         "success": true,<br>
                                         "data": {<br>
                                           "message": "The account has been re-instated to a status of open",<br>
                                           "accountId": "8765432100"<br>
                                         }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and account ID, but an invalid LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: A12345<br>accountId: 1234567890</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                    "code": "BAD_REQUEST",<br>
                    "message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
                  }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that is open or active</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 2000000403</p></td>
                <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> 
                    {<br>
                        "code": "INVESTOR_ACCOUNT_ALREADY_OPEN",<br>
                        "message": "The account already has a status of Open"<br>
                   }
                </p>
            </td>
        </tr>
         <tr>
            <td><p>Request for an account that is closed with a closure reason as transferred out</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000403</p></td>
                <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> 
                    {<br>
                        "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",<br>
                        "message": "The account has a status of closed with a closure reason of transferred out"<br>
                   }
                </p>
            </td>
        </tr>
         <tr>
            <td><p>Request for an account that is closed with a closure reason as cancelled</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1000000403</p></td>
                <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> 
                    {<br>
                        "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",<br>
                        "message": "The account has a status of closed with a closure reason of cancelled"<br>
                   }
                </p>
            </td>
        </tr>
         <tr>
            <td><p>Request for an account that is closed with a closure reason as cancelled</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 3000000403</p></td>
                <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> 
                    {<br>
                        "code": "INVESTOR_COMPLIANCE_CHECK_FAILED",<br>
                        "message": "The investor has failed a compliance check - they may have breached ISA guidelines or regulations."<br>
                   }
                </p>
            </td>
        </tr>        
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000404</p></td>
                <td></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId does not match HMRC’s records."<br>
                                       }
                </p>
            </td>
        </tr>
    </tbody>
</table>