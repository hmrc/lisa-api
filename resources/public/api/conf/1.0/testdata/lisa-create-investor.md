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
            <td><p>Request with a valid payload and LISA manager reference number</p> <p class="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
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
            <td><p>Request with a valid payload and an invalid LISA manager reference number</p> <p class="code--block">lisaManagerReferenceNumber: 123456</p></td>
            <td><p class ="code--block">
                    {<br>
                     "investorNINO": "AA123456A",<br>
                     "firstName": "First Name",<br>
                     "lastName": "Last Name",<br>
                     "dateOfBirth": "1985-03-25"<br>
                   }
                       </p></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
  "code": "BAD_REQUEST",<br>
  "message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
}
                </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing invalid and/or missing data</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td><p class ="code--block">{<br>
                                        "investorNINO": "A1234567A",<br>
                                        "firstName": true,<br>
                                        "dateOfBirth": "25-03-1985"<br>
                                        }</p>
            </td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                  <p class ="code--block">{<br>
							  "code": "BAD_REQUEST",<br>
							  "message": "Bad Request",<br>
							  "errors": [<br>
							    {<br>
							      "code": "MISSING_FIELD",<br>
							      "message": "This field is required",<br>
							      "path": "/lastName"<br>
							    },<br>
							    {<br>
							      "code": "INVALID_DATE",<br>
							      "message": "Date is invalid",<br>
							      "path": "/dateOfBirth"<br>
							    },<br>
							    {<br>
							      "code": "INVALID_FORMAT",<br>
							      "message": "Invalid format has been used",<br>
							      "path": "/investorNINO"<br>
							    },<br>
							    {<br>
							      "code": "INVALID_DATA_TYPE",<br>
							      "message": "Invalid data type has been used",<br>
							      "path": "/firstName"<br>
							    }<br>
							  ]<br>
							}
                  </p>
            </td>
        </tr>
        <tr>
            <td><p>Request containing investor details which don't match HMRC's records</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
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
           <td><p>Request with an invalid 'Accept' header</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br><br>Accept: application/vnd.hmrc.1.0</p></td>
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
            <td><p>Request containing a pre-existing investor's details</p> <p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
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
    </tbody>
</table>