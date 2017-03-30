<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Payload</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Create Investor with valid payload and lisaManagerReferenceNumber</p> <pre>lisaManagerReferenceNumber : Z123456</pre></td>
            <td><p>{"investorNINO" : "AA123456A",<br>
                     "firstName" : "First Name",<br>
                     "lastName" : "Last Name",<br>
                     "dateOfBirth" : "1985-03-25"<br>
                   }
                       </p></td>
            <td><p>{
                     "data": {<br>
                       "investorID": "9876543210",<br>
                       "message": "Investor Created."<br>
                     },<br>
                     "success": true,<br>
                     "status": 201<br>
                   }</p></td>
        </tr>
        <tr>
            <td><p>Create Investor with valid lisaManagerReferenceNumber and an already existing Lisa investor<br>lisaManagerReferenceNumber :Z123456</p></td>
            <td><p>{"investorNINO" : "AA222222A",<br>
                                        "firstName" : "First Name",<br>
                                        "lastName" : "Last Name",<br>
                                        "dateOfBirth" : "1985-03-25"<br>
                                      }
                                          </p></td>
            <td><p>409(Conflict)</p></td>
        </tr>
        <tr>
            <td><p>The lisaManagerReferenceNumber path parameter you've used doesn't match with an authorised LISA provider in HMRC's records.</p></td>
            <td><p>lisaManagerRefNumber = Z1234<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>401 (Unauthorized)</p><p>{ &quot;code&quot; : &quot;UNAUTHORIZED&quot; }</p></td>
        </tr>        
        <tr>
            <td><p>No match found</p></td>
            <td><p>&lt;any other combination of syntactically valid parameters&gt;</p></td>
            <td><p>404 (Not Found)</p><p>{ &quot;code&quot; : &quot;NOT_FOUND&quot; }</p></td>
        </tr>
        <tr>
            <td><p>Invalid NINO</p></td>
            <td><p>nino = &lt;any invalid NINO e.g. AA000003X&gt;<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>400 (Bad Request)</p><p>{ &quot;code&quot; : &quot;NINO_INVALID&quot; }</p></td>
        </tr>
        <tr>
            <td><p>Missing NINO</p></td>
            <td><p>&lt;nino not provided&gt;<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>400 (Bad Request)</p><p>{ &quot;code&quot; : &quot;NINO_INVALID&quot; }</p></td>
        </tr>
        <tr>
            <td><p>Missing first name</p></td>
            <td><p>&lt;firstName not provided&gt;<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>400 (Bad Request)</p><p>{ &quot;code&quot; : &quot;BAD_REQUEST&quot; }</p></td>
        </tr>
        <tr>
            <td><p>Missing surname</p></td>
            <td><p>&lt;surname not provided&gt;<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>400 (Bad Request)</p><p>{ &quot;code&quot; : &quot;BAD_REQUEST&quot; }</p></td>
        </tr>
        <tr>
            <td><p>Invalid date of birth</p></td>
            <td><p>dateOfBirth = &lt;any invalid date of birth e.g. 1901-13-01&gt;<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>400 (Bad Request)</p><p>{ &quot;code&quot; : &quot;DOB_INVALID&quot; }</p></td>
        </tr>
        <tr>
            <td><p>Missing date of birth</p></td>
            <td><p>&lt;dateOfBirth not provided&gt;<br>&lt;all other parameters syntactically valid&gt;</p></td>
            <td><p>400 (Bad Request)</p><p>{ &quot;code&quot; : &quot;DOB_INVALID&quot; }</p></td>
        </tr>
    </tbody>
</table>