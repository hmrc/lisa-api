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
            <td><p>Close Account endpoint with valid accountId and Lisa Manager</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :A1234568</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason":"All funds withdrawn",<br>
                                     	  "closureDate":"2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 OK</code></p>
                <p class ="code--block"> {<br>
                                         "status": 200,<br>
                                         "success": true,<br>
                                         "data": {<br>
                                           "message": "LISA Account Closed",<br>
                                           "accountId": "A1234568"<br>
                                         }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Close Account endpoint without accountClosureReason and (or) closereDate in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :A1234568</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "closureDate":"2017-01-03"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400(Bad RequestOK)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "BAD_REQUEST",<br>
                                            "message": "Bad Request"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Close Account endpoint with an accountId that doesnot exist</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :A1234562</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason" : "All funds withdrawn",
                                          "closureDate" : "2017-01-03"
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404(Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId given does not match with HMRC’s records"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Close Account endpoint with an accountId that is already closed</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :A1234561</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason" : "All funds withdrawn",
                                          "closureDate" : "2017-01-03"
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403(Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",<br>
                                            "message": "The LISA account is already closed"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Close Account endpoint with a Lisamanager in the URI doesnot exist</p><p class ="code--block">lisaManagerReferenceNumber :Z123456789<br>accountId :A1234561</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason" : "All funds withdrawn",
                                          "closureDate" : "2017-01-03"
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404</code></p>
                <p class ="code--block"> {<br>
                                            "code": "NOT_FOUND",<br>
                                            "message": "Resource was not found"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Close Account endpoint with an invalid Authorization Bearer token</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :A12345</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason" : "All funds withdrawn",
                                          "closureDate" : "2017-01-03"
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">401(Unauthorized)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVALID_CREDENTIALS",<br>
                                            "message": "Invalid Authentication information provided"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Close Account endpoint with an invalid Accept header</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :A1234551<br>Accept:application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason" : "All funds withdrawn",
                                          "closureDate" : "2017-01-03"
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">406(Not Acceptable)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCEPT_HEADER_INVALID",<br>
                                            "message": "The accept header is missing or invalid"<br>
                                       }
                </p>
            </td>
        </tr>
    </tbody>
</table>