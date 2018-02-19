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
		    	<p>Retrieve details for a bonus payment associated with a LISA account</p>
		    	<p class ="code--block">
		    		lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		accountId: 1234567890<br>
		    		transactionId: 1234567890
		    	</p>
			</td>
			<td></td>
			<td>
				<p>HTTP status: <code class="code--slim">200 (OK)</code></p>
				<p class ="code--block">
					{<br>
						"lifeEventId": "1234567890",<br>
						"periodStartDate": "2017-04-06",<br>
						"periodEndDate": "2017-05-05",<br>
						"htbTransfer": {<br>
							"htbTransferInForPeriod": 0.00,<br>
							"htbTransferTotalYTD": 0.00<br>
						},<br>
						"inboundPayments": {<br>
							"newSubsForPeriod": 4000.00,<br>
							"newSubsYTD": 4000.00,<br>
							"totalSubsForPeriod": 4000.00,<br>
							"totalSubsYTD": 4000.00<br>
						},<br>
						"bonuses": {<br>
							"bonusDueForPeriod": 1000.00,<br>
							"totalBonusDueYTD": 1000.00,<br>
							"claimReason": "Life Event"<br>
						}<br>
					}
				</p>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid LISA Manager reference number</p>
		    	<p class ="code--block">
		    		lisaManagerReferenceNumber: 123456
		    		accountId: 1234567890<br>
		    		transactionId: 1234567890
		    	</p>
			</td>
			<td></td>
			<td>
				<p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
				<p class ="code--block">
					{<br>
						"code": "BAD_REQUEST",<br>
						"message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
					}
				</p>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid accountId</p>
		    	<p class ="code--block">
		    		lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		accountId: 1234=5678<br>
		    		transactionId: 1234567890
		    	</p>
			</td>
			<td></td>
			<td>
				<p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
				<p class ="code--block">
					{<br>
						"code": "BAD_REQUEST",<br>
						"message": "accountId in the URL is in the wrong format"<br>
					}
				</p>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid bonus payment transaction</p>
		    	<p class ="code--block">
		    		lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		accountId: 1234567890<br>
		    		transactionId: 0000000404
		    	</p>
			</td>
			<td></td>
			<td>
				<p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
				<p class ="code--block">
					{<br>
						"code": "BONUS_PAYMENT_TRANSACTION_NOT_FOUND",<br>
						"message": "transactionId does not match HMRC’s records"<br>
					}
				</p>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid LISA account</p>
		    	<p class ="code--block">
		    		lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		accountId: 1234567890<br>
		    		transactionId: 1000000404
		    	</p>
			</td>
			<td></td>
			<td>
				<p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
				<p class ="code--block">
					{<br>
						"code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
						"message": "The accountId does not match HMRC’s records"<br>
					}
				</p>
			</td>
		</tr>
	</tbody>
</table>
