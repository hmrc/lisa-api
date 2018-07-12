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
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
  },
  "supersede": {
    "supersededBy": "1234567893"
  }
}
</pre>
			</td>
		</tr>
    <tr>
	    <td>
		    	<p>Retrieve details for a bonus payment associated with a LISA account (regular bonus)</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
  },
  "supersede": {
    "supersededBy": "1234567894"
  }
}
</pre>
			</td>
		</tr>
		<tr>
	    <td>
		    	<p>Retrieve a superseded transaction (bonus recovery)</p>
		    	<p class="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567890<br>
		    		<strong>transactionId:</strong> 0123456789
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
    "totalSubsForPeriod": 4000,
    "totalSubsYTD": 4000
  },
  "bonuses": {
    "bonusPaidYTD": 0,
    "bonusDueForPeriod": 1000,
    "totalBonusDueYTD": 1000,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "automaticRecoveryAmount": 1000,
    "originalTransactionId": "1234567890",
    "originalBonusDueForPeriod": 1000,
    "transactionResult": -1000,
    "reason": "Bonus recovery"
  }
}
</pre>
			</td>
		</tr>
		<tr>
	    <td>
		    	<p>Retrieve a superseded transaction (additional bonus)</p>
		    	<p class="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567890<br>
		    		<strong>transactionId:</strong> 1234567894
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
    "totalSubsForPeriod": 4000,
    "totalSubsYTD": 4000
  },
  "bonuses": {
    "bonusPaidYTD": 0,
    "bonusDueForPeriod": 1000,
    "totalBonusDueYTD": 1000,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "originalTransactionId": "1234567892",
    "originalBonusDueForPeriod": 4000,
    "transactionResult": 4000,
    "reason": "Additional bonus"
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
					<strong>transactionId:</strong> 1234567890
				</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
	"code": "BAD_REQUEST",
	"message": "lisaManagerReferenceNumber in the URL is in the wrong format"
}
</pre>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid accountId</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234%3D5678<br>
		    		<strong>transactionId:</strong> 0123456789
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
	"code": "BAD_REQUEST",
	"message": "accountId in the URL is in the wrong format"
}
</pre>
			</td>
		</tr>
	   <tr>
		    <td>
		    	<p>Request with an invalid bonus payment transaction</p>
		    	<p class ="code--block">
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
		    		<strong>lisaManagerReferenceNumber:</strong> <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
		    		<strong>accountId:</strong> 1234567899<br>
		    		<strong>transactionId:</strong> 1000000404
		    	</p>
			</td>
			<td>
				<p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
	"code": "INVESTOR_ACCOUNTID_NOT_FOUND",
	"message": "The accountId does not match HMRC’s records"
}
</pre>
			</td>
		</tr>
	</tbody>
</table>
