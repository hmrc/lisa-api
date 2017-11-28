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
            <td><p>Request with an invalid LISA manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/1.0#testing-the-api">Use your test user profile</a></p></td>
            <td></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
                <p class ="code--block"> {<br>
  "code": "BAD_REQUEST",<br>
  "message": "lisaManagerReferenceNumber in the URL is in the wrong format"<br>
}
                </p>
            </td>
        </tr>
    </tbody>
</table>
