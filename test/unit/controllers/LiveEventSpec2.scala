package unit.controllers

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.mvc.{ControllerComponents, PlayBodyParsers}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.metrics.LisaMetrics
import uk.gov.hmrc.lisaapi.models.ReportLifeEventAccountClosedResponse
import uk.gov.hmrc.lisaapi.models.des.DesFailureResponse
import uk.gov.hmrc.lisaapi.services.{AuditService, LifeEventService}
import unit.utils.DesWireMockConnector

import scala.concurrent.Future

class LiveEventServiceSpec2 extends DesWireMockConnector {

  "LiveEventController " must {}
  "return ReportLifeEventAccountClosedResponse" when {
    "the error code is INVESTOR_ACCOUNT_ALREADY_CLOSED" in {
      // do somethinf like this:
      desWireMockConnectorStub("f","K",200) mustBe 200
     }



  }

}
