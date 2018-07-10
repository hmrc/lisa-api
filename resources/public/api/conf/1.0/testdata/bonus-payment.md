<table>
    <col width="20%">
    <col width="40%">
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
              <p>Request with a valid payload, LISA Manager reference number and account ID</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Bonus transaction created",
    "transactionId": "0123456789"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request with a valid payload, LISA Manager reference number and account ID (late notification)</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567891
              </p>
            </td>
            <td>
<pre class ="code--block">
{
  "lifeEventId": "1234567800",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Bonus transaction created - late notification",
    "transactionId": "0023456789"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request with a valid payload, LISA Manager reference number and account ID (regular bonus)</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Regular Bonus"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Bonus transaction created",
    "transactionId": "0003456789"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Superseded transaction - Bonus recovery</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 4000.00,
    "totalSubsYTD": 4000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 1000.00,
    "totalBonusDueYTD": 1000.00,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "automaticRecoveryAmount": 1000.00,
    "transactionId": "0000456789",
    "transactionAmount": 1000.00,
    "transactionResult": -1000.00,
    "reason": "Bonus recovery"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Bonus transaction superseded",
    "transactionId": "0000456789"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Superseded transaction - Additional bonus</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 4000.00,
    "totalSubsYTD": 4000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 1000.00,
    "totalBonusDueYTD": 1000.00,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "transactionId": "0000056789",
    "transactionAmount": 4000.00,
    "transactionResult": 4000.00,
    "reason": "Additional bonus"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Bonus transaction superseded",
    "transactionId": "0000056789"
  }
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request with a valid payload and account ID, but an invalid LISA Manager reference number</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                123456<br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class ="code--block">
{
  "code": "BAD_REQUEST",
  "message": "lisaManagerReferenceNumber in the URL is in the wrong format"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request with a valid payload and LISA Manager reference number, but an invalid account ID</p>
              <p class ="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234%3D5678
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
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
            <p>Request containing invalid and/or missing data</p>
            <p class="code--block">
              <strong>lisaManagerReferenceNumber:</strong><br>
              <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
              <br>
              <strong>accountId:</strong><br>
              1234567890
            </p>
          </td>
	        <td>
<pre class="code--block">
{
  "lifeEventId": true,
  "periodStartDate": "2017-04-06",
  "periodEndDate": "05-05-2017",
  "htbTransfer": {
    "htbTransferInForPeriod": 5.50,
    "htbTransferTotalYTD": 5.5001
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "claimReason": "X"
  },
  "supersede": {
    "transactionId": "ABC123",
    "transactionAmount": true,
    "transactionResult": -10.005,
    "reason": "Recovery"
  }
}
</pre>
	        </td>
	        <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
    "code": "BAD_REQUEST",
    "message": "Bad Request",
    "errors": [
        {
            "code": "INVALID_DATA_TYPE",
            "message": "Invalid data type has been used",
            "path": "/lifeEventId"
        },
        {
            "code": "INVALID_DATE",
            "message": "Date is invalid",
            "path": "/periodEndDate"
        },
        {
            "code": "INVALID_MONETARY_AMOUNT",
            "message": "Amount cannot be negative, and can only have up to 2 decimal places",
            "path": "/htbTransfer/htbTransferTotalYTD"
        },
        {
            "code": "MISSING_FIELD",
            "message": "This field is required",
            "path": "/bonuses/totalBonusDueYTD"
        },
        {
            "code": "INVALID_FORMAT",
            "message": "Invalid format has been used",
            "path": "/bonuses/claimReason"
        },
        {
            "code": "INVALID_FORMAT",
            "message": "Invalid format has been used",
            "path": "/supersede/transactionId"
        },
        {
            "code": "INVALID_DATA_TYPE",
            "message": "Invalid data type has been used",
            "path": "/supersede/transactionAmount"
        },
        {
            "code": "INVALID_MONETARY_AMOUNT",
            "message": "Amount can only have up to 2 decimal places",
            "path": "/supersede/transactionResult"
        },
        {
            "code": "INVALID_FORMAT",
            "message": "Invalid format has been used",
            "path": "/supersede/reason"
        }
    ]
}
</pre>
	        </td>
        </tr>
        <tr>
            <td>
              <p>Request with invalid monetary amounts and/or invalid dates</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "9999-04-05",
  "periodEndDate": "2016-06-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 0.00,
    "newSubsYTD": 0.00,
    "totalSubsForPeriod": 0.0,
    "totalSubsYTD": 0.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 0.0,
    "totalBonusDueYTD": 0.0,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "FORBIDDEN",
  "message": "There is a problem with the request data",
  "errors": [
    {
      "code": "INVALID_MONETARY_AMOUNT",
      "message": "newSubsForPeriod and htbTransferInForPeriod cannot both be 0",
      "path": "/inboundPayments/newSubsForPeriod"
    },
    {
      "code": "INVALID_MONETARY_AMOUNT",
      "message": "newSubsForPeriod and htbTransferInForPeriod cannot both be 0",
      "path": "/htbTransfer/htbTransferInForPeriod"
    },
    {
      "code": "INVALID_MONETARY_AMOUNT",
      "message": "totalSubsForPeriod must be more than 0",
      "path": "/inboundPayments/totalSubsForPeriod"
    },
    {
      "code": "INVALID_MONETARY_AMOUNT",
      "message": "bonusDueForPeriod must be more than 0",
      "path": "/bonuses/bonusDueForPeriod"
    },
    {
      "code": "INVALID_MONETARY_AMOUNT",
      "message": "totalBonusDueYTD must be more than 0",
      "path": "/bonuses/totalBonusDueYTD"
    },
    {
      "code": "INVALID_DATE",
      "message": "The periodStartDate must be the 6th day of the month",
      "path": "/periodStartDate"
    },
    {
      "code": "INVALID_DATE",
      "message": "The periodEndDate must be the 5th day of the month which occurs after the periodStartDate",
      "path": "/periodEndDate"
    },
    {
      "code": "INVALID_DATE",
      "message": "The periodStartDate may not be a future date",
      "path": "/periodStartDate"
    },
    {
      "code": "INVALID_DATE",
      "message": "The periodEndDate cannot be before 6 April 2017",
      "path": "/periodEndDate"
    }
  ]
}
</pre>
            </td>
        </tr>
        <tr>
        	 <td>
            <p>Request with a 'claimReason' of 'Life Event', but without a lifeEventId</p>
            <p class="code--block">
              <strong>lisaManagerReferenceNumber:</strong><br>
              <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
              <br>
              <strong>accountId:</strong><br>
              1234567890
            </p>
           </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
	        <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_NOT_PROVIDED",
  "message": "lifeEventId is required when the claimReason is a life event"
}
</pre>
	        </td>
        </tr>
       <tr>
            <td>
              <p>Request containing invalid bonus payment figures</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                0000000403
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567801",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "BONUS_CLAIM_ERROR",
  "message": "The bonus amount given is above the maximum annual amount, or the qualifying deposits are above the maximum annual amount or the bonus claim does not equal the correct percentage of qualifying funds"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request for an account that has already been closed or voided</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                0000000903
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567802",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID",
  "message": "This LISA account has already been closed or been made void by HMRC"
}
</pre>
            </td>
       </tr>
       <tr>
            <td>
              <p>Request for a bonus claim after 5 April 2018 containing help to buy funds.</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2018-04-06",
  "periodEndDate": "2018-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 1000.00,
    "htbTransferTotalYTD": 1000.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10500.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Regular Bonus"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class ="code--block">
{
  "code": "HELP_TO_BUY_NOT_APPLICABLE",
  "message": "Help to Buy is not applicable on this account"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Superseded transaction containing details which don't match an existing transaction</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1000000403
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2018-04-06",
  "periodEndDate": "2018-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10500.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "automaticRecoveryAmount": 1000.00,
    "transactionId": "1234567892",
    "transactionAmount": 2000.00,
    "transactionResult": -1000.00,
    "reason": "Bonus recovery"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class ="code--block">
{
  "code": "SUPERSEDED_BONUS_REQUEST_AMOUNT_MISMATCH",
  "message": "The transactionId on the request does not match to an existing transactionId or does not match the bonusDueForPeriod amount"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Superseded transaction with an outcome error</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                2000000403
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2018-04-06",
  "periodEndDate": "2018-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10500.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "automaticRecoveryAmount": 1000.00,
    "transactionId": "1234567892",
    "transactionAmount": 2000.00,
    "transactionResult": -1000.00,
    "reason": "Bonus recovery"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class ="code--block">
{
  "code": "SUPERSEDED_BONUS_REQUEST_OUTCOME_ERROR",
  "message": "The calculation from your superseded bonus claim is incorrect"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request containing a life event ID that does not exist</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1000000404
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567803",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "LIFE_EVENT_NOT_FOUND",
  "message": "The lifeEventId does not match with HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request containing an account ID that does not exist</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                0000000404
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567804",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "The accountId does not match HMRC’s records"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request for a bonus claim that has already been requested</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                0000000409
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class ="code--block">
{
  "code": "BONUS_CLAIM_ALREADY_EXISTS",
  "message": "The investor’s bonus payment has already been requested"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request to supersede a transaction that has already been superseded</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1000000409
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "periodStartDate": "2018-04-06",
  "periodEndDate": "2018-05-05",
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10500.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Superseding bonus claim"
  },
  "supersede": {
    "automaticRecoveryAmount": 1000.00,
    "transactionId": "0000006789",
    "transactionAmount": 2000.00,
    "transactionResult": -1000.00,
    "reason": "Bonus recovery"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class ="code--block">
{
  "code": "BONUS_REQUEST_ALREADY_SUPERSEDED",
  "message": "The transactionId and transactionAmount in the request match to a transactionId and corresponding bonusDueForPeriod amount on an existing transaction record for this account"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request with an invalid 'Accept' header</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                1234567890<br>
                <br>
                <strong>Accept:</strong><br>
                application/vnd.hmrc.1.0
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567891",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
<pre class="code--block">
{
  "code": "ACCEPT_HEADER_INVALID",
  "message": "The accept header is missing or invalid"
}
</pre>
            </td>
        </tr>
        <tr>
            <td>
              <p>Request with a valid payload, LISA Manager reference number and account ID</p>
              <p class="code--block">
                <strong>lisaManagerReferenceNumber:</strong><br>
                <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                <br>
                <strong>accountId:</strong><br>
                0000000409
              </p>
            </td>
            <td>
<pre class="code--block">
{
  "lifeEventId": "1234567890",
  "periodStartDate": "2017-04-06",
  "periodEndDate": "2017-05-05",
  "htbTransfer": {
    "htbTransferInForPeriod": 0.00,
    "htbTransferTotalYTD": 0.00
  },
  "inboundPayments": {
    "newSubsForPeriod": 4000.00,
    "newSubsYTD": 4000.00,
    "totalSubsForPeriod": 40000.00,
    "totalSubsYTD": 40000.00
  },
  "bonuses": {
    "bonusPaidYTD": 0.0,
    "bonusDueForPeriod": 10000.00,
    "totalBonusDueYTD": 10000.00,
    "claimReason": "Life Event"
  }
}
</pre>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
<pre class ="code--block">
{
  "code": "BONUS_CLAIM_ALREADY_EXISTS",
  "message": "The investor’s bonus payment has already been requested"
}
</pre>
            </td>
        </tr>
	</tbody>
</table>
