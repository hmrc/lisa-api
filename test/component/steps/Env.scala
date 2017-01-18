package component.steps

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import component.FeatureSuite
import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers

trait Env extends ScalaDsl with EN with Matchers {

  val hostPost = 9000
  val host = sys.env.getOrElse("HOST", s"http://localhost:$hostPost")

  val stubPort = sys.env.getOrElse("WIREMOCK_PORT", "11111").toInt
  val stubHost = "localhost"

  val wireMockUrl = s"http://$stubHost:$stubPort"
  final val wireMockServer = new WireMockServer(wireMockConfig().port(stubPort))


  Before { scenario =>
    FeatureSuite.ensureSetup
  }

  After { scenario =>
    WireMock.reset()
  }

}

object Env extends Env