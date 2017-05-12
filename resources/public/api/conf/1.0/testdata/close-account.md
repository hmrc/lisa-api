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
            <td><p>Request with a valid payload, LISA Manager Reference Number and Account ID</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason":"All funds withdrawn",<br>
                                     	  "closureDate": "2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block"> {<br>
                                         "status": 200,<br>
                                         "success": true,<br>
                                         "data": {<br>
                                           "message": "LISA Account Closed",<br>
                                           "accountId": "1234567890"<br>
                                         }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "closureDate": "3000-01-01"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "BAD_REQUEST",<br>
                                            "message": "Bad Request"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed</p><p class="code--block">lisaManagerReferenceNumber: Z123456<br>accountId: A1234561</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason": "All funds withdrawn",<br>
                                     	  "closureDate": "2017-01-03"<br>
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
            <td><p>Request containing an Account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br>accountId: A1234562</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason": "All funds withdrawn",<br>
                                     	  "closureDate": "2017-01-03"<br>
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
            <td><p>Request containing a LISA Manager Reference Number that doesn't exist</p><p class="code--block">lisaManagerReferenceNumber: Z123456789<br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason": "All funds withdrawn",<br>
                                     	  "closureDate": "2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "NOT_FOUND",<br>
                                            "message": "Resource was not found"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason": "All funds withdrawn",<br>
                                          "closureDate": "2017-01-03"<br>
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
        <tr>
            <td><p>Request which fails due to an unexpected error</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br>accountId: A1234563</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason": "All funds withdrawn",<br>
                                          "closureDate": "2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">500 (Internal Server Error)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INTERNAL_SERVER_ERROR",<br>
                                            "message": "Internal server error"<br>
                                          }
                </p>
            </td>
        </tr>
    </tbody>
</table>