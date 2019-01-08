/*
 * Copyright 2019 HM Revenue & Customs
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

import org.joda.time.DateTime
import play.api.http.Status._
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Reads}
import play.utils.UriEncoding
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.logging.Authorization
import uk.gov.hmrc.lisaapi.config.{AppContext, WSHttp}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

trait DesConnector extends ServicesConfig {


  val httpPost:HttpPost = WSHttp
  val httpPut:HttpPut = WSHttp
  val httpGet:HttpGet = WSHttp
  val urlEncodingFormat:String = "utf-8"
  lazy val desUrl = baseUrl("des")
  lazy val lisaServiceUrl = s"$desUrl/lifetime-isa/manager"

  val httpReads: HttpReads[HttpResponse] = new HttpReads[HttpResponse] {
    override def read(method: String, url: String, response: HttpResponse) = response
  }

  private def updateHeaderCarrier(headerCarrier: HeaderCarrier) =
    headerCarrier.copy(extraHeaders = Seq(("Environment" -> AppContext.desUrlHeaderEnv)),
          authorization = Some(Authorization(s"Bearer ${AppContext.desAuthToken}")))

  private def updateHeaderCarrierWithAllDesHeaders(headerCarrier: HeaderCarrier) =
    headerCarrier.copy(extraHeaders = Seq(("Environment" -> AppContext.desUrlHeaderEnv), ("OriginatorId" -> "DA2_LISA")),
      authorization = Some(Authorization(s"Bearer ${AppContext.desAuthToken}")))

  /**
    * Attempts to create a new LISA investor
    */
  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)
                    (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/investors"
    Logger.debug("Posting Create Investor request to des: " + uri)
    val result = httpPost.POST[CreateLisaInvestorRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Create Investor request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case CONFLICT => parseDesResponse[CreateLisaInvestorAlreadyExistsResponse](res)
        case _ => parseDesResponse[CreateLisaInvestorSuccessResponse](res)
      }
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
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case CREATED => DesAccountResponse(request.accountId)
        case _ => parseDesResponse[DesFailureResponse](res)
      }
    })
  }

  /**
    * Attempts to get the details for LISA account
    */
  def getAccountInformation(lisaManager: String, accountId: String)
                           (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}"
    Logger.debug("Getting the Account details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(httpReads, hc = updateHeaderCarrierWithAllDesHeaders(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => {
          parseDesResponse[GetLisaAccountSuccessResponse](res) match {
            case success: GetLisaAccountSuccessResponse => success.copy(accountId = accountId)
            case fail: DesResponse => fail
          }
        }
      }
    })
  }

  /**
    * Attempts to reinstate a LISA account
    */
  def reinstateAccount(lisaManager: String, accountId: String)
                      (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/reinstate"
    Logger.debug("Reinstate Account request returned status: " + uri)

    val result = httpPut.PUT[JsValue,HttpResponse](uri,Json.toJson(""))(implicitly,httpReads, updateHeaderCarrierWithAllDesHeaders(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))
    result.map(res => {
      Logger.debug("Reinstate Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case OK => parseDesResponse[DesReinstateAccountSuccessResponse](res)
        case _ => parseDesResponse[DesFailureResponse](res)
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
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case CREATED => DesAccountResponse(request.accountId)
        case _ => parseDesResponse[DesFailureResponse](res)
      }
    })
  }

  /**
    * Attempts to close a LISA account
    */
  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)
                  (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/close-account"
    Logger.debug("Posting Close Account request to des: " + uri)
    val result = httpPost.POST[CloseLisaAccountRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Close Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case OK => DesEmptySuccessResponse
        case _ => parseDesResponse[DesFailureResponse](res)
      }
    })
  }

  /**
    * Attempts to report a LISA Life Event
    */
  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequestBase)
                     (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/life-event"
    Logger.debug("Posting Life Event request to des: " + uri)
    val result = httpPost.POST[ReportLifeEventRequestBase, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Life Event request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[DesLifeEventResponse](res)
      }

    })
  }

  /**
    * Attempts to get a LISA Life Event
    */
  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: LifeEventId)
                  (implicit hc: HeaderCarrier): Future[Either[DesFailure, Seq[GetLifeEventItem]]] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/life-events/$lifeEventId"
    Logger.debug("Getting life event from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(
      httpReads,
      hc = updateHeaderCarrierWithAllDesHeaders(hc),
      MdcLoggingExecutionContext.fromLoggingDetails(hc)
    )

    result.map(res => {
      Logger.debug("Get life event returned status: " + res.status)

      res.status match {
        case SERVICE_UNAVAILABLE => Left(DesUnavailableResponse)
        case _ => {
          Try(res.json.as[Seq[GetLifeEventItem]]) match {
            case Success(data) => Right(data)
            case Failure(er) => {
              if (res.status == 200 | res.status == 201) {
                Logger.error(s"Error from DES (parsing as DesResponse): ${er.getMessage}")
              }

              Try(res.json.as[DesFailureResponse]) match {
                case Success(data) => {
                  Logger.info(s"DesFailureResponse from DES: ${data}")
                  Left(data)
                }
                case Failure(ex) => {
                  Logger.error(s"Error from DES (parsing as DesFailureResponse): ${ex.getMessage}")
                  Left(DesFailureResponse())
                }
              }
            }
          }
        }
      }
    })
  }

  /**
    * Attempts to update the first subscription date
    */
  def updateFirstSubDate(lisaManager: String, accountId: String, request: UpdateSubscriptionRequest)
                        (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}"
    Logger.debug("Posting update subscription request to des: " + uri)
    val result = httpPut.PUT[UpdateSubscriptionRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrierWithAllDesHeaders(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Update first subscription date request returned status: " + res.status)
      res.status match {
        case OK => parseDesResponse[DesUpdateSubscriptionSuccessResponse](res)
        case CONFLICT => parseDesResponse[DesTransactionExistResponse](res)
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[DesFailureResponse](res)
      }

    })
  }

  /**
    * Attempts to request a bonus payment
    *
    * @return A tuple of the http status code and a des response
    */
  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)
                         (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/bonus-claim"
    Logger.debug("Posting Bonus Payment request to des: " + uri)
    val result = httpPost.POST[RequestBonusPaymentRequest, HttpResponse](uri, request)(implicitly, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Bonus Payment request returned status: " + res.status)
      res.status match {
        case CONFLICT => parseDesResponse[DesTransactionExistResponse](res)
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[DesTransactionResponse](res)
      }
    })
  }

  /**
    * Attempts to get a submitted bonus payment's details from ITMP
    */
  def getBonusOrWithdrawal(lisaManager: String, accountId: String, transactionId: String)
                          (implicit hc: HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/transaction/$transactionId"
    Logger.debug("Getting the Bonus Payment transaction details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(httpReads, hc = updateHeaderCarrierWithAllDesHeaders(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Bonus Payment transaction details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[GetBonusOrWithdrawalResponse](res)
      }
    })
  }


  /**
    * Attempts to report a withdrawal charge
    *
    * @return A tuple of the http status code and a des response
    */
  def reportWithdrawalCharge(lisaManager: String, accountId: String, request: ReportWithdrawalChargeRequest)
                            (implicit hc: HeaderCarrier): Future[DesResponse] = {

    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/withdrawal"
    Logger.debug("Posting withdrawal request to des: " + uri)

    val result = httpPost.POST[ReportWithdrawalChargeRequest, HttpResponse](uri, request)(ReportWithdrawalChargeRequest.desReportWithdrawalChargeWrites, httpReads, updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Withdrawal request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[DesTransactionResponse](res)
      }
    })
  }

  /**
    * Attempts to details on a transaction from ETMP
    */
  def getTransaction(lisaManager: String, accountId: String, transactionId: String)
                    (implicit hc:HeaderCarrier): Future[DesResponse] = {
    val uri = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/transaction/$transactionId/bonusChargeDetails"
    Logger.debug("Getting the Transaction details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(httpReads, hc = updateHeaderCarrier(hc), MdcLoggingExecutionContext.fromLoggingDetails(hc))

    result.map(res => {
      Logger.debug("Get Transaction details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[DesGetTransactionResponse](res)
      }
    })
  }

  def getBulkPayment(lisaManager: String, startDate: DateTime, endDate: DateTime)
                    (implicit hc:HeaderCarrier): Future[DesResponse] = {
    val uri = s"$desUrl/enterprise/financial-data/ZISA/$lisaManager/LISA" +
      s"?dateFrom=${startDate.toString("yyyy-MM-dd")}" +
      s"&dateTo=${endDate.toString("yyyy-MM-dd")}" +
      "&onlyOpenItems=false"

    Logger.debug("Getting Bulk payment details from des: " + uri)

    val result: Future[HttpResponse] = httpGet.GET(uri)(
      httpReads,
      hc = updateHeaderCarrierWithAllDesHeaders(hc),
      MdcLoggingExecutionContext.fromLoggingDetails(hc)
    )

    result.map(res => {
      Logger.debug("Get Bulk payment details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _ => parseDesResponse[GetBulkPaymentResponse](res)
      }
    })
  }

  // scalastyle:off magic.number
  def parseDesResponse[A <: DesResponse](res: HttpResponse)
                                        (implicit reads:Reads[A]): DesResponse = {
    Try(res.json.as[A]) match {
      case Success(data) =>
        data
      case Failure(er) =>
        if (res.status == 200 | res.status == 201) {
          Logger.error(s"Error from DES (parsing as DesResponse): ${er.getMessage}")
        }

        Try(res.json.as[DesFailureResponse]) match {
          case Success(data) => Logger.info(s"DesFailureResponse from DES: ${data}")
             data
          case Failure(ex) => Logger.error(s"Error from DES (parsing as DesFailureResponse): ${ex.getMessage}")
             DesFailureResponse()
        }
    }
  }

}

object DesConnector extends DesConnector
