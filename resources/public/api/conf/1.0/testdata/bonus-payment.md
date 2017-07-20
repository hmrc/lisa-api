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
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
                                             }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
                <p class ="code--block"> {<br>
                                           "status": 201,<br>
                                           "success": true,<br>
                                           "data": {<br>
                                             "message": "Bonus transaction created",<br>
                                             "transactionId": "7777777777"<br>
                                           }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with a valid payload and AccountID, but an invalid LISA Manager Reference Number</p><p class ="code--block">lisaManagerReferenceNumber: 123456<br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
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
	            <p class ="code--block">{<br>
	"lifeEventId": true,<br>
	"periodStartDate": "2017-04-06",<br>
	"periodEndDate": "05-05-2017",<br>
	"htbTransfer": {<br>
		"htbTransferInForPeriod": 5.50,<br>
		"htbTransferTotalYTD": 5.5001<br>
	},<br>
	"inboundPayments": {<br>
		"newSubsForPeriod": 4000.00,<br>
		"newSubsYTD": 4000.00,<br>
		"totalSubsForPeriod": 40000.00,<br>
		"totalSubsYTD": 40000.00<br>
	},<br>
	"bonuses": {<br>
		"bonusPaidYTD": 0.0,<br>
		"bonusDueForPeriod": 10000.00,<br>
		"claimReason": "X"<br>
	}<br>
}
	            </p>
	        </td>
	        <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
	            <p class ="code--block"> {<br>
  "code": "BAD_REQUEST",<br>
  "message": "Bad Request",<br>
  "errors": [<br>
    {<br>
      "code": "INVALID_MONETARY_AMOUNT",<br>
      "message": "Amount cannot be negative, and can only have up to 2 decimal places",<br>
      "path": "/htbTransfer/htbTransferTotalYTD"<br>
    },<br>
    {<br>
      "code": "INVALID_DATA_TYPE",<br>
      "message": "Invalid data type has been used",<br>
      "path": "/lifeEventId"<br>
    },<br>
    {<br>
      "code": "MISSING_FIELD",<br>
      "message": "This field is required",<br>
      "path": "/bonuses/totalBonusDueYTD"<br>
    },<br>
    {<br>
      "code": "INVALID_FORMAT",<br>
      "message": "Invalid format has been used",<br>
      "path": "/bonuses/claimReason"<br>
    },<br>
    {<br>
      "code": "INVALID_DATE",<br>
      "message": "Date is invalid",<br>
      "path": "/periodEndDate"<br>
    }<br>
  ]<br>
}
	            </p>
	        </td>
        </tr>
        <tr>
        	 <td><p>Request with a 'claimReason' of 'Life Event', but without a lifeEventId</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1234567890</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
                                             }
                </p>
            </td>
	        <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
	            <p class ="code--block"> {<br>
	                                        "code": "LIFE_EVENT_NOT_PROVIDED",<br>
	                                        "message": "lifeEventId is required when the claimReason is \"Life Event\""<br>
	                                      }
	            </p>
	        </td>
        </tr>
       <tr>
            <td><p>Request containing invalid bonus payment figures</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 0000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
                                             }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "BONUS_CLAIM_ERROR",<br>
                                           "message": "The bonus information given exceeds the maximum annual amount, the qualifying deposits exceed the maximum annual amount, or the bonus claim doesn't equal the correct percentage of stated qualifying funds."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request for an account that has already been closed or voided</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 0000000903</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
                                             }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>"code": "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID",<br>
                                            "message": "The LISA account has already been closed or voided."<br>
                                          }
                </p>
            </td>
       </tr>
        <tr>
            <td><p>Request containing a Life Event ID that does not exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
                                             }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "LIFE_EVENT_NOT_FOUND",<br>
                                           "message": "The lifeEventId does not match with HMRC’s records."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an Account ID that doesn't exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 0000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
                                             }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountId given does not match with HMRC’s records."<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing a LISA Manager Reference Number that doesn't exist</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1234567890 </p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
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
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 1234567890<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
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
            <td><p>Request which fails due to an unexpected error</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile<a><br>accountId: 0000000500</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventId": "1234567891",<br>
                                               "periodStartDate": "2017-04-06",<br>
                                               "periodEndDate": "2017-05-05",<br>
                                               "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                               },<br>
                                               "inboundPayments": {<br>
                                                 "newSubsForPeriod": 4000.00,<br>
                                                 "newSubsYTD": 4000.00,<br>
                                                 "totalSubsForPeriod": 40000.00,<br>
                                                 "totalSubsYTD": 40000.00<br>
                                               },<br>
                                               "bonuses": {<br>
                                                 "bonusPaidYTD": 0.0,<br>
                                                 "bonusDueForPeriod": 10000.00,<br>
                                                 "totalBonusDueYTD": 10000.00,<br>
                                                 "claimReason": "Life Event"<br>
                                               }<br>
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
