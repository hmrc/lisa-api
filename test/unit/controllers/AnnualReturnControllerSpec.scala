/*
 * Copyright 2018 HM Revenue & Customs
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
import org.scalatest.BeforeAndAfter
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.HeaderNames
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.controllers.{AnnualReturnController, ErrorBadRequestAccountId, ErrorBadRequestLmrn}

import scala.concurrent.Future

class AnnualReturnControllerSpec extends PlaySpec
  with MockitoSugar
  with OneAppPerSuite
  with BeforeAndAfter {

  val acceptHeaderV1 = (HeaderNames.ACCEPT, "application/vnd.hmrc.1.0+json")
  val acceptHeaderV2 = (HeaderNames.ACCEPT, "application/vnd.hmrc.2.0+json")

  "Submit annual return" must {

    "return with 400 bad request" when {
      "given an invalid lmrn in the url" in {
        doRequest(lmrn = "123456") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestLmrn.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestLmrn.message
        }
      }
      "given an invalid accountId in the url" in {
        doRequest(accountId = "1234567890!") { res =>
          status(res) mustBe BAD_REQUEST

          val json = contentAsJson(res)

          (json \ "code").as[String] mustBe ErrorBadRequestAccountId.errorCode
          (json \ "message").as[String] mustBe ErrorBadRequestAccountId.message
        }
      }
    }

    "return 406 not acceptable" when {
      "the accept header is for v1.0 of the api" in {
        doRequest(acceptHeader = acceptHeaderV1){ res =>
          status(res) mustBe NOT_ACCEPTABLE
        }
      }
    }

    "return 501 not implemented" when {
      "the accept header is for v2.0 of the api" in {
        doRequest(){ res =>
          status(res) mustBe NOT_IMPLEMENTED
        }
      }
    }

  }

  private def doRequest(lmrn: String = "Z123456", accountId: String = "1234567890", acceptHeader: (String, String) = acceptHeaderV2)
                       (callback: (Future[Result]) =>  Unit): Unit = {
    val req = FakeRequest(Helpers.POST, "/")
    val res = SUT.submitReturn(lmrn, accountId).
                  apply(req.withHeaders(acceptHeader).
                  withBody(AnyContentAsEmpty))

    callback(res)
  }

  val mockAuthCon: LisaAuthConnector = mock[LisaAuthConnector]

  val SUT = new AnnualReturnController {
    override val authConnector = mockAuthCon
    override lazy val v2endpointsEnabled = true
  }

}
