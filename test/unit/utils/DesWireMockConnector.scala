package unit.utils

import com.github.tomakehurst.wiremock.client.WireMock.{get,urlPathEqualTo,aResponse}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import com.google.inject.Inject
import org.scalatest.mockito.MockitoSugar
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.{Application, Configuration, Environment}
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.connectors.DesConnector
import uk.gov.hmrc.play.bootstrap.config.RunMode
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.inject.bind
import scala.concurrent.ExecutionContext.Implicits.global
import scala.io.Source

trait DesWireMockConnector extends WireMockHelper with MockitoSugar {


  val validBonusPaymentResponseJson = Source.fromInputStream(getClass().getResourceAsStream("/json/request.valid.bonus-payment-response.json")).mkString
  val httpClient = inject[HttpClient]
  val configuration = Configuration("microservice.services.des.host" -> "", "microservice.services.des.port" -> 0)
  val environment = inject[Environment]
  val appContext = inject[AppContext]
  val runMode = inject[RunMode]


  object DesConnector extends DesConnector(httpClient, environment, appContext, configuration, runMode) {
    override val desUrl: String = s"http://localhost:${server.port()}"
  }

  def desWireMockConnectorStub(urlToGet: String, responseData: String, status: Int): StubMapping = {
    server.stubFor(get(urlPathEqualTo(urlToGet)).willReturn(
      aResponse()
        .withStatus(status)
          .withBody(responseData)
    )
    )
  }
}
