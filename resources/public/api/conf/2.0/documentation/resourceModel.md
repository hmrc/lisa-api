The resource model show below depicts the resources that are exposed via the API.
Three of the resources are read-only via the API; LISAManager, HMRCPayment and HMRCTransaction.
LISAManager is effectively created as part of the LISA registration process – which is not done
over API methods.  HMRCPayment and HMRCTransaction resources are created by HMRC systems and
represent the money transferred between HMRC and the LISA provider in either direction. Three
resources are writeable via the API; LISAInvestor, LISAAccount and LISATransaction.  These three
resources represent data sent from the LISA provider to HMRC.  LISAInvestor is a resource that
is shared between LISA Providers.  LISAAccount represents the creation a LISA account by the LISA
provider on behalf of an investor. LISATransaction represents some of the transactions on the LISA
account where HMRC has a business reason to need the information.
See the resource model image at [link]
(https://github.com/ridouta/lifetime-isa/blob/master/documentation/LISA%20data%20model%20v2_4.png)
