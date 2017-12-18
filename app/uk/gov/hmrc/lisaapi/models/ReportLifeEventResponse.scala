/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.lisaapi.models

import org.joda.time.DateTime
import play.api.libs.functional.syntax._
import play.api.libs.json._

trait ReportLifeEventResponse

case class ReportLifeEventSuccessResponse(lifeEventId: String) extends ReportLifeEventResponse
case object ReportLifeEventErrorResponse extends ReportLifeEventResponse
case object ReportLifeEventInappropriateResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountNotFoundResponse extends ReportLifeEventResponse
case object ReportLifeEventIdNotFoundResponse extends ReportLifeEventResponse
case object ReportLifeEventAccountClosedResponse extends ReportLifeEventResponse

case class ReportLifeEventAlreadyExistsResponse (lifeEventID: String) extends ReportLifeEventResponse



case class RequestLifeEventSuccessResponse(lifeEventId: String, eventType: LifeEventType, eventDate: DateTime) extends ReportLifeEventResponse

object RequestLifeEventSuccessResponse {
  implicit val writes: Writes[RequestLifeEventSuccessResponse] = (
    (JsPath \ "lifeEventId").write[String] and
    (JsPath \ "eventType").write[String] and
    (JsPath \ "eventDate").write[String].contramap[DateTime](d => d.toString("yyyy-MM-dd"))
  ) (unlift(RequestLifeEventSuccessResponse.unapply))
}