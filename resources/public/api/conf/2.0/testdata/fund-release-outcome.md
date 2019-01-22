<table>
  <colgroup>
    <col width="20%" />
    <col width="40%" />
    <col width="40%" />
  </colgroup>
  <thead>
    <tr>
      <th>Scenario</th>
      <th>Request Payload</th>
      <th>Response</th>
    </tr>
    <tr>
      <td>
        <p>Purchase outcome created</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />1234567890 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-10-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
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
    "lifeEventId": "5678900001",
    "message": "Purchase outcome created"
  }
}               
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase outcome superseded</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />1234567890 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "eventDate": "2017-10-05",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000,
  "supersede": {
    "originalLifeEventId": "5678900001",
    "originalEventDate": "2017-10-10"
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
    "lifeEventId": "5678900002",
    "message": "Purchase outcome superseded"
  }
}        
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Invalid LISA Manager Reference Number</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> 123456 <br /> <br /> <strong>accountId:</strong><br />1234567890 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-10-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
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
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />1234%3D5678 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-10-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
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
        <p>Supersede details do not match the original request</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />5000000403 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "eventDate": "2017-10-05",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000,
  "supersede": {
    "originalLifeEventId": "5678900000",
    "originalEventDate": "2017-10-10"
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
        <p>Fund release not found</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />1000000404 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-10-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
}                                    
</pre>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">404 (Not Found)</code></p>
        <pre class="code--block">
{
  "code" : "FUND_RELEASE_NOT_FOUND",
  "message" : "The fundReleaseId does not match HMRC’s records"
}                                                                                
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Investor account not found</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />0000000404 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-10-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
}  
</pre>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">404 (Not found)</code></p>
        <pre class="code--block">
{
  "code": "INVESTOR_ACCOUNTID_NOT_FOUND",
  "message": "The accountId does not match HMRC’s records"
}                                               
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>The purchase outcome you are superseding has already been superseded</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />1000000409 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "eventDate": "2017-10-05",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000,
  "supersede": {
    "originalLifeEventId": "5678900001",
    "originalEventDate": "2017-10-10"
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
  "lifeEventId": "5678900002"
}                        
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>Purchase outcome already exists</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />0000000409 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789001",
  "eventDate": "2017-10-10",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
}                                                     
</pre>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
        <pre class="code--block">
{
  "code": "LIFE_EVENT_ALREADY_EXISTS",
  "message": "The investor’s life event has already been reported",
  "lifeEventId": "5678900001"
}                                               
</pre>
      </td>
    </tr>
    <tr>
      <td>
        <p>The associated fund release has been superseded</p>
        <p class="code--block"> <strong>lisaManagerReferenceNumber:</strong><br /> <a href="#testing">Use your test user profile</a><br /> <br /> <strong>accountId:</strong><br />2000000409 </p>
      </td>
      <td>
        <pre class="code--block">
{
  "fundReleaseId": "3456789000",
  "eventDate": "2017-10-05",
  "propertyPurchaseResult": "Purchase completed",
  "propertyPurchaseValue": 250000
}
</pre>
      </td>
      <td>
        <p>HTTP status: <code class="code--slim">409 (Conflict)</code></p>
        <pre class="code--block">
{
  "code": "FUND_RELEASE_SUPERSEDED",
  "message": "This fund release has already been superseded",
  "lifeEventId": "3456789001"
}                                               
</pre>
      </td>
    </tr>
  </thead>
</table>
