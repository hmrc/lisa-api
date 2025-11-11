# lisa-api

## Requirements

This service is written in [Scala 2.13](http://www.scala-lang.org/) and [Play](http://playframework.com/), needs at least a Java 11 to run.

## Description

Access this REST API on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation).

The API allows Lifetime ISA (LISA) managers to:

* report a new investor to HMRC
* create, transfer or make changes to a LISA account
* get details of a LISA account
* report or get details of an investor life event
* request or get details of bonus payments and withdrawal charges
* get payment and debt information

## Testing Approach

You can use the sandbox environment to [test this API](https://developer.service.hmrc.gov.uk/api-documentation/docs/testing).

It does not currently support [stateful behaviour](https://developer.service.hmrc.gov.uk/api-documentation/docs/testing/stateful-behaviour), but you can use the payloads described in Resources to test specific scenarios.

You must set up a test user which is an organisation for this API using the [Create Test User API](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/api-platform-test-user/1.0#_create-a-test-user-which-is-an-organisation_post_accordion).

## Requirements

All end points are User Restricted (see [authorisation](https://developer.service.hmrc.gov.uk/api-documentation/docs/authorisation)). Versioning, data formats etc follow the API Platform standards (see [the reference guide](https://developer.service.hmrc.gov.uk/api-documentation/docs/reference-guide)).

You can dive deeper into the documentation in the [API Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/2.0).

Resources
----------

| Method | URL                                                                                                                    | Description                                                                                                                                                                                                                                              |
| :----: | ---------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}                                                                 | Use a LISA manager reference to get a list of all available endpoints.                                                                                                                                                                                   |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/investors                                                   | Report a new LISA investor to HMRC to generate an investor ID. If the investor already exists, you will get their reference number.                                                                                                                      |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts                                                    | Create a new account you’ve set up for an investor, or transfer an existing account from another LISA provider. If you’re creating a new account you’ll need to create a LISA investor first.                                                            |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/reinstate-account                                  | Re-open a LISA account that has been closed.                                                                                                                                                                                                             |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}                                        | Use an account ID to get account details.                                                                                                                                                                                                                |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/close-account                          | Close an account and report the reason and date.                                                                                                                                                                                                         |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/update-subscription                    | Modify the date when the first deposit was paid after a LISA account was created.                                                                                                                                                                        |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/events                                 | Report to HMRC if an investor has been diagnosed with a terminal illness or died. You need to do this to get a lifeEventId before you can request a bonus payment from HMRC.                                                                             |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/annual-returns                         | Report details to HMRC about LISA accounts you managed in the last tax year. You can also correct a previous return of information. You cannot send or correct a return of information if the investor account is cancelled or void.                     |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/fund-releases                          | Request the release of LISA funds to buy a property. You can also correct a request by changing the withdrawal amount or property purchase date. When you make a correction, you cannot change the property details or the conveyancer reference number. |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/purchase-extensions                    | Request an extension to your request to release funds to buy a property.                                                                                                                                                                                 |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/purchase-outcomes                      | Report to HMRC if a property purchase was completed or failed.                                                                                                                                                                                           |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/events/{lifeEventId}                   | View life event data that has been submitted to HMRC. You can view death and terminal illness, property purchase funds release, property purchase extension, property purchase outcome, and annual return of information.                                |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/withdrawal-charges                     | Tell HMRC that an investor has taken money out of a LISA account without an associated life event. You can also correct a previous withdrawal charge.                                                                                                    |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/withdrawal-charges/{transactionId}     | Use an investor’s transaction ID to get a request for a withdrawal charge that has been submitted to HMRC.                                                                                                                                               |
| POST   | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/transactions                           | Request a bonus payment from HMRC and provide a reason for the request. You can also correct a bonus claim during or after the claim reporting period. You can repay any overpaid amounts to HMRC and receive additional payments from corrected claims. |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/transactions/{transactionId}           | Use an investor’s transaction ID to get a request for a bonus payment that has been submitted to HMRC.                                                                                                                                                   |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/accounts/{accountId}/transactions/{transactionId}/payments  | Use an investor’s transaction ID to get payment details for a bonus claim or withdrawal charge, including when the amount is due to be paid or collected.                                                                                                 |
| GET    | /lifetime-isa/manager/<br>{lisaManagerReferenceNumber}<br>/payments?startDate={startDate}&endDate={endDate}            | Get a list of all pending and paid payments from HMRC and due and collected debts owed to HMRC in a specific date range.                                                                                                                                 |

For more information, visit the [API Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/2.0).

## Test data

Test data for each LISA API is available on the [HMRC Developer Hub](https://developer.service.hmrc.gov.uk/api-documentation/docs/api/service/lisa-api/2.0).

## Running locally

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs a [JRE](http://www.oracle.com/technetwork/java/javase/overview/index.html) to run.

Install [Service Manager](https://github.com/hmrc/sm2), then start dependencies:

    sm2 --start LISA_API_ALL --wait 30 && sm2 --stop LISA_API

Start the app:

    sbt run

## Testing the Service

This service uses [sbt-scoverage](https://github.com/scoverage/sbt-scoverage) to provide test coverage reports.

Run this script before raising a PR to ensure your code changes pass the Jenkins pipeline. This runs all the unit tests with scalastyle and checks for dependency updates:

```
./run_all_tests.sh
```

## Support

If you have any business-related questions, email the Software Developer Support Team (SDST) at <sdsteam@hmrc.gov.uk>

## License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")
