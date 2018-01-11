/*
 * Copyright 2018 HM Revenue & Customs
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

import com.fasterxml.jackson.databind.JsonSerializer.None
import play.api.Logger
import uk.gov.hmrc.lisaapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import play.api.libs.json.{JsValue, Json, Reads}
import play.mvc.BodyParser.AnyContent
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

trait DesConnector extends ServicesConfig {


  val httpPost:HttpPost = WSHttp
  val httpPut:HttpPut = WSHttp
  val httpGet:HttpGet = WSHttp
  lazy val desUrl = baseUrl("des")
  lazy val lisaServiceUrl = s"$desUrl/lifetime-isa/manager"

  val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  private def updateHeaderCarrier(headerCarrier: HeaderCarrier) =
    headerCarrier.copy(extraHeaders = Seq(("Environment" -> AppContext.desUrlHeaderEnv)),
          authorization = Some(Authorization(s"Bearer ${AppContext.desAuthToken}")))

  /**
    * Attempts to create a new LISA investor
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)
                    (implicit hc: HeaderCarrier): Future[(Int, DesResponse)] = {
    val uri = s"$lisaServiceUrl/$lisaManager/investors"
    Logger.debug("Posting Create Investor request to des: " + uri)
    val result = httpPost.POST[CreateLisaInvestorRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Create Investor request returned status: " + res.status)
      parseDesResponse[DesCreateInvestorResponse](res)
    })
  }

  /**
    * Attempts to create a new LISA account
    */
  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)
                   (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts"
    Logger.debug("Posting Create Account request to des: " + uri)
    val result = httpPost.POST[CreateLisaAccountCreationRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Create Account request returned status: " + res.status)
      res.status match {
        case 201 => DesAccountResponse(request.accountId)
        case _ => parseDesResponse[DesFailureResponse](res)._2
      }
    })
  }

  /**
    * Attempts to get the details for LISA account
    */
  def getAccountInformation(lisaManager: String, accountId: String)
                           (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId"
    Logger.debug("Getting the Account details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(httpReads, hc = updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Account request returned status: " + res.status)
      parseDesResponse[DesGetAccountResponse](res)._2
    })
  }

  /**
    * Attempts to reinstate a LISA account
    */
  def reinstateAccount (lisaManager: String, accountId: String)
                           (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/reinstate"
    Logger.debug("Reinstate Account request returned status: " + uri)

    val result = httpPut.PUT[JsValue,HttpResponse](uri,Json.toJson(""))(implicitly,httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))
    result.map(r => {
      Logger.debug("Reinstate Account request returned status: " + r.status)
      r.status match {
        case 200 => parseDesResponse[DesReinstateAccountSuccessResponse](r)._2
        case _ => parseDesResponse[DesFailureResponse](r)._2
      }
    })
  }

  /**
    * Attempts to transfer an existing LISA account
    */
  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)
                     (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts"
    Logger.debug("Posting Create Transfer request to des: " + uri)
    val result = httpPost.POST[CreateLisaAccountTransferRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Create Transfer request returned status: " + res.status)
      res.status match {
        case 201 => DesAccountResponse(request.accountId)
        case _ => parseDesResponse[DesFailureResponse](res)._2
      }
    })
  }

  /**
    * Attempts to close a LISA account
    *
    * @return A tuple of the http status code and an (optional) data response
    */
  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)
                  (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/close-account"
    Logger.debug("Posting Close Account request to des: " + uri)
    val result = httpPost.POST[CloseLisaAccountRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(r => {
      Logger.debug("Close Account request returned status: " + r.status)
      r.status match {
        case 200 => DesEmptySuccessResponse
        case _ => parseDesResponse[DesFailureResponse](r)._2
      }
    })
  }

  /**
    * Attempts to report a LISA Life Event
    */
  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequest)
                     (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/life-event"
    Logger.debug("Posting Life Event request to des: " + uri)
    val result = httpPost.POST[ReportLifeEventRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Life Event request returned status: " + res.status)
      res.status match {
        case 409 => {
          parseDesResponse[DesLifeEventExistResponse](res)._2
        }
        case _ => parseDesResponse[DesLifeEventResponse](res)._2
      }

    })
  }

  /**
    * Attempts to update the first subscription date
    */
  def updateFirstSubDate(lisaManager: String, accountId: String, request: UpdateSubscriptionRequest)
                     (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/update-subscription"
    Logger.debug("Posting update subscription request to des: " + uri)
    val result = httpPost.POST[UpdateSubscriptionRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Update first subscription date request returned status: " + res.status)
      res.status match {
        case 200 => parseDesResponse[DesUpdateSubscriptionSuccessResponse](res)._2
        case _ => parseDesResponse[DesFailureResponse](res)._2
      }

    })
  }

  /**
    * Attempts to get a LISA Life Event
    */
  def getLifeEvent(lisaManager: String, accountId: String, eventId: String)
                  (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/life-event/$eventId"
    Logger.debug("Posting Life Event request to des: " + uri)
    val result = httpGet.GET(uri)(httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Life Event request returned status: " + res.status)
      parseDesResponse[DesLifeEventRetrievalResponse](res)._2
    })
  }

  /**
    * Attempts to request a bonus payment
    *
    * @return A tuple of the http status code and a des response
    */
  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier): Future[(Int, DesResponse)] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/bonus-claim"
    Logger.debug("Posting Bonus Payment request to des: " + uri)
    val result = httpPost.POST[RequestBonusPaymentRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Bonus Payment request returned status: " + res.status)
      parseDesResponse[DesTransactionResponse](res)
    })
  }

  /**
    * Attempts to get a submitted bonus payment's details from ITMP
    */
  def getBonusPayment(lisaManager: String, accountId: String, transactionId: String)
                     (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/transactions/$transactionId"
    Logger.debug("Getting the Bonus Payment transaction details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(httpReads, hc = updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Bonus Payment transaction details returned status: " + res.status)
      parseDesResponse[DesGetBonusPaymentResponse](res)._2
    })
  }

  /**
    * Attempts to details on a transaction from ETMP
    */
  def getTransaction(lisaManager: String, accountId: String, transactionId: String)
                    (implicit hc:HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/$accountId/transactions/$transactionId/payments"
    Logger.debug("Getting the Transaction details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(httpReads, hc = updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Transaction details returned status: " + res.status)
      parseDesResponse[DesGetTransactionResponse](res)._2
    })
  }

  // scalastyle:off magic.number
  def parseDesResponse[A <: DesResponse](res: HttpResponse)
                                        (implicit reads:Reads[A]): (Int, DesResponse) = {
    Try(res.json.as[A]) match {
      case Success(data) =>
        (res.status, data)
      case Failure(_) =>
        Try(res.json.as[DesFailureResponse]) match {
          case Success(data) => Logger.info(s"DesFailureResponse from DES :${data}")
             (res.status, data)
          case Failure(ex) => Logger.error(s"Error from DES :${ex.getMessage}")
             (500, DesFailureResponse())
        }

    }
  }

}

object DesConnector extends DesConnector
