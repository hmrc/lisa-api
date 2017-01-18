package component

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import component.steps.Env._
import cucumber.api.CucumberOptions
import cucumber.api.junit.Cucumber
import org.junit.runner.RunWith
import org.junit.{AfterClass, BeforeClass}
import play.api.test.{FakeApplication, TestServer}

@RunWith(classOf[Cucumber])
@CucumberOptions(
  features = Array("features"),
  glue = Array("component/steps"),
  format = Array("pretty",
    "html:target/component-reports/cucumber",
    "json:target/component-reports/cucumber.json"),
  tags = Array("~@wip")
)
class FeatureSuite

object FeatureSuite extends StubApplicationConfiguration {

  private lazy val testServer = new TestServer(hostPost, app)

  private lazy val app = FakeApplication(additionalConfiguration = config )

  private lazy val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))

  private var isSetup = false

  /**
   * Apparently necessary for running individual features from within IntelliJ.
   */
  def ensureSetup() = if (!isSetup) setup()

  @BeforeClass
  def setup() {
    wireMockServer.start()
    WireMock.configureFor(stubHost, stubPort)
    testServer.start()
    isSetup = true
  }

  @AfterClass
  def cleanup() {
    testServer.stop()
    wireMockServer.stop()
  }

}
