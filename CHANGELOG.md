# Change log

## Next release

* Report withdrawal charge API added

## 1.68.0

Released 18 July 2018

### Bonus corrections added
 
The 'request a bonus payment' endpoint has been enhanced with new (optional) supersede fields. You can use these to supersede a bonus claim you have previously submitted. This results in either an additional bonus or a bonus recovery.

The GET endpoint has also been updated to return the supersede fields.

New error responses have been added to support errors which may occur because of superseding a bonus request.

### Additional validation added to Request a bonus payment

* **BONUS\_CLAIM\_TIMESCALES\_EXCEEDED**  
Bonus claims can only be made or altered for dates up to 6 years and 14 days in the past.
* **HELP\_TO\_BUY\_NOT\_APPLICABLE**  
Help to buy figures can only be submitted for claims dated up to April 2018.

### Endpoint titles and descriptions updated

We have renamed some of the API endpoints and changed their descriptions in the API specification on the Developer Hub. This is a documentation change only, so urls, parameters, etc remain the same.

**For example:**

"Retrieve payment details" has been renamed to "Get a list of all bonus payments in a date range."

The description of that endpoint has been changed from "This allows a LISA provider to retrieve payments made by HMRC in a specific date range" to "Get a list of all completed and due bonus payments from HMRC in a specific date range."

### Test data IDs updated

The IDs used in the test data scenario requests and responses have been updated. This is to provide a end-to-end success path across the API endpoints.

### Test data code formatted

The JSON examples in the test data scenarios are now properly indented. This makes them easier to read, particularly when copying the examples.

### Response documentation added for POST endpoints

The response fields are now documented for both POST and GET endpoints.

### Modify date of first subscription updated

The message returned when an account has been made void has been changed.

The old message was:

*"Successfully updated the firstSubscriptionDate for the LISA account and changed the account status to void because the investor has another account with a more recent firstSubscriptionDate"*

The new message is:

*"Successfully updated the firstSubscriptionDate for the LISA account and changed the account status to void because the investor has another account with an earlier firstSubscriptionDate"*



