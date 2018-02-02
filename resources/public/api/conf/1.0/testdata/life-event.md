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
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
                <p class ="code--block"> {<br>
                                            "status": 201,<br>
                                              "success": true,<br>
                                              "data": {<br>
                                                "message": "Life event created",<br>
                                                "lifeEventId": "9876543210"<br>
                                              }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and accountID, but an invalid LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890</p></td>
                        <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
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
                                            "eventType" : "Invalid Event Type"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
					  "code": "BAD_REQUEST",<br>
					  "message": "Bad Request",<br>
					  "errors": [<br>
					    {<br>
					      "code": "MISSING_FIELD",<br>
					      "message": "This field is required",<br>
					      "path": "/eventDate"<br>
					    },<br>
					    {<br>
					      "code": "INVALID_FORMAT",<br>
					      "message": "Invalid format has been used",<br>
					      "path": "/eventType"<br>
					    }<br>
					  ]<br>
					}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an event date before 6 April 2017</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-05"<br>
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
					      "message": "The eventDate cannot be before 6 April 2017",<br>
					      "path": "/eventDate"<br>
					    }<br>
					  ]<br>
					}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a life event that conflicts with a previously reported event</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "LIFE_EVENT_INAPPROPRIATE",<br>
                                            "message": "The life event conflicts with a previous life event reported"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed or voided</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000903</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>"code": "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID",<br>
                                            "message": "This LISA account has already been closed or been made void by HMRC"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
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
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
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
            <td><p>Request containing an already reported event</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000409</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-20"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "LIFE_EVENT_ALREADY_EXISTS",<br>
                                            "message": "The investor’s life event has already been reported",<br>
                                            "lifeEventId": "1234567890"<br>
                                          }
                </p>
            </td>
        </tr>
    </tbody>
</table>
