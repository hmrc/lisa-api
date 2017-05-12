<table>
    <col width="25%">
    <col width="40%">
    <col width="35%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Request Payload</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Create Request with a valid payload and LISA Manager Reference Number</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	    "investorId": "9876543210",<br>
                                     	    "creationReason": "New",<br>
                                     	    "accountId": "1234567890",<br>
                                     	    "firstSubscriptionDate": "2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">201(Created)</code></p>
                <p class ="code--block"> {<br>
                                         "status": 201,<br>
                                         "success": true,<br>
                                         "data": {<br>
                                           "message": "Account Created.",<br>
                                           "accountId": "1234567890"<br>
                                         }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Transfer Request with a valid payload and LISA Manager Reference Number</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                              "investorId": "9876543210",<br>
                                              "creationReason": "Transferred",<br>
                                              "accountId": "1234567890",<br>
                                              "firstSubscriptionDate": "2011-03-23",<br>
                                              "transferAccount": {<br>
                                                "transferredFromAccountId": "8765432100",<br>
                                                "transferredFromLMRN": "Z654321",<br>
                                                "transferInDate": "2015-12-13"<br>
                                              }<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
                <p class ="code--block"> {<br>
                                         "status": 201,<br>
                                          "success": true,<br>
                                          "data": {<br>
                                            "message": "Account Transferred.",<br>
                                            "accountId": "1234567890"<br>
                                          }<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	    "investorId": "9876543",<br>
                                     	    "creationReason": "Unknown",<br>
                                     	    "firstSubscriptionDate": 2011<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "BAD_REQUEST",<br>
                                         "message": "Bad Request"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request with an invalid 'Authorization' bearer token</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br><br>Authorization: Bearer X</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	    "investorId": "9876543210",<br>
                                     	    "creationReason": "New",<br>
                                     	    "accountId": "1234567890",<br>
                                     	    "firstSubscriptionDate": "2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">401 (Unauthorized)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "INVALID_CREDENTIALS",<br>
                                         "message": "Invalid Authentication information provided"<br>
                                       }
                </p>
            </td>
        </tr>
        
        
        
        <tr>
            <td><p>Create Account endpoint with an investor with details that doesnot match the HMRC records</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "investorId":"1234567890",<br>
                                            "creationReason":"New",<br>
                                            "accountId":"8765432100",<br>
                                            "firstSubscriptionDate":"2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "INVESTOR_NOT_FOUND",<br>
                                         "message": "The investor details given do not match with HMRC’s records"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Create Account endpoint with valid Lisa manager and an investor not eligible for LISA account</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "investorId":"1234567891",<br>
                                            "creationReason":"New",<br>
                                            "accountId":"8765432100",<br>
                                            "firstSubscriptionDate":"2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "INVESTOR_ELIGIBILITY_CHECK_FAILED",<br>
                                         "message": "The investor is not eligible for a LISA account"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Create Account endpoint with valid Lisa manager and an investor with failed compliance check</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "investorId": "1234567892",<br>
                                            "creationReason": "New",<br>
                                            "accountId": "8765432100",<br>
                                            "firstSubscriptionDate": "2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "INVESTOR_COMPLIANCE_CHECK_FAILED",<br>
                                         "message": "The investor has failed a compliance check - they may have breached ISA guidelines or regulations"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Transfer Account endpoint with transferredFromAccountId and transferredFromLMRN in the payload doesn't match HMRC records</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                         "investorId":"1234567889",<br>
                                         "creationReason":"Transferred",<br>
                                         "accountId":"1234567890",<br>
                                         "firstSubscriptionDate":"2011-03-23",<br>
                                         "transferAccount": {<br>
                                           "transferredFromAccountId": "8765432102",<br>
                                           "transferredFromLMRN": "Z123456",<br>
                                           "transferInDate": "2015-12-13"<br>
                                         }<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "PREVIOUS_INVESTOR_ACCOUNT_DOES_NOT_EXIST",<br>
                                          "message": "The transferredFromAccountId and transferredFromLMRN given don’t match with an account on HMRC’s records"<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Transfer Account endpoint without transfer details in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                               "investorId":"9876543210",<br>
                                               "creationReason":"Transferred",<br>
                                               "accountId":"87654321",<br>
                                               "firstSubscriptionDate":"2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "TRANSFER_ACCOUNT_DATA_NOT_PROVIDED",<br>
                                          "message": "The transferredFromAccountId, transferredFromLMRN and transferInDate are not provided and are required for transfer of an account."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Create Account endpoint with transfer details in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                             "investorId":"1234500892",<br>
                                             "creationReason":"New",<br>
                                             "accountId":"8765432100",<br>
                                             "firstSubscriptionDate":"2011-03-23",<br>
                                             "transferAccount": {<br>
                                               "transferredFromAccountId": "98123459898",<br>
                                               "transferredFromLMRN": "Z543333",<br>
                                               "transferInDate": "2015-12-13"<br>
                                             }
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "TRANSFER_ACCOUNT_DATA_PROVIDED",<br>
                                         "message": "transferredFromAccountId, transferedFromLMRN, and transferInDate fields should only be completed when the creationReason is \"Transferred\"."<br>
                                       }
                </p>
            </td>
        </tr>
        <tr>
            <td><p>The LISA account has already been closed or voided</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "investorId":"0000000403",<br>
                                            "creationReason":"New",<br>
                                            "accountId":"8765432100",<br>
                                            "firstSubscriptionDate":"2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                <p class ="code--block"> {<br>
                                         "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID",<br>
                                         "message": "The LISA account has already been closed or voided."<br>
                                       }
                </p>
            </td>
        </tr>
        
        
        
        <tr>
            <td><p>Request containing a LISA Manager Reference Number that doesn't exist</p><p class ="code--block">lisaManagerReferenceNumber: Z123456789</p></td>
            <td>
                <p class ="code--block"> {<br>
                                            "investorId":"0000000403",<br>
                                            "creationReason":"New",<br>
                                            "accountId":"8765432100",<br>
                                            "firstSubscriptionDate":"2011-03-23"<br>
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
            <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br><br>Accept: application/vnd.hmrc.1.0</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	    "investorId": "9876543210",<br>
                                     	    "creationReason": "New",<br>
                                     	    "accountId": "1234567890",<br>
                                     	    "firstSubscriptionDate": "2011-03-23"<br>
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
            <td><p>Create Request for a pre-existing account</p><p class ="code--block">lisaManagerReferenceNumber: Z123456789</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	    "investorId": "1234567899",<br>
                                     	    "creationReason": "New",<br>
                                     	    "accountId": "1234567890",<br>
                                     	    "firstSubscriptionDate": "2011-03-23"<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNT_ALREADY_EXISTS",<br>
                                            "message": "The LISA account already exists"<br>
                                          }
                </p>
            </td>
        </tr> 
        <tr>
            <td><p>Transfer Request for a pre-existing account</p><p class ="code--block">lisaManagerReferenceNumber: Z123456789</p></td>
            <td>
                <p class ="code--block"> {<br>
                                              "investorId": "1234567890",<br>
                                              "creationReason": "Transferred",<br>
                                              "accountId": "1234567890",<br>
                                              "firstSubscriptionDate": "2011-03-23",<br>
                                              "transferAccount": {<br>
                                                "transferredFromAccountId": "8765432100",<br>
                                                "transferredFromLMRN": "Z654321",<br>
                                                "transferInDate": "2015-12-13"<br>
                                              }<br>
                                        }
                </p>
            </td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                <p class ="code--block"> {<br>
                                            "code": "INVESTOR_ACCOUNT_ALREADY_EXISTS",<br>
                                            "message": "The LISA account already exists"<br>
                                          }
                </p>
            </td>
        </tr> 
        <tr>
            <td><p>Request which fails due to an unexpected error</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td>
                <p class ="code--block"> {<br>
                                     	    "investorId": "1234567894",<br>
                                     	    "creationReason": "New",<br>
                                     	    "accountId": "1234567890",<br>
                                     	    "firstSubscriptionDate": "2011-03-23"<br>
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