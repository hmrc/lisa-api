/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.libs.json._

case class UpdateSubscriptionRequest(firstSubscriptionDate: DateTime)

object UpdateSubscriptionRequest {

    implicit val updateSubscriptionRequestReads: Reads[UpdateSubscriptionRequest] = (__ \ "firstSubscriptionDate").
      read(JsonReads.notFutureDate).map {firstSubscriptionDate => UpdateSubscriptionRequest(new DateTime(firstSubscriptionDate)) }

    implicit val updateSubscriptionRequestWrites: Writes[UpdateSubscriptionRequest] =
      (__ \ "firstSubscriptionDate").write[String].contramap {(firstSubDate: UpdateSubscriptionRequest) => firstSubDate.firstSubscriptionDate.toString("yyyy-MM-dd") }


}

