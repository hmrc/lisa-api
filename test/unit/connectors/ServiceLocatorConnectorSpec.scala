package unit.connectors

import org.scalatest.concurrent.ScalaFutures
import org.mockito.Mockito._
import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.lisaapi.connectors.ServiceLocatorConnector
import uk.gov.hmrc.lisaapi.domain.Registration
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ServiceLocatorConnectorSpec extends UnitSpec with MockitoSugar with ScalaFutures {

  trait Setup {
    implicit val hc = HeaderCarrier()
    val serviceLocatorException = new RuntimeException

    val connector = new ServiceLocatorConnector {
      override val http = mock[HttpPost]
      override val appUrl: String = "http://api-microservice.service"
      override val appName: String = "api-microservice"
      override val serviceUrl: String = "https://SERVICE_LOCATOR"
      override val handlerOK: () => Unit = mock[Function0[Unit]]
      override val handlerError: Throwable => Unit = mock[Function1[Throwable, Unit]]
      override val metadata: Option[Map[String, String]] = Some(Map("third-party-api" -> "true"))
    }
  }

  "register" should {
    "register the JSON API Definition into the Service Locator" in new Setup {

      val registration = Registration(serviceName = "api-microservice", serviceUrl = "http://api-microservice.service", metadata = Some(Map("third-party-api" -> "true")))

      when(connector.http.POST(s"${connector.serviceUrl}/registration", registration, Seq("Content-Type"-> "application/json"))).thenReturn(Future.successful(HttpResponse(200)))

      connector.register.futureValue shouldBe true
      verify(connector.http).POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))
      verify(connector.handlerOK).apply()
      verify(connector.handlerError, never).apply(serviceLocatorException)
    }


    "fail registering in service locator" in new Setup {

      val registration = Registration(serviceName = "api-microservice", serviceUrl = "http://api-microservice.service", metadata = Some(Map("third-party-api" -> "true")))
      when(connector.http.POST(s"${connector.serviceUrl}/registration", registration, Seq("Content-Type"-> "application/json"))).thenReturn(Future.failed(serviceLocatorException))

      connector.register.futureValue shouldBe false
      verify(connector.http).POST("https://SERVICE_LOCATOR/registration", registration, Seq("Content-Type"-> "application/json"))
      verify(connector.handlerOK, never).apply()
      verify(connector.handlerError).apply(serviceLocatorException)
    }

  }
}

