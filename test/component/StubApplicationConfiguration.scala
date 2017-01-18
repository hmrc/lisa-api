package component

import component.steps.Env
import Env.{stubHost, stubPort}

trait StubApplicationConfiguration {

  val config = Map[String, Any](
    "auditing.enabled" -> false,
    "microservice.services.datastream.host" -> stubHost,
    "microservice.services.datastream.port" -> stubPort,
    "microservice.services.datastream.enabled" -> false,
    "microservice.services.service-locator.host" -> stubHost,
    "microservice.services.service-locator.port" -> stubPort,
    "microservice.services.service-locator.enabled" -> false
  )
}
