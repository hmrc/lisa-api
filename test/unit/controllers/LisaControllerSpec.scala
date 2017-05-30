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

import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.libs.functional.syntax._
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.mvc.{Action, AnyContent, AnyContentAsJson}
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.controllers.AccountController
import uk.gov.hmrc.lisaapi.services.{AccountService, AuditService}
import uk.gov.hmrc.lisaapi.utils.ErrorConverter
import org.mockito.Matchers._
import org.mockito.Mockito.when
import org.mockito.Mockito.verify
import org.mockito.Mockito.times
import scala.concurrent.Future

class LisaControllerSpec extends PlaySpec with MockitoSugar with OneAppPerSuite {

  val acceptHeader: (String, String) = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")

  case class TestType(prop1: String, prop2: String)

  implicit val testTypeReads: Reads[TestType] = (
    (JsPath \ "prop1").read[Int].map[String](i => throw new RuntimeException("Deliberate Test Exception")) and
      (JsPath \ "prop2").read[String]
    ) (TestType.apply _)

  "The withValidJson method" must {

      "return with an Internal Server Error" when {

        "an exception is thrown by one of our Json reads" in {
          val jsonString = """{"prop1": 123, "prop2": "123"}"""
          val res = SUT.testJsonValidator().apply(FakeRequest(Helpers.PUT, "/")
            .withHeaders(acceptHeader)
            .withBody(AnyContentAsJson(Json.parse(jsonString))))

          status(res) mustBe (INTERNAL_SERVER_ERROR)
        }
      }
  }
  val mockService = mock[AccountService]
  val mockErrorConverter = mock[ErrorConverter]

  val SUT = new AccountController {
    override val service: AccountService = mockService

    def testJsonValidator(): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
      withValidJson[TestType] { _ =>
        Future.successful(PreconditionFailed) // we don't ever want this to return
      }
    }
  }


}
