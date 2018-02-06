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
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason":"All funds withdrawn",<br>
                                     	  "closureDate": "2017-05-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block"> {<br>
                                         "status": 200,<br>
                                         "success": true,<br>
                                         "data": {<br>
                                           "message": "LISA account closed",<br>
                                           "accountId": "1234567890"<br>
                                         }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload, LISA Manager reference number and account ID</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason":"Cancellation",<br>
                                          "closureDate": "2017-05-20"<br>
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
            <td><p>Request with a valid payload and account ID, but an invalid LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: A12345<br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason":"All funds withdrawn",<br>
                                     	  "closureDate": "2017-05-20"<br>
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
            <td><p>Request containing invalid and/or missing data</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "closureDate": "3000"<br>
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
                        },<br>
                        {<br>
                          "code": "MISSING_FIELD",<br>
                          "message": "This field is required",<br>
                          "path": "/accountClosureReason"<br>
                        }<br>
                      ]<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a closure date before 6 April 2017</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                      "accountClosureReason": "All funds withdrawn",
                      "closureDate": "2017-04-05"<br>
                    }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                      "code": "FORBIDDEN",<br>
                      "message": "There is a problem with the request data",<br>
                      "errors": [<br>
                        {<br>
                          "code": "INVALID_DATE",<br>
                          "message": "The closureDate cannot be before 6 April 2017",<br>
                          "path": "/closureDate"<br>
                        }<br>
                      ]<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been voided</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234560</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason": "All funds withdrawn",<br>
                                     	  "closureDate": "2017-05-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNT_ALREADY_VOID",<br>
                                            "message": "The LISA account is already void"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234561</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason": "All funds withdrawn",<br>
                                     	  "closureDate": "2017-05-20"<br>
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
            <td><p>Request to close an account with cancellation as the reason when the cancellation period is over</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234568</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason": "Cancellation",<br>
                                          "closureDate": "2017-05-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "CANCELLATION_PERIOD_EXCEEDED",<br>
                                            "message": "You cannot close the account with cancellation as the reason because the cancellation period is over"<br>
                                       }
                </p>
            </td>
        </tr>
        </tr>
         <tr>
            <td><p>Request to close an account with all funds withdrawn as the reason and it is still within the cancellation period</p><p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234569</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason": "All funds withdrawn",<br>
                                          "closureDate": "2017-05-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCOUNT_WITHIN_CANCELLATION_PERIOD",<br>
                                            "message": "You cannot close the account with all funds withdrawn as the reason because it is within the cancellation period"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: A1234562</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	  "accountClosureReason": "All funds withdrawn",<br>
                                     	  "closureDate": "2017-05-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId does not match HMRC’s records"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                          "accountClosureReason": "All funds withdrawn",<br>
                                          "closureDate": "2017-05-20"<br>
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