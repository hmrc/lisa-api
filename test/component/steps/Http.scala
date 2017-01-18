package component.steps

import scalaj.http.HttpRequest


object Request {

  sealed trait AcceptHeader
  case object AcceptValid extends AcceptHeader
  case object AcceptMissing extends AcceptHeader
  case object AcceptBadFormat extends AcceptHeader
  case object AcceptUndefined extends AcceptHeader

  implicit class RequestBuilder(httpRequest: HttpRequest) {
    def addAcceptHeader(acceptHeader: AcceptHeader): HttpRequest = {
      acceptHeader match {
        case AcceptMissing => httpRequest.header("Accept", "")
        case AcceptValid => httpRequest.header("Accept", "application/vnd.hmrc.1.0+json")
        case AcceptBadFormat => httpRequest.header("Accept", "application/vnd.hmrc.1.0+XML") // XML
        case AcceptUndefined => throw new scala.RuntimeException("Undefined accept in the scenario - no accept status defined")
      }
    }

  }

}


object Responses {
  val statusCodes = Map(
    "OK" -> 200,
    "UNSUPPORTED_MEDIA_TYPE" -> 415,
    "CONFLICT" -> 409,
    "REQUEST_TIMEOUT" -> 408,
    "NOT_FOUND" -> 404,
    "BAD_REQUEST" -> 400,
    "UNAUTHORIZED" -> 401,
    "NOT_ACCEPTABLE" -> 406,
    "INTERNAL_SERVER_ERROR" -> 500,
    "BAD_GATEWAY" -> 502
  )

}
