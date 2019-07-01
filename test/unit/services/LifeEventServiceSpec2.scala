/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.services

import com.google.inject.Inject
import org.joda.time.DateTime
import play.api.test.Injecting
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.lisaapi.controllers.LifeEventController
import uk.gov.hmrc.lisaapi.models.{LifeEvent, ReportLifeEventRequest, ReportLifeEventResponse, ReportLifeEventSuccessResponse}
import uk.gov.hmrc.lisaapi.models.des.{DesLifeEventResponse, DesResponse}
import uk.gov.hmrc.lisaapi.services.LifeEventService
import unit.utils.DesWireMockConnector

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class LifeEventServiceSpec2 extends DesWireMockConnector with Injecting {



  "Report life event" must {
      "return DesLifeEventResponse" when {
        "given a success response from the DES connector" in {
          val respData = "{" + "\"lifeEventID\": \"1234567891\"}"
          desWireMockConnectorStub("/lifetime-isa/manager/Z019283/accounts/192837/life-event", respData, 201)
          val lifeEventService = new LifeEventService(DesConnector2)
          val responseFuture: Future[ReportLifeEventResponse] = lifeEventService.reportLifeEvent("Z019283", "192837", ReportLifeEventRequest("LISA Investor Terminal Ill Health", new DateTime("2017-04-06")))(HeaderCarrier())
          Await.result(responseFuture, Duration.Inf) mustBe ReportLifeEventSuccessResponse("1234567891")
        }
      }

      "return ReportLifeEventAlreadyExistsResponse" when {
        "the error code is LIFE_EVENT_ALREADY_EXISTS" in {
          ???
        }
      }

      "return ReportLifeEventAlreadySupersededResponse" when {
        "the error code is SUPERSEDED_LIFE_EVENT_ALREADY_SUPERSEDED" in {
         ???
        }
      }

      "return ReportLifeEventInappropriateResponse" when {
        "the error code is LIFE_EVENT_INAPPROPRIATE" in {
          ???
        }
      }

      "return ReportLifeEventAccountClosedOrVoidResponse" when {
        "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED_OR_VOID" in {
          ???
        }
      }

      "return ReportLifeEventAccountClosedResponse" when {
        "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED" in {
          ???
        }
      }

      "return ReportLifeEventAccountVoidResponse" when {
        "the error code is INVESTOR_ACCOUNT_ALREADY_VOID" in {
          ???
        }
      }

      "return ReportLifeEventAccountCancelledResponse" when {
        "the error code is INVESTOR_ACCOUNT_ALREADY_CANCELLED" in {
          ???
        }
      }

      "return ReportLifeEventAccountNotFoundResponse" when {
        "the error code is INVESTOR_ACCOUNTID_NOT_FOUND" in {
          ???
        }
      }

      "return ReportLifeEventMismatchResponse" when {
        "the error code is SUPERSEDING_LIFE_EVENT_MISMATCH" in {
          ???
        }
      }

      "return ReportLifeEventAccountNotOpenLongEnoughResponse" when {
        "the error code is COMPLIANCE_ERROR_ACCOUNT_NOT_OPEN_LONG_ENOUGH" in {
          ???
        }
      }

      "return ReportLifeEventOtherPurchaseOnRecordResponse" when {
        "the error code is COMPLIANCE_ERROR_OTHER_PURCHASE_ON_RECORD" in {
          ???
        }
      }

      "return ReportLifeEventFundReleaseSupersededResponse" when {
        "the error code is FUND_RELEASE_LIFE_EVENT_ID_SUPERSEDED" in {
          ???
        }
      }

      "return ReportLifeEventFundReleaseNotFoundResponse" when {
        "the error code is FUND_RELEASE_LIFE_EVENT_ID_NOT_FOUND" in {
          ???
        }
      }

      "return ReportLifeEventExtensionOneAlreadyApprovedResponse" when {
        "the error code is PURCHASE_EXTENSION_1_LIFE_EVENT_ALREADY_APPROVED" in {
          ???
        }
      }

      "return ReportLifeEventExtensionTwoAlreadyApprovedResponse" when {
        "the error code is PURCHASE_EXTENSION_2_LIFE_EVENT_ALREADY_APPROVED" in {
          ???
      }

      "return ReportLifeEventExtensionOneNotYetApprovedResponse" when {
        "the error code is PURCHASE_EXTENSION_1_LIFE_EVENT_NOT_YET_APPROVED" in {
         ???
        }
      }

      "return ReportLifeEventServiceUnavailableResponse" when {
        "a DesUnavailableResponse is received" in {
         ???
        }
      }

      "return ReportLifeEventErrorResponse" when {
        "the error code doesn't match any of the previous values" in {
          ???
        }
      }

    }

    "Get life event" must {

      "return a Left with a appropriate ErrorResponse" when {

        "a INVESTOR_ACCOUNT_ID_NOT_FOUND error code is returned" in {
          ???
        }

        "a LIFE_EVENT_ID_NOT_FOUND error code is returned" in {
          ???
        }

        "a DesUnavailableResponse is returned" in {
          ???
        }

        "any other error code is returned" in {
          ???
        }

      }

      "return a Right with a Seq GetLifeEventItem" when {

        "a Seq GetLifeEventItem is returned" in {
          ???
          }
        }
      }

    }



}
