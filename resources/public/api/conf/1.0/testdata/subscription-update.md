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
            <td><p>Request with a valid payload, LISA Manager Reference Number and Account ID</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "firstSubscriptionDate" : "2017-05-05"<br>
                                          }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block"> {<br>
                                         "data": {<br>
                                           "LISA Account firstSubscriptionDate has been updated successfully",<br>
                                           "accountId": "1234567890"<br>
                                         }<br>
                                         "success": true,<br>
                                         "status": 200<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and AccountID, but an invalid LISA Manager Reference Number</p><p class ="code--block">lisaManagerReferenceNumber: A12345<br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                             "firstSubscriptionDate" : "2017-05-05"<br>
                                }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                    "code": "BAD_REQUEST",<br>
                    "message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
                  }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "firstSubscriptionDate": "3000-01-01"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                                              "code": "BAD_REQUEST",<br>
                                              "message": "Bad Request",<br>
                                              "errors": [<br>
                                                {<br>
                                                  "code": "INVALID_DATE",<br>
                                                  "message": "Date is invalid",<br>
                                                  "path": "/closureDate"<br>
                                                }<br>
                                              ]<br>
}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 8765432100</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "firstSubscriptionDate": "2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",<br>
                                            "message": "The LISA account is already closed"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an Account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: A1234562</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "firstSubscriptionDate": "2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId given does not match with HMRC’s records"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "firstSubscriptionDate": "2017-01-03"<br>
                                        }
                </p>
            </td>
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