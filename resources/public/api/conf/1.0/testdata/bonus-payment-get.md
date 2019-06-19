<table>
	<colgroup>
		<col width="40%">
		<col width="60%">
	</colgroup>
	<thead>
		<tr>
			<th>Scenario</th>
			<th>Response</th>
		</tr>
	</thead>
	<tbody>
	    <tr>
		    <td>
		    	<p>Retrieve details for a bonus payment associated with a LISA account</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="#testing">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567890<br>
		    		<strong>transactionId:</strong> 0123456789
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0,
    "htbTransferTotalYTD": 0
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000,
    "newSubsYTD": 4000,
    "totalSubsForPeriod": 40000,
    "totalSubsYTD": 40000
  },
  "bonuses": {
    "bonusPaidYTD": 0,
    "bonusDueForPeriod": 10000,
    "totalBonusDueYTD": 10000,
    "claimReason": "Life Event"
  }
}
</pre>
			</td>
		</tr>
    <tr>
	    <td>
		    	<p>Retrieve details for a bonus payment associated with a LISA account (regular bonus)</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="#testing">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567890<br>
		    		<strong>transactionId:</strong> 0003456789
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">200 (OK)</code></p>
<pre class="code--block">
{
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000,
    "newSubsYTD": 4000,
    "totalSubsForPeriod": 40000,
    "totalSubsYTD": 40000
  },
  "bonuses": {
    "bonusPaidYTD": 0,
    "bonusDueForPeriod": 10000,
    "totalBonusDueYTD": 10000,
    "claimReason": "Regular Bonus"
  }
}
</pre>
			</td>
		</tr>
		<tr>
		  <td>
				<p>Request with an invalid LISA Manager reference number</p>
				<p class ="code--block">
					<strong>lisaManagerReferenceNumber:</strong> 123456<br>
					<strong>accountId:</strong> 1234567890<br>
					<strong>transactionId:</strong> 0123456789
				</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
}
</pre>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid accountId</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="#testing">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234%3D5678<br>
		    		<strong>transactionId:</strong> 0123456789
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter accountId in the correct format, like ABC12345"
}
</pre>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid bonus payment transaction</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="#testing">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567890<br>
		    		<strong>transactionId:</strong> 0000000404
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "BONUS_PAYMENT_TRANSACTION_NOT_FOUND",
  "message": "transactionId does not match HMRC’s records"
}
</pre>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid LISA account</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="#testing">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567899<br>
		    		<strong>transactionId:</strong> 1000000404
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "Enter a real accountId"
}
</pre>
			</td>
		</tr>
	</tbody>
</table>
