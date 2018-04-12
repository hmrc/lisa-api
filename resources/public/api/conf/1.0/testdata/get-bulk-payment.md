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
                <p>Request for payments where some are found</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-05-20<br>
                    endDate: 2017-10-20
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
                                          "lisaManagerReferenceNumber": "Z123456",<br>
                                          "payments": [<br>
                                            {<br>
                                              "paymentAmount": 10000,<br>
                                              "paymentDate": "2017-06-01",<br>
                                              "paymentReference": "1040000872"<br>
                                            },<br>
                                            {<br>
                                              "paymentAmount": 12000,<br>
                                              "dueDate": "2017-07-04"<br>
                                            }<br>
                                          ]<br>
                                        }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request for payments where none are found</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-06<br>
                    endDate: 2017-04-06
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block">{<br>
                        "code": "PAYMENT_NOT_FOUND",<br>
                        "message": "No bonus payments have been made for this date range"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an invalid LISA Manager reference number</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: 123456<br>
                </p>
            </td>
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
            <td>
                <p>Request with startDate in the wrong format</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 20-05-2017<br>
                    endDate: 2017-05-20
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block">{<br>
                        "code": "BAD_REQUEST",<br>
                        "message": "startDate is in the wrong format"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with endDate in the wrong format</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-05-20<br>
                    endDate: 20-05-2017
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block">{<br>
                        "code": "BAD_REQUEST",<br>
                        "message": "endDate is in the wrong format"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with startDate and endDate in the wrong format</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 20-05-2017<br>
                    endDate: 20-05-2017
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block">{<br>
                        "code": "BAD_REQUEST",<br>
                        "message": "startDate and endDate are in the wrong format"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with and endDate in the future</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: (today's date)<br>
                    endDate: (any date in the future)
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block">{<br>
                        "code": "FORBIDDEN",<br>
                        "message": "endDate cannot be in the future"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an endDate before the startDate</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-12-20<br>
                    endDate: 2017-12-19
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block">{<br>
                        "code": "FORBIDDEN",<br>
                        "message": "endDate cannot be before startDate"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an endDate before the startDate</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-05<br>
                    endDate: 2017-04-06
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block">{<br>
                        "code": "FORBIDDEN",<br>
                        "message": "startDate cannot be before 6 April 2017"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an endDate over a year after startDate</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-06<br>
                    endDate: 2018-04-07
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block">{<br>
                        "code": "FORBIDDEN",<br>
                        "message": "endDate cannot be more than a year after startDate"<br>
                    }
                </p>
            </td>
        </tr>
        <tr>
            <td>
                <p>Request with an invalid 'Accept' header</p>
                <p class ="code--block">
                    lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                    startDate: 2017-04-06<br>
                    endDate: 2017-05-05
                    <br>
                    Accept: application/vnd.hmrc.1.0
                </p>
            </td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCEPT_HEADER_INVALID",<br>
                                            "message": "The accept header is missing or invalid"<br>
                                          }
                </p>
            </td>
        </tr>    </tbody>
</table>
