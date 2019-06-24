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
            <td><p>Request with a valid LISA Manager reference number, account ID and life event ID</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>lifeEventId: 1234567890</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">200 (OK)</code></p>
                <p class ="code--block"> {<br>
	                "lifeEventID": "9876543210",<br>
					  	"eventType" : "LISA Investor Terminal Ill Health",<br>
						"eventDate" : "2017-04-20"<br>
					}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid account ID and life event ID, but an invalid LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890<br>lifeEventId: 1234567890</p></td>
                        <td></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                    "code": "BAD_REQUEST",<br>
                    "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"<br>
                  }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 0000000404<br>lifeEventId: 1234567890</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "Enter a real accountId"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an account ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1000000404<br>lifeEventId: 1234567890</p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "LIFE_EVENT_NOT_FOUND",<br>
                                            "message": "Enter a real lifeEventId"<br>
                                          }
                </p>Enter a real accountId
            </td>
        </tr>        
        <tr>
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>accountId: 1234567890<br>lifeEventId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
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
