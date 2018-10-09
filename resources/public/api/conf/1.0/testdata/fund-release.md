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
                  <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
          "postalCode": "AA11 1AA",
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
          "fundReleaseId": "3456789000"
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
                            <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                               <br>
                                   <strong>accountId:</strong><br>1234567891
                           </p>
                       </td>
                       <td>
        <pre class="code--block">
 {
   "eventDate": "2017-05-05",
   "withdrawalAmount": 5000.00,
   "supersede": {
               "originalFundReleaseId": "3456789000",
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
         "fundReleaseId": "3456789001"
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
           "postalCode": "AA11 1AA",
     }
  }       
        </pre>
                                    </td>
                                    <td>
                                        <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
        <pre class="code--block">
  {
     "code": "BAD_REQUEST",
     "message": "lisaManagerReferenceNumber in the URL is in the wrong format"
  }
        </pre>
                                    </td>
                                    </tr>
                                    <tr>
                                                <td>
                                                    <p>Invalid Account ID</p>
                                                    <p class="code--block">
                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                        <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
           "postalCode": "AA11 1AA",
    }
 }                                   
                                    </pre>
                                                </td>
                                                <td>
                                                    <p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
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
                                                                    <p>This LISA account is already closed</p>
                                                                    <p class="code--block">
                                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                                        <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                        <br>
                                                                        <strong>accountId:</strong><br>4030000008
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
           "postalCode": "AA11 1AA",
    }
 }                                                   
                                                       </pre>
                                                             </td>
                                                                 <td>
                                                                     <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                     <pre class="code--block">
  {
     "code": "INVESTOR_ACCOUNT_ALREADY_CLOSED",
     "message": "The LISA account has already been closed"
  }                                               
                                                     </pre>
                                                                 </td>
                                                             </tr>                                        
                                                             <tr>
                                                                <td>
                                                                    <p>This LISA account is already void</p>
                                                                    <p class="code--block">
                                                                    <strong>lisaManagerReferenceNumber:</strong><br>
                                                                    <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                     <br>
                                                                     <strong>accountId:</strong><br>4030000009
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
         "postalCode": "AA11 1AA",
   }
 }                                                                 </pre>
                                                                   </td>
                                                                        <td>
                                                                            <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                                            <pre class="code--block">
 {
   "code": "INVESTOR_ACCOUNT_ALREADY_VOID",
   "message": "The LISA account has already been voided"
 }                                               
                                                                    </pre>
                                                                    </td>
                                                                    </tr>   
                                                                    <tr>
                                                                    <td>
                                                                        <p>This LISA account is already cancelled</p>
                                                                        <p class="code--block">
                                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                                        <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                          <br>
                                                                              <strong>accountId:</strong><br>4030000010
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
           "postalCode": "AA11 1AA",
     }
  }   
                                                                                </pre>
                                                                                </td>
                                                                                <td>
                                                                                   <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                                                   <pre class="code--block">
  {
    "code": "INVESTOR_ACCOUNT_ALREADY_CANCELLED",
    "message": "The LISA account has already been cancelled"
  }                                               
                                                                                 </pre>
                                                                                 </td>
                                                                                 </tr>   
                                                                                     <tr>
                                                                                         <td>
                                                                                            <p>Account not open long enough</p>
                                                                                            <p class="code--block">
                                                                                            <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                            <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                                            <br>
                                                                                            <strong>accountId:</strong><br>4030000011
                                                                                            </p>
                                                                                            </td>
                                                                                                 <td>
                                                                                            <pre class="code--block"> 
 {
   "eventDate": "2017-12-10",
   "withdrawalAmount": 4000.00,
   "conveyancerReference": "CR12345-678900",
   "propertyDetails": {
         "nameOrNumber": "1",
         "postalCode": "AA11 2AA",
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
                                                                                                  <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                                                   <br>
                                                                                                    <strong>accountId:</strong><br>4030000012
                                                                                                   </p>
                                                                                                   </td>
                                                                                                   <td>
                                                                                                   <pre class="code--block"> 
 {
   "eventDate": "2017-05-10",
   "withdrawalAmount": 4000.00,
   "conveyancerReference": "CR12345-6789",
   "propertyDetails": {
          "nameOrNumber": "39",
          "postalCode": "AA11 1AA",
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
                                                                                                  <p>Superseded fund release mismatch error</p>
                                                                                                  <p class="code--block">
                                                                                                  <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                  <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                                               <br>
                                                                                                  <strong>accountId:</strong><br>4030000013
                                                                                                  </p>
                                                                                                  </td>
                                                                                                  <td>
                                                                                                  <pre class="code--block">
 {
    "eventDate": "2017-06-05",
    "withdrawalAmount": 10000.00,
    "supersede": {
                "originalFundReleaseId": "3456789000",
                "originalEventDate": "2017-05-05"
     }
  }     
                                                                                                </pre>
                                                                                                </td>
                                                                                                <td>
                                                                                                    <p>HTTP status: <code class="code--slim">403 (Forbidden)</code></p>
                                                                                                    <pre class="code--block">
{
    "code": "SUPERSEDED_FUND_RELEASE_MISMATCH_ERROR",
    "message": "originalFundReleaseId and the originalEventDate do not match the information in the original request"
}  
                                                                                                   </pre>
                                                                                                   </td>
                                                                                                   </tr>
                                                                                                    <tr>
                                                                                                    <td>
                                                                                                       <p>Account ID does not exist</p>
                                                                                                       <p class="code--block">
                                                                                                       <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                       <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                                                   <br>
                                                                                                       <strong>accountId:</strong><br>1000000404
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
           "postalCode": "AA11 1AA",
      }
 }          
                                                                                               </pre>
                                                                                               </td>
                                                                                               <td>
                                                                                                   <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
                                                                                                   <pre class="code--block">
{
    "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
    "message": "The accountId does not match with HMRC’s records"
}    
                                                                                              </pre>
                                                                                              </td>
                                                                                              </tr>  
                                                                                              <tr>
                                                                                              <td>
                                                                                                  <p>Superseded fund release account already superseded</p>
                                                                                                  <p class="code--block">
                                                                                                  <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                                  <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
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
                "originalFundReleaseId": "3456789000",
                "originalEventDate": "2017-05-10"
     }
  }       
                                                                                                </pre>
                                                                                                </td>
                                                                                                     <td>
                                                                                                         <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                                                                                                         <pre class="code--block">
{
    "code": "SUPERSEDED_FUND_RELEASE_ALREADY_SUPERSEDED",
    "message": "This fund release has already been superseded"
}                        
                                                                                 </pre>
                                                                                 </td>
                                                                                 </tr>
                                                                                 <tr>
                                                                                     <td>
                                                                                        <p>Fund Release Already Exists</p>
                                                                                        <p class="code--block">
                                                                                        <strong>lisaManagerReferenceNumber:</strong><br>
                                                                                        <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a><br>
                                                                                        <br>
                                                                                        <strong>accountId:</strong><br>2000000409
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
          "postalCode": "AA11 1AA",
   }
}                                                   
                                                                                         </pre>
                                                                                         </td>
                                                                                         <td>
                                                                                             <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
                                                                                             <pre class="code--block">
{
  "code": "FUND_RELEASE_ALREADY_EXISTS",
  "message": "The investor’s fund release has already been reported"
}                                               
                                                                                        </pre>
                                                                                        </td>
                                                                                        </tr> 
    </thead>
    </tbody>
    </table>
