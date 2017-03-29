<table>
    <col width="25%">
    <col width="35%">
    <col width="40%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Parameters</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Happy path<br/>(example 1)</p></td>
            <td><p>nino = AA000003D<br>firstName = John<br>surname = Smith<br>dateOfBirth = 1981-01-01</p></td>
            <td><p>201 (Created)</p></td>
        </tr>
        <tr>
            <td><p>Happy path<br/>(example 2)</p></td>
            <td><p>nino = AA000004C<br>firstName = Peter<br>surname = Jones<br>dateOfBirth = 1982-01-01</p></td>
            <td><p>201 (Created)</p><p>{ &quot;eligible&quot; : false }</p></td>
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