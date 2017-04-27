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
            <td><p>Report Life Event endpoint with valid Event type</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :1000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">201(Created)</code></p>
                <p class ="code--block"> {<br>
                                            "status": 201,<br>
                                              "success": true,<br>
                                              "data": {<br>
                                                "message": "Life Event Created",<br>
                                                "lifeEventId": "9876543210"<br>
                                              }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Report Life Event endpoint with invalid Event type</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :1000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "Invalid Event",<br>
                                            "eventDate" : "2017-04-06"<br>
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
            <td><p>Report Life Event endpoint with a LisaManager that does not exist</p><p class ="code--block">lisaManagerReferenceNumber :Z123456789<br>accountId :1000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404(Not Found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "NOT_FOUND",<br>
                                            "message": "Resource was not found"<br>
                                          }
                </p>
            </td>
        </tr>        
        <tr>
            <td><p>Report Life Event endpoint with a life event that conflicts with a previously reported life event</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :0000000403</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403(Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "LIFE_EVENT_INAPPROPRIATE",<br>
                                            "message": "The life event conflicts with previous life event reported"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Report Life Event endpoint with an account that has already been closed or voided</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :0000000903</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
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
            <td><p>Report Life Event endpoint with a life event that is already reported</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :0000000409</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">409(Conflict)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "LIFE_EVENT_ALREADY_EXISTS",<br>
                                            "message": "The investor’s life event has already been reported"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Report life event endpoint with an accountId that does not exist</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :0000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">404(Not found)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNTID_NOT_FOUND",<br>
                                            "message": "The accountID given does not match with HMRC’s records"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Report life event endpoint with an invalid Accept Header</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :1000000404<br>Accept:application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">406(Not Acceptable)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "ACCEPT_HEADER_INVALID",<br>
                                            "message": "The accept header is missing or invalid"<br>
                                          }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Report life event endpoint with an invalid Authorization Bearer token</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>accountId :1000000404</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "eventType" : "LISA Investor Terminal Ill Health",<br>
                                            "eventDate" : "2017-04-06"<br>
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