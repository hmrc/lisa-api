package uk.gov.hmrc.lisaapi.models


case class ID(id:String)

case class AccountId(id:String)

case class ReferenceNumber(ref:String)

case class TransactionType (code:String)
{
  val types = Set("Penalty", "Bonus")
  def apply (typeCode: String) =
    if (types.contains(typeCode))  typeCode
    else throw new IllegalArgumentException
}

case class EventType(typeCode:String) {

  val types = Set("LISA Investor Terminal Ill Health", "LISA Investor Death", "House Purchase")
  def apply (typeCode: String) =
    if (types.contains(typeCode))  typeCode
    else throw new IllegalArgumentException

}


case class CreationReason(code: String) {
  sealed trait Reason

  case object New extends Reason
  case object Transferred extends Reason

  val reasons = Set (New, Transferred)

  def apply (code: String) =
    code.toUpperCase match {
      case New => New
      case Transferred => New
      case _  => new IllegalArgumentException
    }

}

case class ClaimReason(reason: String) {

  val elements = Set("Life Event", "Regular Bonus")

  def apply (reason: String) =
    if (elements.contains(reason))  reason
    else throw new IllegalArgumentException
}

case class ClosureReason(closureCode: String) {

  val elements = Set ("Transferred out", "All funds withdrawn" , "Voided")

  def apply (closureCode: String) =
    if (elements.contains(closureCode))  closureCode
    else throw new IllegalArgumentException
}

case class Bonuses(bonusDueForPeriod:String,
                   totalBonusDueYTD:String,
                   bonusPaidYTD:String,
                   claimReason:ClaimReason)

case class HelpToBuyTransfer(htbTransferInForPeriod:String,htbTransferTotalYTD:String)

case class LisaManager(referenceNo: ReferenceNumber, name: String)

case class LisaInvestor(NINO:String, firstName :String, lastName:String, dob:ISO8601Date)

case class LisaAccount(investorID:ID,
                       accountID: AccountId,
                       lisaManagerReferenceNumber:ReferenceNumber,
                       creationReason:CreationReason,
                       firstSubscriptionDate:ISO8601Date,
                       transferAccount:TransferAccount,
                       accountClosureReason:ClosureReason,
                       closureDate:ISO8601Date)

case class TransferAccount(transferredFromAccountID:AccountId, transferredFromLMRN:ReferenceNumber, transferInDate:ISO8601Date)

case class LifeEvent(accountID: AccountId,
                      lisaManagerReferenceNumber:ReferenceNumber,
                      eventType:EventType,
                      eventDate:ISO8601Date
                      )

case class ISO8601Date (date:String)

case class LisaTransaction(accountID: AccountId,
                           lisaManagerReferenceNumber:ReferenceNumber,
                           eventId:ID,
                           periodStartDate:ISO8601Date,
                           periodEndDate:ISO8601Date,
                           transactionType: TransactionType,
                           bonuses: Bonuses,
                           htbTransfer:HelpToBuyTransfer
                            )



