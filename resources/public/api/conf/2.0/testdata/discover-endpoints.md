<table>
    <col width="40%">
    <col width="60%">
    <thead>
        <tr>
            <th>Scenario</th>
            <th>Response</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td><p>Request with an invalid LISA Manager reference number</p><p class ="code--block">lisaManagerReferenceNumber: 123456</p></td>
            <td><p>HTTP status: <code class="code--slim">400 (Bad Request)</code></p>
<pre class="code--block">
{
  "code": "BAD_REQUEST",
  "message": "lisaManagerReferenceNumber in the URL is in the wrong format"
}
</pre>
            </td>
        </tr>
    </tbody>
</table>
