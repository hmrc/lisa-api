You can [use the HMRC Developer Sandbox to test the API](https://test-developer.service.hmrc.gov.uk/api-documentation/docs/sandbox/introduction).
The Sandbox is an enhanced testing service that functions as a simulator of HMRC’s production environment.

The Sandbox for the Lifetime ISA API does not currently support [stateful behaviour](https://test-developer.service.hmrc.gov.uk/api-documentation/docs/sandbox/stateful-behaviour),
but you can use the payloads described in the resources to test specific scenarios.

Some scenarios will already have a lisaManagerReferenceNumber to use.
Other scenarios will need you to create your own test user. To create your own test user:

1\. use the [Create Test User API](https://test-developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user/1.0#_create-a-test-user-which-is-an-organisation_post_accordion)

2\. then use the userId and password in the response to get an OAuth 2.0 access token.