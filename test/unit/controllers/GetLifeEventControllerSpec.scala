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

package unit.controllers

import java.time.LocalDate

import helpers.ControllerTestFixture
import org.mockito.ArgumentMatchers.{any, eq => matchersEquals}
import org.mockito.Mockito._
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.{ErrorAccountNotFound, GetLifeEventController}
import uk.gov.hmrc.lisaapi.models._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GetLifeEventControllerSpec extends ControllerTestFixture {

  val getLifeEventController: GetLifeEventController = new GetLifeEventController(
    mockAuthConnector,
    mockAppContext,
    mockLisaMetrics,
    mockLifeEventService,
    mockAuditService,
    mockControllerComponents,
    mockParser
  ) {
    override lazy val v2endpointsEnabled = true
  }

  val acceptHeaderV1: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val acceptHeaderV2: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")
  val lisaManager                      = "Z019283"
  val accountId                        = "ABC/12345"
  val eventId                          = "1234567890"

  override def beforeEach(): Unit = {
    reset(mockAuditService)
    when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future(Some("1234")))
  }

  "Get Life Event" should {

    "not be available for api version 1" in {
      val req = FakeRequest(Helpers.GET, "/")
      val res =
        getLifeEventController.getLifeEvent(lisaManager, accountId, eventId).apply(req.withHeaders(acceptHeaderV1))

      status(res) mustBe NOT_ACCEPTABLE
    }

    "return ok for api version 2" when {
      "given a successful response from the service layer" in {
        val annualReturn = GetLifeEventItem("12345", "STATUTORY_SUBMISSION", LocalDate.parse("2018-01-01"))

        when(mockLifeEventService.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(List(annualReturn))))

        val res = getLifeEventController
          .getLifeEvent(lisaManager, accountId, eventId)
          .apply(FakeRequest().withHeaders(acceptHeaderV2))

        status(res) mustBe OK
        contentAsJson(res) mustBe Json.toJson[Seq[GetLifeEventItem]](List(annualReturn))
      }
    }

    "return an error for api version 2" when {
      "given an error response from the service layer" in {
        when(mockLifeEventService.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(ErrorAccountNotFound)))

        val res = getLifeEventController
          .getLifeEvent(lisaManager, accountId, eventId)
          .apply(FakeRequest().withHeaders(acceptHeaderV2))

        status(res) mustBe NOT_FOUND
        contentAsJson(res) mustBe Json.obj(
          "code"    -> ErrorAccountNotFound.errorCode,
          "message" -> ErrorAccountNotFound.message
        )
      }

    }

    "audit getLifeEventReported" when {
      "given a successful response from the service layer" in {
        val annualReturn = GetLifeEventItem("12345", "STATUTORY_SUBMISSION", LocalDate.parse("2018-01-01"))
        when(mockLifeEventService.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Right(List(annualReturn))))

        val res = getLifeEventController
          .getLifeEvent(lisaManager, accountId, eventId)
          .apply(FakeRequest().withHeaders(acceptHeaderV2))
        await(res)
        verify(mockAuditService).audit(
          auditType = matchersEquals("getLifeEventReported"),
          path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events/$eventId"),
          auditData = matchersEquals(
            Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId"                  -> accountId,
              "lifeEventId"                -> eventId
            )
          )
        )(any())
      }
    }

    "audit getLifeEventNotReported" when {
      "given an error response from the service layer" in {
        when(mockLifeEventService.getLifeEvent(any(), any(), any())(any()))
          .thenReturn(Future.successful(Left(ErrorAccountNotFound)))

        val res = getLifeEventController
          .getLifeEvent(lisaManager, accountId, eventId)
          .apply(FakeRequest().withHeaders(acceptHeaderV2))
        await(res)
        verify(mockAuditService).audit(
          auditType = matchersEquals("getLifeEventNotReported"),
          path = matchersEquals(s"/manager/$lisaManager/accounts/$accountId/events/$eventId"),
          auditData = matchersEquals(
            Map(
              "lisaManagerReferenceNumber" -> lisaManager,
              "accountId"                  -> accountId,
              "lifeEventId"                -> eventId,
              "reasonNotReported"          -> ErrorAccountNotFound.errorCode
            )
          )
        )(any())
      }
    }

  }
}
