<p>You can <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/sandbox/introduction">use the HMRC Developer Sandbox to test the API</a>. The Sandbox is an enhanced testing service that functions as a simulator of HMRC’s production environment.</p>
<p>The Sandbox for the Lifetime ISA API does not currently support <a href="https://test-developer.service.hmrc.gov.uk/api-documentation/docs/sandbox/stateful-behaviour">stateful behaviour</a>, but you can use the payloads described in the resources to test specific scenarios.</p>
<p>Some scenarios will already have a lisaManagerReferenceNumber to use. Other scenarios will need you to create your own test user. To create your own test user:
1. use the Create Test User API
2. then use the userId and password in the response to get an OAuth 2.0 access token.</p>
