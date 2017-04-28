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

package unit.controllers

import org.joda.time.DateTime
import org.mockito.Matchers._
import org.mockito.Matchers.{eq => matchersEquals, _}
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import org.scalatest._
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsJson
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers._
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse
import uk.gov.hmrc.lisaapi.services.{AuditService, InvestorService}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

class InvestorControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfterEach {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  val investorJson = """{
                         "investorNINO" : "AB123456D",
                         "firstName" : "Ex first Name",
                         "lastName" : "Ample",
                         "dateOfBirth" : "1973-03-24"
                       }""".stripMargin

  val invalidInvestorJson = """{
                         "investorNINO" : 123456,
                         "firstName" : "Ex first Name",
                         "lastName" : "Ample",
                         "dateOfBirth" : "1973-03-24"
                       }""".stripMargin

  val lisaManager = "Z019283"

  override def beforeEach() {
    reset(mockAuditService)
    reset(mockService)
  }

  "The Investor Controller" should {

    "return with status 201 created" in {
      when(mockService.createInvestor(any(), any())(any())).thenReturn(Future.successful(CreateLisaInvestorSuccessResponse("AB123456")))

      val res = SUT.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT,"/").withHeaders(acceptHeader).
        withBody(AnyContentAsJson(Json.parse(investorJson))))

      status(res) must be (CREATED)
    }

    "return with status 406 not acceptable" when {
      "given an invalid accept header" in {
        when(mockService.createInvestor(any(), any())(any())).thenReturn(Future.successful(CreateLisaInvestorSuccessResponse("AB123456")))
        val res = SUT.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT, "/").withHeaders(("accept", "application/vnd.hmrcc.1.0+json")))
        status(res) must be(406)
      }
    }

    "return with status 403 FORBIDDEN" when {
      "given the the Investor details cannot be found" in {
        when(mockService.createInvestor(any(), any())(any())).
          thenReturn(Future.successful(CreateLisaInvestorErrorResponse(403,
            DesFailureResponse("INVESTOR_NOT_FOUND","The investor details given do not match with HMRC’s records"))))
        val res = SUT.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT,"/").withHeaders(acceptHeader).
          withBody(AnyContentAsJson(Json.parse(investorJson))))
        status(res) must be (FORBIDDEN)
      }
    }

    "convert names to uppercase" when {
      "given standard a-z characters" in {
        when(mockService.createInvestor(any(), any())(any())).thenReturn(Future.successful(CreateLisaInvestorSuccessResponse("AB123456")))

        val json = investorJson.replace("Ex first Name", "rick").replace("Ample", "Sanchez")

        val res = SUT.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT,"/").withHeaders(acceptHeader).
          withBody(AnyContentAsJson(Json.parse(json))))

        await(res)

        verify(mockService).createInvestor(matchersEquals(lisaManager), matchersEquals(CreateLisaInvestorRequest(
          investorNINO = "AB123456D",
          firstName = "RICK",
          lastName = "SANCHEZ",
          dateOfBirth = new DateTime("1973-03-24"))
        ))(any())
      }
    }

    "reject names" when {
      "they contain diacritics" in {
        when(mockService.createInvestor(any(), any())(any())).thenReturn(Future.successful(CreateLisaInvestorSuccessResponse("AB123456")))

        val json = investorJson.replace("Ex first Name", "riçk").replace("Ample", "Sånchez")

        val res = SUT.createLisaInvestor(lisaManager).apply(FakeRequest(Helpers.PUT,"/").withHeaders(acceptHeader).
          withBody(AnyContentAsJson(Json.parse(json))))

        status(res) mustBe BAD_REQUEST

        verify(mockService, times(0)).createInvestor(any(), any())(any())
      }
    }

    "audit an investorNotCreated event" when {
      "a investor not found response is returned" in {
        when(mockService.createInvestor(any(), any())(any())).
          thenReturn(Future.successful(CreateLisaInvestorErrorResponse(403,
            DesFailureResponse("INVESTOR_NOT_FOUND","The investor details given do not match with HMRC’s records"))))

        await(SUT.createLisaInvestor(lisaManager).
          apply(FakeRequest(Helpers.PUT, "/").
            withHeaders(acceptHeader).
            withBody(AnyContentAsJson(Json.parse(investorJson)))))

        verify(mockAuditService).audit(
          auditType = matchersEquals("investorNotCreated"),
          path = matchersEquals(s"/manager/$lisaManager/investors"),
          auditData = matchersEquals(Map(
            "lisaManagerReferenceNumber" -> lisaManager,
            "investorNINO" -> "AB123456D",
            "dateOfBirth" -> "1973-03-24",
            "reasonNotCreated" -> ErrorInvestorNotFound.errorCode
          )))(any())
      }

      "a investor already exists response is returned" in {
        val investorId = "9876543210"

        when(mockService.createInvestor(any(), any())(any())).
          thenReturn(Future.successful(CreateLisaInvestorErrorResponse(409,
            DesFailureResponse("INVESTOR_ALREADY_EXISTS","The investor already has a record with HMRC"))))


        await(SUT.createLisaInvestor(lisaManager).
          apply(FakeRequest(Helpers.PUT, "/").
            withHeaders(acceptHeader).
            withBody(AnyContentAsJson(Json.parse(investorJson)))))

        verify(mockAuditService).audit(
          auditType = matchersEquals("investorNotCreated"),
          path = matchersEquals(s"/manager/$lisaManager/investors"),
          auditData = matchersEquals(Map(
            "lisaManagerReferenceNumber" -> lisaManager,
            "investorNINO" -> "AB123456D",
            //            "investorID" -> investorId,
            "dateOfBirth" -> "1973-03-24",
            "reasonNotCreated" -> ErrorInvestorAlreadyExists(investorId).errorCode
          )))(any())
      }
      //
      //      "a error response is returned" in {
      //        when(mockService.createInvestor(any(), any())(any())).
      //          thenThrow(new RuntimeException)
      //
      //
      //        await(SUT.createLisaInvestor(lisaManager).
      //          apply(FakeRequest(Helpers.PUT, "/").
      //          withHeaders(acceptHeader).
      //          withBody(AnyContentAsJson(Json.parse(investorJson)))))
      //
      //        verify(mockAuditService).audit(
      //          auditType = matchersEquals("investorNotCreated"),
      //          path = matchersEquals(s"/manager/$lisaManager/investors"),
      //          auditData = matchersEquals(Map(
      //            "lisaManagerReferenceNumber" -> lisaManager,
      //            "investorNINO" -> "AB123456D",
      //            "dateOfBirth" -> "1973-03-24",
      //            "reasonNotCreated" -> ErrorInternalServerError.errorCode
      //          )))(any())
      //      }
    }

    "audit an investorCreated event" when {
      "a successful response is returned" in {
        when(mockService.createInvestor(any(), any())(any())).
          thenReturn(Future.successful(CreateLisaInvestorSuccessResponse("AB123456")))

        await(SUT.createLisaInvestor(lisaManager).
          apply(FakeRequest(Helpers.PUT,"/").
            withHeaders(acceptHeader).
            withBody(AnyContentAsJson(Json.parse(investorJson)))))

        verify(mockAuditService).audit(
          auditType = matchersEquals("investorCreated"),
          path = matchersEquals(s"/manager/$lisaManager/investors"),
          auditData = matchersEquals(Map(
            "lisaManagerReferenceNumber" -> lisaManager,
            "investorNINO" -> "AB123456D",
            "dateOfBirth" -> "1973-03-24",
            "investorID" -> "AB123456")
          ))(any())
      }
    }
  }

  val mockAuditService: AuditService = mock[AuditService]
  val mockService: InvestorService = mock[InvestorService]

  val SUT = new InvestorController {
    override val service: InvestorService = mockService
    override val auditService: AuditService = mockAuditService
  }
}
