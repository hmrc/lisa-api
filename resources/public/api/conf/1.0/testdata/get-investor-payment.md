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
            <td><p>Request for a paid payment</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 1234567890</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
					     "transactionId": "1234567890",<br>
					     "creationDate": "2000-01-01",<br>
					     "bonusDueForPeriod": 1,<br>
					     "status": "Paid",<br>
					     "paymentDate": "2000-01-01",<br>
					     "paymentReference": "002630000993",<br>
					     "paymentAmount": 1<br>
						}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pending transaction</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 000000200</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
						     "transactionId": "000000200",<br>
						     "creationDate": "2000-01-01",<br>
						     "bonusDueForPeriod": 1,<br>
						     "status": "Pending"<br>
						}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for a pending transaction that has a due date</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 300000200</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
						    "transactionId": "300000200",<br>
						    "creationDate": "2000-01-01",<br>
						    "bonusDueForPeriod": 1,<br>
						    "status": "Pending",<br>
						    "paymentDueDate": "2000-01-01",<br>
						    "paymentAmount": 1<br>
						}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for a cancelled transaction</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 100000200</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
						    "transactionId": "100000200",<br>
						    "creationDate": "2000-01-01",<br>
						    "bonusDueForPeriod": 1,<br>
						    "status": "Cancelled",<br>						}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for a superceded transaction</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 200000200</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
						    "transactionId": "200000200",<br>
						    "creationDate": "2000-01-01",<br>
						    "bonusDueForPeriod": 1,<br>
						    "status": "Superceded",<br>						}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for a payment where a charge is owed</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 500000200</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block">{<br>
					     "transactionId": "500000200",<br>
					     "creationDate": "2000-01-01",<br>
					     "status": "Due",<br>
					     "chargeReference": "XM002610108957"<br>
						}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid Account ID and Transaction ID, but an invalid LISA Manager Reference Number</p><p class ="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890<br>transactionId: 1234567890</p></td>
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
            <td><p>Request for a transaction that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 0000000404</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "TRANSACTION_NOT_FOUND",<br>
                                            "message": "The transactionId does not match with HMRC’s records."<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 1000000404</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId given does not match with HMRC’s records"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>transactionId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
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
