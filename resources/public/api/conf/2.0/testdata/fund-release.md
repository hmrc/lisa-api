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
        <tr>
              <td>
                  <p>Fund release created</p>
                  <p class="code--block">
                  <strong>lisaManagerReferenceNumber:</strong><br>
                  <a href="#testing">Use your test user profile</a><br>
                   <br>
                        <strong>accountId:</strong><br>1234567890
                  </p>
               </td>
         <td>
<pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
          </td>
          <td>
              <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Fund release created",
    "lifeEventId": "3456789000"
  }
}
</pre>         
                    </td>
                </tr>
                <tr>
                     <td>
                         <p>Fund release superseded</p>
                         <p class="code--block">
                            <strong>lisaManagerReferenceNumber:</strong><br>
                            <a href="#testing">Use your test user profile</a><br>
                               <br>
                                   <strong>accountId:</strong><br>1234567890
                           </p>
                       </td>
                       <td>
<pre class="code--block">
{
  "eventDate": "2017-05-05",
  "withdrawalAmount": 5000.00,
  "supersede": {
    "originalLifeEventId": "3456789000",
    "originalEventDate": "2017-05-10"
  }
}
</pre>
                     </td>
                     <td>
                         <p>HTTP status: <code class="code--slim">201 (Created)</code></p>
<pre class="code--block">
{
  "status": 201,
  "success": true,
  "data": {
    "message": "Fund release superseded",
    "lifeEventId": "3456789001"
  }
}
</pre>
                            </td>
                        </tr>
                        <tr>
                                    <td>
                                        <p>Invalid LISA Manager Reference Number</p>
                                        <p class="code--block">
                                           <strong>lisaManagerReferenceNumber:</strong><br> 123456
                                            <br>
                                            <br>
                                            <strong>accountId:</strong><br>1234567890
                                        </p>
                                    </td>
                                    <td>
<pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                    </td>
                                    <td>
                                        <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter lisaManagerReferenceNumber in the correct format, like Z1234"
}
</pre>
                                    </td>
                                    </tr>
                                    <tr>
                                                <td>
                                                    <p>Invalid Account ID</p>
                                                    <p class="code--block">
                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                        <a href="#testing">Use your test user profile</a><br>
                                                        <br>
                                                        <strong>accountId:</strong><br>1234%3D5678
                                                    </p>
                                                </td>
                                                <td>
<pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                </td>
                                                <td>
                                                    <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "Enter accountId in the correct format, like ABC12345"
}
</pre>
                                                </td>
                                            </tr>
                                            <tr>
                                                  <td>
                                                    <p>Submission has not passed validation</p>
                                                    <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> 123456 <br /> <br /> <strong>accountId:</strong><br />0000000405 </p>
                                                  </td>
                                                  <td>
<pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                  </td>
                                                  <td>
                                                    <p>HTTP status: <code class="code--slim">400 (Invalid Payload)</code></p>
<pre class="code--block">
{
  "code": "INVALID_PAYLOAD",
  "message": "Submission has not passed validation"
}
</pre>
                                                  </td>
                                                </tr>
                                             <tr>
                                                <td>
                                                    <p>This LISA account is already closed</p>
                                                    <p class="code--block">
                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                        <a href="#testing">Use your test user profile</a><br>
                                                        <br>
                                                        <strong>accountId:</strong><br>1000000403
                                                    </p>
                                                </td>
 <td>
<pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                             </td>
                                                                 <td>
                                                                     <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",
  "message": "The LISA account is already closed"
}
</pre>
                                                                 </td>
                                                             </tr>                                        
                                                             <tr>
                                                                <td>
                                                                    <p>This LISA account is already void</p>
                                                                    <p class="code--block">
                                                                    <strong>lisaManagerReferenceNumber:</strong><br>
                                                                    <a href="#testing">Use your test user profile</a><br>
                                                                     <br>
                                                                     <strong>accountId:</strong><br>3000000403
                                                                     </p>
                                                                     </td>
                                                                     <td>
<pre class="code--block"> 
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}                                                                 
</pre>
                                                                   </td>
                                                                        <td>
                                                                            <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_VOID",
  "message": "The LISA account is already void"
}
</pre>
                                                                    </td>
                                                                    </tr>   
                                                                    <tr>
                                                                    <td>
                                                                        <p>This LISA account is already cancelled</p>
                                                                        <p class="code--block">
                                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                                        <a href="#testing">Use your test user profile</a><br>
                                                                          <br>
                                                                              <strong>accountId:</strong><br>2000000403
                                                                              </p>
                                                                              </td>
                                                                              <td>
<pre class="code--block"> 
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                                                </td>
                                                                                <td>
                                                                                   <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "INVESTOR_ACCOUNT_ALREADY_CANCELLED",
  "message": "The LISA account is already cancelled"
}
</pre>
                                                                                 </td>
                                                                                 </tr>   
                                                                                     <tr>
                                                                                         <td>
                                                                                            <p>Account not open long enough</p>
                                                                                            <p class="code--block">
                                                                                            <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                            <a href="#testing">Use your test user profile</a><br>
                                                                                            <br>
                                                                                            <strong>accountId:</strong><br>4000000403
                                                                                            </p>
                                                                                            </td>
                                                                                                 <td>
<pre class="code--block"> 
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                                                            </td>
                                                                                            <td>
                                                                                                <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
<pre class="code--block">
{
  "code": "COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH",
  "message": "The account has not been open for long enough"
}
</pre>
                                                                                             </td>
                                                                                             </tr>
                                                                                              <tr>
                                                                                              <td>
                                                                                                  <p>Other purchase on record</p>
                                                                                                  <p class="code--block">
                                                                                                  <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                  <a href="#testing">Use your test user profile</a><br>
                                                                                                   <br>
                                                                                                    <strong>accountId:</strong><br>6000000403
                                                                                                   </p>
                                                                                                   </td>
                                                                                                   <td>
<pre class="code--block"> 
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                                                                </td>
                                                                                                <td>
                                                                                                     <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                                                                     <pre class="code--block">
{
  "code": "COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD",
  "message": "Another property purchase is already recorded"
}
</pre>
                                                                                               </td>
                                                                                               </tr> 
                                                                                               <tr>
                                                                                               <td>
                                                                                                  <p>Supersede details do not match the original request</p>
                                                                                                  <p class="code--block">
                                                                                                  <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                  <a href="#testing">Use your test user profile</a><br>
                                                                                               <br>
                                                                                                  <strong>accountId:</strong><br>5000000403
                                                                                                  </p>
                                                                                                  </td>
                                                                                                  <td>
                                                                                                  <pre class="code--block">
{
  "eventDate": "2017-06-05",
  "withdrawalAmount": 10000.00,
  "supersede": {
    "originalLifeEventId": "3456789000",
    "originalEventDate": "2017-05-05"
  }
}
</pre>
                                                                                                </td>
                                                                                                <td>
                                                                                                    <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                                                                    <pre class="code--block">
{
  "code": "SUPERSEDED_LIFE_EVENT_MISMATCH_ERROR",
  "message": "originalLifeEventId and the originalEventDate do not match the information in the original request"
}
</pre>
                                                                                                   </td>
                                                                                                   </tr>
                                                                                                   <tr>
                                                                                                   <td>
                                                                                                       <p>Invalid Data Provided</p>
                                                                                                       <p class="code--block">
                                                                                                       <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                       <a href="#testing">Use your test user profile</a><br>
                                                                                                       <br>
                                                                                                       <strong>accountId:</strong><br>1234567890
                                                                                                       </p>
                                                                                                       </td>
                                                                                                       <td>
<pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  },
  "supersede": {
    "originalLifeEventId": "3456789000",
    "originalEventDate": "2017-05-05"
  }
}
</pre>
                                                                                                         </td>
                                                                                                         <td>
                                                                                                             <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                                                                         <pre class="code--block">
{
  "code": "INVALID_DATA_PROVIDED",
  "message": "You can only change eventDate or withdrawalAmount when superseding a property purchase fund release"
}
</pre>
                                                                                                          </td>
                                                                                                          </tr>  
                                                                                                    <tr>
                                                                                                    <td>
                                                                                                       <p>Account ID does not exist</p>
                                                                                                       <p class="code--block">
                                                                                                       <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                       <a href="#testing">Use your test user profile</a><br>
                                                                                                   <br>
                                                                                                       <strong>accountId:</strong><br>0000000404
                                                                                                       </p>
                                                                                                       </td>
                                                                                                       <td>
                                                                                                       <pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                                                               </td>
                                                                                               <td>
                                                                                                   <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                                                                                                   <pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "Enter a real accountId"
}
</pre>
                                                                                              </td>
                                                                                              </tr>  
                                                                                              <tr>
                                                                                              <td>
                                                                                                  <p>The fund release you are superseding has already been superseded</p>
                                                                                                  <p class="code--block">
                                                                                                  <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                  <a href="#testing">Use your test user profile</a><br>
                                                                                              <br>
                                                                                              <strong>accountId:</strong><br>1000000409
                                                                                              </p>
                                                                                              </td>
                                                                                                   <td>
                                                                                                       <pre class="code--block">
{
  "eventDate": "2017-05-05",
  "withdrawalAmount": 4000.00,
  "supersede": {
    "originalLifeEventId": "3456789000",
    "originalEventDate": "2017-05-10"
  }
}
</pre>
                                                                                                </td>
                                                                                                     <td>
                                                                                                         <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                                                                                                         <pre class="code--block">
{
  "code": "SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED",
  "message": "This life event has already been superseded",
  "lifeEventId": "3456789001"
}
</pre>
                                                                                 </td>
                                                                                 </tr>
                                                                                 <tr>
                                                                                     <td>
                                                                                        <p>Fund release already exists</p>
                                                                                        <p class="code--block">
                                                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                        <a href="#testing">Use your test user profile</a><br>
                                                                                        <br>
                                                                                        <strong>accountId:</strong><br>0000000409
                                                                                        </p>
                                                                                        </td>
                                                                                        <td>
                                                                                            <pre class="code--block">
{
  "eventDate": "2017-05-10",
  "withdrawalAmount": 4000.00,
  "conveyancerReference": "CR12345-6789",
  "propertyDetails": {
    "nameOrNumber": "1",
    "postalCode": "AA11 1AA"
  }
}
</pre>
                                                                                         </td>
                                                                                         <td>
                                                                                             <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                                                                                             <pre class="code--block">
{
  "code": "LIFE_EVENT_ALREADY_EXISTS",
  "message": "The investor’s life event has already been reported",
  "lifeEventId": "3456789000"
}
</pre>
                                                                                        </td>
                                                                                        </tr> 
    </thead>
    </tbody>
</table>