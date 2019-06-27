
package unit.utils

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig
import com.google.inject.Singleton
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Suite}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.Injector
import uk.gov.hmrc.lisaapi.config.APIAccessConfig
import uk.gov.hmrc.play.bootstrap.http.HttpClient


@Singleton
trait WireMockHelper extends BaseSpec with BeforeAndAfterAll with BeforeAndAfterEach with GuiceOneAppPerSuite{
  this: Suite =>
  protected lazy val injector: Injector = app.injector
  val config: APIAccessConfig = injector.instanceOf[APIAccessConfig]
  val http: HttpClient = injector.instanceOf[HttpClient]
  protected val server: WireMockServer = new WireMockServer(wireMockConfig().dynamicPort())
  override def beforeAll(): Unit = {
    server.start()
    WireMock.configureFor("localhost", server.port())
    super.beforeAll()
  }
  override def beforeEach(): Unit = {
    server.resetAll()
    super.beforeEach()
  }
  override def afterAll(): Unit = {
    server.stop()
    super.afterAll()
  }
}
