package uk.gov.hmrc.lisaapi.models

sealed trait ReportLifeEventResponse

case class ReportLifeEventSuccessResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventErrorResponse extends ReportLifeEventResponse
case object ReportLifeEventInappropriateResponse extends ReportLifeEventResponse
case object ReportLifeEventAlreadyExistsResponse extends ReportLifeEventResponse

