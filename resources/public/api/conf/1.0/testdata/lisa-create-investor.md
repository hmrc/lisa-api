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
            <td><p>Create Investor with valid payload and lisaManagerReferenceNumber</p> <p class="code--block">lisaManagerReferenceNumber : Z123456</p></td>
            <td><p class ="code--block">
                    {<br>
                     "investorNINO" : "AA123456A",<br>
                     "firstName" : "First Name",<br>
                     "lastName" : "Last Name",<br>
                     "dateOfBirth" : "1985-03-25"<br>
                   }
                       </p></td>
            <td><p>HTTP status: <code class="code--slim">201 (Created)</code></p>
                <p class ="code--block">{<br>
                     "data": {<br>
                                "investorID": "9876543210",<br>
                                "message": "Investor Created."<br>
                     },<br>
                     "success": true,<br>
                     "status": 201<br>
                   }</p></td>
        </tr>
        <tr>
            <td><p>Create Investor with valid lisaManagerReferenceNumber and an already existing Lisa investor</p> <p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO" : "AA222222A",<br>
                                        "firstName" : "First Name",<br>
                                        "lastName" : "Last Name",<br>
                                        "dateOfBirth" : "1985-03-25"<br>
                                      }
                                          </p></td>
            <td><p>HTTP status: <code class="code--slim">409(Conflict)</code></p><p class ="code--block">{<br>
                                    "code":"INVESTOR_ALREADY_EXISTS",<br>
                                    "message":"The investor already has a record with HMRC ","id":"A33339484"<br>
                                    }
            </p>
            </td>
        </tr>
        <tr>
            <td><p>Create Investor endpoint with valid Lisa manager and an investor with details that doesnot match with HMRC records</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO":"AA111111A",<br>
                                        "firstName":"First Name",<br>
                                        "lastName":"Last Name",<br>
                                        "dateOfBirth":"1985-03-25"<br>
                                        }</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                  <p class ="code--block">{<br>
                                            "code":"INVESTOR_NOT_FOUND",<br>
                                            "message":"The investor details given do not match with HMRC’s records"
                                            }
                                            </p>
             </td>
        </tr>
        <tr>
            <td><p>Create Investor endpoint with a Lisa manager that doesnot exist</p><p class="code--block">lisaManagerReferenceNumber : Z123456789</p></td>
            <td><p class ="code--block">{<br>
                   "investorNINO":"AA111110A",<br>
                   "firstName":"First Name",<br>
                   "lastName":"Last Name",<br>
                   "dateOfBirth":"1985-03-25"<br>
                   }</p></td>
            <td><p>HTTP status: <code class="code--slim">404 (Not Found)</code></p><p class="code--block">{<br>
                                           "code": "NOT_FOUND",<br>
                                           "message": "Resource was not found"<br>
                                         }</p>
            </td>
        </tr>
        <tr>
            <td><p>Create Investor endpoint with valid an invalid investor NINO in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO":"A1234567A",<br>
                                        "firstName":"First Name",<br>
                                        "lastName":"Last Name",<br>
                                        "dateOfBirth":"1985-03-25"<br>
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
           <td><p>Create Investor endpoint with out investor NINO in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
           <td><p class ="code--block">{<br>
                                       "firstName":"First Name",<br>
                                       "lastName":"Last Name",<br>
                                       "dateOfBirth":"1985-03-25"<br>
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
           <td><p>Create Investor endpoint with out investor First name in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO":"AA111110A",<br>
                                       "lastName":"Last Name",<br>
                                       "dateOfBirth":"1985-03-25"<br>
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
           <td><p>Create Investor endpoint with out investor Last name in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO":"AA111110A",<br>
                                       "firstName":"First Name",<br>
                                       "dateOfBirth":"1985-03-25"<br>
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
           <td><p>Create Investor endpoint with out investor dateOfBirth in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO":"AA111110A",<br>
                                       "firstName":"First Name",<br>
                                       "lastName":"Last Name" <br>
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
           <td><p>Create Investor endpoint with investor dateOfBirth in invalid format in the payload</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO":"AA111110A",<br>
                                       "firstName":"First Name",<br>
                                       "lastName":"Last Name",<br>
                                       "dateOfBirth":"25-03-1985"<br>
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
           <td><p>Create Investor endpoint with an invalid Accept Header</p><p class ="code--block">lisaManagerReferenceNumber :Z123456<br>Accept:application/vnd.hmrc.1.0</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO":"AA111110A",<br>
                                       "firstName":"First Name",<br>
                                       "lastName":"Last Name",<br>
                                       "dateOfBirth":"25-03-1985"<br>
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
           <td><p>Create Investor endpoint with an invalid Authorisation Bearer token</p><p class ="code--block">lisaManagerReferenceNumber :Z123456</p></td>
           <td><p class ="code--block">{<br>
                                      "investorNINO":"AA111110A",<br>
                                       "firstName":"First Name",<br>
                                       "lastName":"Last Name",<br>
                                       "dateOfBirth":"25-03-1985"<br>
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
        <tr>
            <td><p>The lisaManagerReferenceNumber path parameter you've used doesn't match with an authorised LISA provider in HMRC's records.</p></td>
            <td><p>lisaManagerRefNumber = Z1234<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>401 (Unauthorized)</p><p>{ &quot;code&quot; : &quot;UNAUTHORIZED&quot; }</p></td>
        </tr>
    </tbody>
</table>