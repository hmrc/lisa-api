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
            <td><p>Request with a valid payload and LISA Manager Reference Number</p> <p class="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td><p class ="code--block">
                    {<br>
                     "investorNINO": "AA123456A",<br>
                     "firstName": "First Name",<br>
                     "lastName": "Last Name",<br>
                     "dateOfBirth": "1985-03-25"<br>
                   }
                       </p></td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
                <p class ="code--block">{<br>
                     "status": 201,<br>
                     "success": true,<br>
                     "data": {<br>
                                "investorId": "9876543210",<br>
                                "message": "Investor Created."<br>
                     }<br>
                   }</p></td>
        </tr>
        <tr>
            <td><p>Request containing a pre-existing investor's details</p> <p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO": "AA222222A",<br>
                                        "firstName": "First Name",<br>
                                        "lastName": "Last Name",<br>
                                        "dateOfBirth": "1985-03-25"<br>
                                      }
                                          </p></td>
            <td><p>HTTP status: <code class="code--slim">409 (Conflict)</code></p><p class ="code--block">{<br>
                                    "code": "INVESTOR_ALREADY_EXISTS",<br>
                                    "message": "The investor already has a record with HMRC",<br>
                                    "id": "1234567890"<br>
                                    }
            </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing investor details which don't match HMRC's records</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO": "AA111111A",<br>
                                        "firstName": "First Name",<br>
                                        "lastName": "Last Name",<br>
                                        "dateOfBirth": "1985-03-25"<br>
                                        }</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                  <p class ="code--block">{<br>
                                            "code": "INVESTOR_NOT_FOUND",<br>
                                            "message": "The investor details given do not match with HMRC’s records"<br>
                                            }
                                            </p>
             </td>
        </tr>
        <tr>
            <td><p>Request containing a LISA Manager Reference Number that doesn't exist</p><p class="code--block">lisaManagerReferenceNumber: Z123456789</p></td>
            <td><p class ="code--block">{<br>
                   "investorNINO": "AA123456A",<br>
                   "firstName": "First Name",<br>
                   "lastName": "Last Name",<br>
                   "dateOfBirth": "1985-03-25"<br>
                   }</p></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p><p class="code--block">{<br>
                                           "code": "NOT_FOUND",<br>
                                           "message": "Resource was not found"<br>
                                         }</p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing an invalid investor NINO</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO": "A1234567A",<br>
                                        "firstName": "First Name",<br>
                                        "lastName": "Last Name",<br>
                                        "dateOfBirth": "1985-03-25"<br>
                                        }</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                  <p class ="code--block">{<br>
                                                            "code": "BAD_REQUEST",<br>
                                                            "message": "Bad Request"<br>
                                                          }
                                  </p>
            </td>
        </tr>
        <tr>
           <td><p>Request missing an investor NINO</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
           <td><p class ="code--block">{<br>
                                       "firstName": "First Name",<br>
                                       "lastName": "Last Name",<br>
                                       "dateOfBirth": "1985-03-25"<br>
                                       }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "BAD_REQUEST",<br>
                                                           "message": "Bad Request"<br>
                                                         }
                                 </p>
           </td>
       </tr>
       <tr>
           <td><p>Request missing a first name</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO": "AA123456A",<br>
                                       "lastName": "Last Name",<br>
                                       "dateOfBirth": "1985-03-25"<br>
                                       }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "BAD_REQUEST",<br>
                                                           "message": "Bad Request"<br>
                                                         }
                                 </p>
           </td>
       </tr>
        <tr>
           <td><p>Request missing a last name</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO": "AA123456A",<br>
                                       "firstName": "First Name",<br>
                                       "dateOfBirth": "1985-03-25"<br>
                                       }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "BAD_REQUEST",<br>
                                                           "message": "Bad Request"<br>
                                                         }
                                 </p>
           </td>
        </tr>
        <tr>
           <td><p>Request missing a date of birth</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO": "AA123456A",<br>
                                       "firstName": "First Name",<br>
                                       "lastName": "Last Name" <br>
                                       }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "BAD_REQUEST",<br>
                                                           "message": "Bad Request"<br>
                                                         }
                                 </p>
           </td>
        </tr>
        <tr>
           <td><p>Request containing a date of birth set in the future</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO": "AA123456A",<br>
                                       "firstName": "First Name",<br>
                                       "lastName": "Last Name",<br>
                                       "dateOfBirth": "3000-01-01"<br>
                                       }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "BAD_REQUEST",<br>
                                                           "message": "Bad Request"<br>
                                                         }
                                 </p>
           </td>
        </tr>
        <tr>
           <td><p>Request containing a date of birth in an invalid format</p><p class ="code--block">lisaManagerReferenceNumber: Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO": "AA123456A",<br>
                                       "firstName": "First Name",<br>
                                       "lastName": "Last Name",<br>
                                       "dateOfBirth": "25-03-1985"<br>
                                       }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "BAD_REQUEST",<br>
                                                           "message": "Bad Request"<br>
                                                         }
                                 </p>
           </td>
        </tr>
        <tr>
           <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br><br>Accept: application/vnd.hmrc.1.0</p></td>
           <td><p class ="code--block">{<br>
                     "investorNINO": "AA123456A",<br>
                     "firstName": "First Name",<br>
                     "lastName": "Last Name",<br>
                     "dateOfBirth": "1985-03-25"<br>
                   }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">406 (Not Acceptable)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "ACCEPT_HEADER_INVALID",<br>
                                                           "message": "The accept header is missing or invalid"<br>
                                                         }
                                 </p>
           </td>
        </tr>
        <tr>
           <td><p>Request with an invalid 'Authorization' bearer token</p><p class ="code--block">lisaManagerReferenceNumber: Z123456<br><br>Authorization: Bearer X</p></td>
           <td><p class ="code--block">{<br>
                     "investorNINO": "AA123456A",<br>
                     "firstName": "First Name",<br>
                     "lastName": "Last Name",<br>
                     "dateOfBirth": "1985-03-25"<br>
                   }</p>
           </td>
           <td><p>HTTP status: <code class="code--slim">401 (Unauthorized)</code></p>
                                 <p class ="code--block">{<br>
                                                           "code": "INVALID_CREDENTIALS",<br>
                                                           "message": "Invalid Authentication information provided"<br>
                                                         }
                                 </p>
           </td>
        </tr>
    </tbody>
</table>