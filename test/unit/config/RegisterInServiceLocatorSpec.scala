package unit.config

import org.scalatest.mock.MockitoSugar
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class RegisterInServiceLocatorSpec extends UnitSpec with MockitoSugar with OneAppPerSuite {

  override implicit lazy val app: Application = new GuiceApplicationBuilder().build()

  trait Setup extends ServiceLocatorRegistration {
    val mockConnector = mock[ServiceLocatorConnector]
    override val slConnector = mockConnector
    override implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "onStart" should {
    "register the microservice in service locator when registration is enabled" in new Setup {
      override val registrationEnabled: Boolean = true
      when(mockConnector.register(any())).thenReturn(Future.successful(true))
      onStart(app)
      verify(mockConnector).register(any())
    }


    "not register the microservice in service locator when registration is disabled" in new Setup {
      override val registrationEnabled: Boolean = false
      onStart(app)
      verify(mockConnector, never()).register(any())
    }
  }
}
