package component.steps

import component.steps.Request.{AcceptBadFormat, AcceptMissing, AcceptValid, _}
import cucumber.api.scala.{EN, ScalaDsl}
import org.scalatest.Matchers
import play.api.libs.json.Json

import scalaj.http.Http

object World {
  var responseCode: Int = 0
  var responseBody: String = ""
  var acceptHeader: AcceptHeader = AcceptUndefined
}


class HelloWorldServiceSteps extends ScalaDsl with EN with Matchers  {

  When( """^I GET the LIVE resource '(.*)'$""") { (url: String) =>
    val response  = Http(s"${Env.host}$url").
      addAcceptHeader(World.acceptHeader).asString

    World.responseCode = response.code
    World.responseBody = response.body
  }

  When( """^I GET the SANDBOX resource '(.*)'$""") { (url: String) =>
    val response = Http(s"${Env.host}$url").
      addAcceptHeader(World.acceptHeader).asString
    World.responseCode = response.code
    World.responseBody = response.body
  }

  Then( """^the status code should be '(.*)'$""") { (st: String) =>
    Responses.statusCodes(st) shouldBe World.responseCode
  }

  Given( """^header 'Accept' is '(.*)'$""") { (acceptValue: String) =>
    World.acceptHeader = acceptValue match {
      case "not provided" => AcceptMissing
      case "bad formatted" => AcceptBadFormat
      case "valid" => AcceptValid
      case _ => throw new scala.RuntimeException("Undefined value for accept in the step")
    }
  }

  Then( """^I should receive JSON response:$""") { (expectedResponseBody: String) =>
    Json.parse(expectedResponseBody) shouldBe Json.parse(World.responseBody)
  }

}
