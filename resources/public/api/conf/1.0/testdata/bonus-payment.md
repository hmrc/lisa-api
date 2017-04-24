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
            <td><p>Request Bonus payment endpoint with valid bonus details</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 10000000900</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                "periodStartDate" : "2016-05-22",<br>
                                                "periodEndDate" : "2017-05-22",<br>
                                                "transactionType" : "Penalty",<br>
                                                "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                                },<br>
                                                "inboundPayments" : {<br>
                                                  "newSubsForPeriod" : 4000.00,<br>
                                                  "newSubsYTD" : 4000.00,<br>
                                                  "totalSubsForPeriod" : 40000.00,<br>
                                                  "totalSubsYTD" : 40000.00<br>
                                                },<br>
                                                "bonuses" : {<br>
                                                  "bonusPaidYTD" : 0.0,<br>
                                                  "bonusDueForPeriod" : 10000.00,<br>
                                                  "totalBonusDueYTD" : 10000.00,<br>
                                                  "claimReason" : "Life Event"<br>
                                                }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">401(Created)</code></p>
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
            <td><p>Request Bonus payment endpoint with an accountId that does not exist</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 0000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                "periodStartDate" : "2016-05-22",<br>
                                                "periodEndDate" : "2017-05-22",<br>
                                                "transactionType" : "Penalty",<br>
                                                "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                                },<br>
                                                "inboundPayments" : {<br>
                                                  "newSubsForPeriod" : 4000.00,<br>
                                                  "newSubsYTD" : 4000.00,<br>
                                                  "totalSubsForPeriod" : 40000.00,<br>
                                                  "totalSubsYTD" : 40000.00<br>
                                                },<br>
                                                "bonuses" : {<br>
                                                  "bonusPaidYTD" : 0.0,<br>
                                                  "bonusDueForPeriod" : 10000.00,<br>
                                                  "totalBonusDueYTD" : 10000.00,<br>
                                                  "claimReason" : "Life Event"<br>
                                                }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404(Not Found)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                           "message": "The accountID given does not match with HMRC’s records."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with an accountId that has already been closed or voided</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 0000000903</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                "periodStartDate" : "2016-05-22",<br>
                                                "periodEndDate" : "2017-05-22",<br>
                                                "transactionType" : "Penalty",<br>
                                                "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                                },<br>
                                                "inboundPayments" : {<br>
                                                  "newSubsForPeriod" : 4000.00,<br>
                                                  "newSubsYTD" : 4000.00,<br>
                                                  "totalSubsForPeriod" : 40000.00,<br>
                                                  "totalSubsYTD" : 40000.00<br>
                                                },<br>
                                                "bonuses" : {<br>
                                                  "bonusPaidYTD" : 0.0,<br>
                                                  "bonusDueForPeriod" : 10000.00,<br>
                                                  "totalBonusDueYTD" : 10000.00,<br>
                                                  "claimReason" : "Life Event"<br>
                                                }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403(Forbidden)</code></p>
                <p class ="code--block"> {<br>"code": "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID",<br>
                                            "message": "The LISA account has already been closed or voided."<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with bonus in the payload that exceeds the maximum amount level</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 0000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                 "periodStartDate" : "2016-05-22",<br>
                                                 "periodEndDate" : "2017-05-22",<br>
                                                 "transactionType" : "Bonus",<br>
                                                 "htbTransfer": {<br>
                                                   "htbTransferInForPeriod": 0.00,<br>
                                                   "htbTransferTotalYTD": 0.00<br>
                                                 },<br>
                                                 "inboundPayments" : {<br>
                                                   "newSubsForPeriod" : 4000.00,<br>
                                                   "newSubsYTD" : 4000.00,<br>
                                                   "totalSubsForPeriod" : 40000.00,<br>
                                                   "totalSubsYTD" : 40000.00<br>
                                                 },<br>
                                                 "bonuses" : {<br>
                                                   "bonusPaidYTD" : 0.0,<br>
                                                   "bonusDueForPeriod" : 10000.00,<br>
                                                   "totalBonusDueYTD" : 10000.00,<br>
                                                   "claimReason" : "Life Event"<br>
                                                 }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403(Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "BONUS_CLAIM_ERROR",<br>
                                           "message": "The bonus information given exceeds the maximum annual amount, the qualifying deposits exceed the maximum annual amount, or the bonus claim doesn't equal the correct percentage of stated qualifying funds."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with a life event that does not match the HMRC records</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 1000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                 "periodStartDate" : "2016-05-22",<br>
                                                 "periodEndDate" : "2017-05-22",<br>
                                                 "transactionType" : "Bonus",<br>
                                                 "htbTransfer": {<br>
                                                   "htbTransferInForPeriod": 0.00,<br>
                                                   "htbTransferTotalYTD": 0.00<br>
                                                 },<br>
                                                 "inboundPayments" : {<br>
                                                   "newSubsForPeriod" : 4000.00,<br>
                                                   "newSubsYTD" : 4000.00,<br>
                                                   "totalSubsForPeriod" : 40000.00,<br>
                                                   "totalSubsYTD" : 40000.00<br>
                                                 },<br>
                                                 "bonuses" : {<br>
                                                   "bonusPaidYTD" : 0.0,<br>
                                                   "bonusDueForPeriod" : 10000.00,<br>
                                                   "totalBonusDueYTD" : 10000.00,<br>
                                                   "claimReason" : "Life Event"<br>
                                                 }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404(Not Found)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "LIFE_EVENT_NOT_FOUND",<br>
                                           "message": "The lifeEventID does not match with HMRC’s records."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with an invalid transaction type in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 1001100404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                 "periodStartDate" : "2016-05-22",<br>
                                                 "periodEndDate" : "2017-05-22",<br>
                                                 "transactionType" : "Invalid Transaction Type",<br>
                                                 "htbTransfer": {<br>
                                                   "htbTransferInForPeriod": 0.00,<br>
                                                   "htbTransferTotalYTD": 0.00<br>
                                                 },<br>
                                                 "inboundPayments" : {<br>
                                                   "newSubsForPeriod" : 4000.00,<br>
                                                   "newSubsYTD" : 4000.00,<br>
                                                   "totalSubsForPeriod" : 40000.00,<br>
                                                   "totalSubsYTD" : 40000.00<br>
                                                 },<br>
                                                 "bonuses" : {<br>
                                                   "bonusPaidYTD" : 0.0,<br>
                                                   "bonusDueForPeriod" : 10000.00,<br>
                                                   "totalBonusDueYTD" : 10000.00,<br>
                                                   "claimReason" : "Life Event"<br>
                                                 }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400(Bad Request)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "BAD_REQUEST",<br>
                                           "message": "Bad Request"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint without lifeEventID and Life Event as claimReason</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 1110000503</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               	"periodStartDate" : "2016-05-22",<br>
                                               	"periodEndDate" : "2017-05-22",<br>
                                               	"transactionType" : "Bonus",<br>
                                               	"htbTransfer": {<br>
                                               	"htbTransferInForPeriod": 0.00,<br>
                                               	"htbTransferTotalYTD": 0.00<br>
                                               	},<br>
                                               	"inboundPayments" : {<br>
                                               	"newSubsForPeriod" : 4000.00,<br>
                                               	"newSubsYTD" : 4000.00,<br>
                                               	"totalSubsForPeriod" : 40000.00,<br>
                                               	"totalSubsYTD" : 40000.00<br>
                                               	},<br>
                                               	"bonuses" : {<br>
                                               	"bonusPaidYTD" : 0.0,<br>
                                               	"bonusDueForPeriod" : 10000.00,<br>
                                               	"totalBonusDueYTD" : 10000.00,<br>
                                               	"claimReason" : "Life Event"<br>
                                               	}<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403(Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "LIFE_EVENT_NOT_PROVIDED",<br>
                                           "message": "lifeEventID is required when the claimReason is \"Life Event\""<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with Regular Bonus as claim Reason and without lifeEventID (lifeEventID as an optional element)</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountID: 1110000503</p></td>
            <td>
                <p class ="code--block"> {<br>
                                              "periodStartDate" : "2016-05-22",<br>
                                              "periodEndDate" : "2017-05-22",<br>
                                              "transactionType" : "Bonus",<br>
                                              "htbTransfer": {<br>
                                                "htbTransferInForPeriod": 0.00,<br>
                                                "htbTransferTotalYTD": 0.00<br>
                                              },<br>
                                              "inboundPayments" : {<br>
                                                "newSubsForPeriod" : 4000.00,<br>
                                                "newSubsYTD" : 4000.00,<br>
                                                "totalSubsForPeriod" : 40000.00,<br>
                                                "totalSubsYTD" : 40000.00<br>
                                              },<br>
                                              "bonuses" : {<br>
                                                "bonusPaidYTD" : 0.0,<br>
                                                "bonusDueForPeriod" : 10000.00,<br>
                                                "totalBonusDueYTD" : 10000.00,<br>
                                                "claimReason" : "Regular Bonus"<br>
                                              }<br>
                                          }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">201(Created)</code></p>
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
            <td><p>Request Bonus payment endpoint with invalid Lisa Manager</p><p class ="code--block">lisaManagerReferenceNumber :Z123456789<br>accountID: 10000000900</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                "periodStartDate" : "2016-05-22",<br>
                                                "periodEndDate" : "2017-05-22",<br>
                                                "transactionType" : "Penalty",<br>
                                                "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                                },<br>
                                                "inboundPayments" : {<br>
                                                  "newSubsForPeriod" : 4000.00,<br>
                                                  "newSubsYTD" : 4000.00,<br>
                                                  "totalSubsForPeriod" : 40000.00,<br>
                                                  "totalSubsYTD" : 40000.00<br>
                                                },<br>
                                                "bonuses" : {<br>
                                                  "bonusPaidYTD" : 0.0,<br>
                                                  "bonusDueForPeriod" : 10000.00,<br>
                                                  "totalBonusDueYTD" : 10000.00,<br>
                                                  "claimReason" : "Life Event"<br>
                                                }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">401(Created)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "NOT_FOUND",<br>
                                           "message": "Resource was not found"<br>
                                        }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with invalid Accept Header</p><p class ="code--block">lisaManagerReferenceNumber :Z123456789<br>accountID: 10000000900<br> Accept:application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                "periodStartDate" : "2016-05-22",<br>
                                                "periodEndDate" : "2017-05-22",<br>
                                                "transactionType" : "Penalty",<br>
                                                "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                                },<br>
                                                "inboundPayments" : {<br>
                                                  "newSubsForPeriod" : 4000.00,<br>
                                                  "newSubsYTD" : 4000.00,<br>
                                                  "totalSubsForPeriod" : 40000.00,<br>
                                                  "totalSubsYTD" : 40000.00<br>
                                                },<br>
                                                "bonuses" : {<br>
                                                  "bonusPaidYTD" : 0.0,<br>
                                                  "bonusDueForPeriod" : 10000.00,<br>
                                                  "totalBonusDueYTD" : 10000.00,<br>
                                                  "claimReason" : "Life Event"<br>
                                                }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">406(Not AcceptableCreated)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "ACCEPT_HEADER_INVALID",<br>
                                           "message": "The accept header is missing or invalid"<br>
                                        }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request Bonus payment endpoint with invalid Authorization bearer token</p><p class ="code--block">lisaManagerReferenceNumber :Z123456789<br>accountID: 10000000900</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "lifeEventID" : "1234567891",<br>
                                                "periodStartDate" : "2016-05-22",<br>
                                                "periodEndDate" : "2017-05-22",<br>
                                                "transactionType" : "Penalty",<br>
                                                "htbTransfer": {<br>
                                                  "htbTransferInForPeriod": 0.00,<br>
                                                  "htbTransferTotalYTD": 0.00<br>
                                                },<br>
                                                "inboundPayments" : {<br>
                                                  "newSubsForPeriod" : 4000.00,<br>
                                                  "newSubsYTD" : 4000.00,<br>
                                                  "totalSubsForPeriod" : 40000.00,<br>
                                                  "totalSubsYTD" : 40000.00<br>
                                                },<br>
                                                "bonuses" : {<br>
                                                  "bonusPaidYTD" : 0.0,<br>
                                                  "bonusDueForPeriod" : 10000.00,<br>
                                                  "totalBonusDueYTD" : 10000.00,<br>
                                                  "claimReason" : "Life Event"<br>
                                                }<br>
                                              }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">401(Unauthorized)</code></p>
                <p class ="code--block"> {<br>
                                           "code": "INVALID_CREDENTIALS",<br>
                                           "message": "Invalid Authentication information provided"<br>
                                        }
                </p>
            </td>
        </tr>
    </tbody>
</table>