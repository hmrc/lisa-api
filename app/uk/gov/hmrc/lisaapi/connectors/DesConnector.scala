/*
 * Copyright 2017 HM Revenue & Customs
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

package uk.gov.hmrc.lisaapi.connectors

import uk.gov.hmrc.lisaapi.config.WSHttp
import uk.gov.hmrc.lisaapi.controllers.JsonFormats
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpPost, HttpReads, HttpResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

import play.api.libs.json.Reads

trait DesConnector extends ServicesConfig with JsonFormats {

  val httpPost:HttpPost = WSHttp
  lazy val desUrl = baseUrl("des")
  lazy val lisaServiceUrl = s"$desUrl/lifetime-isa/manager"

  val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  /**
    * Attempts to create a new LISA investor
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesCreateInvestorResponse])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/investors"

    val result = httpPost.POST[CreateLisaInvestorRequest, HttpResponse](uri, request)(implicitly, httpReads, implicitly)

    result.map(r => {
      // catch any NullPointerExceptions that may occur from r.json being a null
      Try(r.json.asOpt[DesCreateInvestorResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => (r.status, None)
      }
    })
  }

  /**
    * Attempts to create a new LISA account
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesAccountResponse])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/createaccount"

    val result = httpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](uri, request)(implicitly, httpReads, implicitly)

    result.map(r => {
      // catch any NullPointerExceptions that may occur from r.json being a null
      Try(r.json.asOpt[DesAccountResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => (r.status, None)
      }
    })
  }

  /**
    * Attempts to transfer a LISA account
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesAccountResponse])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/transferaccount"

    val result = httpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](uri, request)(implicitly, httpReads, implicitly)

    result.map(r => {
      // catch any NullPointerExceptions that may occur from r.json being a null
      Try(r.json.asOpt[DesAccountResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => (r.status, None)
      }
    })
  }

  /**
    * Attempts to close a LISA account
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesAccountResponse])] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/close-account"

    val result = httpPost.POST[CloseLisaAccountRequest, HttpResponse](uri, request)(implicitly, httpReads, implicitly)

    result.map(r => {
      // catch any NullPointerExceptions that may occur from r.json being a null
      Try(r.json.asOpt[DesAccountResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => (r.status, None)
      }
    })
  }

  /**
    * Attempts to report a LISA Life Event
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequest)(implicit hc: HeaderCarrier): Future[(Int, Option[DesResponse])] = {

    def returnLifeTimeId(lifeEvent: Any) = lifeEvent.asInstanceOf[DesLifeEventResponse].lifeEventID

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/events"

    val result = httpPost.POST[ReportLifeEventRequest, HttpResponse](uri, request)(implicitly, httpReads, implicitly)

    result.map(r => {
      parseDesResponse[DesLifeEventResponse](r,returnLifeTimeId)
    })
  }

  def parseDesResponse[A: Reads](r: HttpResponse, f: (Any)=>String):  (Int, Option[DesResponse])  = {
    r.status match {
      case 201 => Try(r.json.asOpt[A]) match {
        case Success(data) => data match {
          case Some(_) => (r.status, Some(DesSuccessResponse(f(data.get))))
          case None => (r.status, None)
        }
        case Failure(_) => (r.status, None)}
      case _ => Try(r.json.asOpt[DesFailureResponse]) match {
        case Success(data) => (r.status, data)
        case Failure(_) => (r.status, None)}
    }
  }
}


object DesConnector extends DesConnector {

}