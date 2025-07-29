/*
 * Copyright 2023 HM Revenue & Customs
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

import com.fasterxml.jackson.core.JsonParseException
import com.google.inject.Inject
import play.api.Logging
import play.api.http.Status._
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.utils.UriEncoding
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.models.des._
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw

import java.time.LocalDate
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class DesConnector @Inject() (
                               wsHttp: HttpClientV2,
                               appContext: AppContext
)(implicit ec: ExecutionContext)
    extends Logging {

  val urlEncodingFormat: String   = "utf-8"
  lazy val lisaServiceUrl: String = s"${appContext.desUrl}/lifetime-isa/manager"


  private def desHeaders(implicit hc: HeaderCarrier): Seq[(String, String)] = Seq(
    "Environment"   -> appContext.desUrlHeaderEnv,
    "Authorization" -> s"Bearer ${appContext.desAuthToken}",
    "CorrelationId" -> correlationId
  )

  private def correlationId(implicit hc: HeaderCarrier): String = {
    val CorrelationIdPattern = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r
    hc.requestId match {
      case Some(requestId) =>
        requestId.value match {
          case CorrelationIdPattern(prefix) => prefix + "-" + randomUUID.toString.substring(24)
          case _                            => randomUUID.toString
        }
      case _               => randomUUID.toString
    }
  }

  private def desHeadersWithOriginator(implicit hc: HeaderCarrier): Seq[(String, String)] =
    desHeaders :+ ("OriginatorId" -> "DA2_LISA")

  /** Attempts to create a new LISA investor
    */
  def createInvestor(lisaManager: String, request: CreateLisaInvestorRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {
    val fullUrl: String = s"$lisaServiceUrl/$lisaManager/investors"
    logger.info("Posting Create Investor request to des: " + fullUrl)

    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Create Investor request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case BAD_REQUEST         => DesBadRequestResponse
        case CONFLICT            => parseDesResponse[CreateLisaInvestorAlreadyExistsResponse](res)
        case _                   => parseDesResponse[CreateLisaInvestorSuccessResponse](res)
      }
    }
  }

  /** Attempts to create a new LISA account
    */
  def createAccount(lisaManager: String, request: CreateLisaAccountCreationRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {
    val fullUrl    = s"$lisaServiceUrl/$lisaManager/accounts"
    logger.info("Posting Create Account request to des: " + fullUrl)
    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]

    result.map { res =>
      logger.info("Create Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case BAD_REQUEST         => DesBadRequestResponse
        case CREATED             => DesAccountResponse(request.accountId)
        case _                   => parseDesResponse[DesFailureResponse](res)
      }
    }
  }

  /** Attempts to get the details for LISA account
    */
  def getAccountInformation(lisaManager: String, accountId: String)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val fullUrl = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}"
    logger.info("Getting the Account details from des: " + fullUrl)
    val result = wsHttp.get(url"$fullUrl")
      .setHeader(desHeadersWithOriginator:_*)
      .execute[HttpResponse]

    result.map { res =>
      logger.info("Get Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _                   =>
          parseDesResponse[GetLisaAccountSuccessResponse](res) match {
            case success: GetLisaAccountSuccessResponse => success.copy(accountId = accountId)
            case fail: DesResponse                      => fail
          }
      }
    }
  }

  /** Attempts to reinstate a LISA account
    */
  def reinstateAccount(lisaManager: String, accountId: String)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val fullUrl =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/reinstate"
    logger.info("Reinstate Account request returned status: " + fullUrl)
    val result = wsHttp.put(url"$fullUrl")
      .setHeader(desHeadersWithOriginator:_*)
      .withBody(Json.toJson(""))
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Reinstate Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case BAD_REQUEST         => DesBadRequestResponse
        case OK                  => parseDesResponse[DesReinstateAccountSuccessResponse](res)
        case _                   => parseDesResponse[DesFailureResponse](res)
      }
    }
  }

  /** Attempts to transfer an existing LISA account
    */
  def transferAccount(lisaManager: String, request: CreateLisaAccountTransferRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {
    val fullUrl    = s"$lisaServiceUrl/$lisaManager/accounts"
    logger.info("Posting Create Transfer request to des: " + fullUrl)
    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]

    result.map { res =>
      logger.info("Create Transfer request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case BAD_REQUEST         => DesBadRequestResponse
        case CREATED             => DesAccountResponse(request.accountId)
        case _                   => parseDesResponse[DesFailureResponse](res)
      }
    }
  }

  /** Attempts to close a LISA account
    */
  def closeAccount(lisaManager: String, accountId: String, request: CloseLisaAccountRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {
    val fullUrl    =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/close-account"
    logger.info("Posting Close Account request to des: " + fullUrl)
    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Close Account request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case BAD_REQUEST         => DesBadRequestResponse
        case OK                  => DesEmptySuccessResponse
        case _                   => parseDesResponse[DesFailureResponse](res)
      }
    }
  }

  /** Attempts to report a LISA Life Event
    */
  def reportLifeEvent(lisaManager: String, accountId: String, request: ReportLifeEventRequestBase)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {

    val fullUrl    =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/life-event"
    logger.info("Posting Life Event request to des: " + fullUrl)
    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Life Event request returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _                   => parseDesResponse[DesLifeEventResponse](res)
      }

    }
  }

  /** Attempts to get a LISA Life Event
    */
  def getLifeEvent(lisaManager: String, accountId: String, lifeEventId: LifeEventId)(implicit
    hc: HeaderCarrier
  ): Future[Either[DesFailure, Seq[GetLifeEventItem]]] = {

    val fullUrl =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/life-events/$lifeEventId"
    logger.info("Getting life event from des: " + fullUrl)


    val result = wsHttp.get(url"$fullUrl")
      .setHeader(desHeadersWithOriginator:_*)
      .execute[HttpResponse]

    result.map { res =>
      logger.info("Get life event returned status: " + res.status)

      res.status match {
        case SERVICE_UNAVAILABLE => Left(DesUnavailableResponse)
        case _                   =>
          Try(res.json.as[Seq[GetLifeEventItem]]) match {
            case Success(data) => Right(data)
            case Failure(er)   =>
              if (res.status == 200 | res.status == 201) {
                logger.error(s"Error from DES (parsing as DesResponse): ${er.getMessage}")
              }

              Try(res.json.as[DesFailureResponse]) match {
                case Success(data) =>
                  logger.info(s"DesFailureResponse from DES: $data")
                  Left(data)
                case Failure(ex)   =>
                  logger.error(s"Error from DES (parsing as DesFailureResponse): ${ex.getMessage}")
                  Left(DesFailureResponse())
              }
          }
      }
    }
  }

  /** Attempts to update the first subscription date
    */
  def updateFirstSubDate(lisaManager: String, accountId: String, request: UpdateSubscriptionRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {

    val fullUrl    = s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}"
    logger.info("Posting update subscription request to des: " + fullUrl)
    val result = wsHttp.put(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeadersWithOriginator:_*)
      .execute[HttpResponse]

    result.map { res =>
      logger.info("Update first subscription date request returned status: " + res.status)
      res.status match {
        case OK                  => parseDesResponse[DesUpdateSubscriptionSuccessResponse](res)
        case BAD_REQUEST         => DesBadRequestResponse
        case CONFLICT            => parseDesResponse[DesTransactionExistResponse](res)
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _                   => parseDesResponse[DesFailureResponse](res)
      }

    }
  }

  /** Attempts to request a bonus payment
    *
    * @return
    *   A tuple of the http status code and a des response
    */
  def requestBonusPayment(lisaManager: String, accountId: String, request: RequestBonusPaymentRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {

    val fullUrl    =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/bonus-claim"
    logger.info("Posting Bonus Payment request to des: " + fullUrl)
    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]
    result
      .map { res =>
        logger.info("Bonus Payment request returned status: " + res.status)
        res.status match {
          case CONFLICT            => parseDesResponse[DesTransactionExistResponse](res)
          case BAD_REQUEST         => DesBadRequestResponse
          case SERVICE_UNAVAILABLE => DesUnavailableResponse
          case _                   => parseDesResponse[DesTransactionResponse](res)
        }
      }
      .recover {
        case response: UpstreamErrorResponse =>
          if (response.reportAs == 499) {
            logger.error(s"[DesConnector][requestBonusPayment] Service unavailable")
            DesUnavailableResponse
          } else {
            logger.error(s"[DesConnector][requestBonusPayment] Upstream Des error")
            DesFailureResponse(response.message, response.message)
          }
        case th: Throwable                   =>
          logger.error(s"[DesConnector][requestBonusPayment] Des failure error: " + th.getMessage)
          DesFailureResponse()
      }
  }

  /** Attempts to get a submitted bonus payment's details from ITMP
    */
  def getBonusOrWithdrawal(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {
    val fullUrl =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/transaction/$transactionId"
    logger.info("Getting the Bonus Payment transaction details from des: " + fullUrl)

    val result = wsHttp.get(url"$fullUrl")
      .setHeader(desHeadersWithOriginator:_*)
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Get Bonus Payment transaction details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _                   => parseDesResponse[GetBonusOrWithdrawalResponse](res)
      }
    }
  }

  /** Attempts to report a withdrawal charge
    *
    * @return
    *   A tuple of the http status code and a des response
    */
  def reportWithdrawalCharge(lisaManager: String, accountId: String, request: ReportWithdrawalChargeRequest)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {

    val fullUrl =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/withdrawal"
    logger.info("Posting withdrawal request to des: " + fullUrl)

    val result = wsHttp.post(url"$fullUrl")
      .withBody(Json.toJson(request)(ReportWithdrawalChargeRequest.desReportWithdrawalChargeWrites))
      .setHeader(desHeaders:_*)
      .execute[HttpResponse]


    result.map { res =>
      logger.info("Withdrawal request returned status: " + res.status)
      (res.status, res.body != null && res.body.contains("supersededTransactionByID")) match {
        case (CONFLICT, _)            => parseDesResponse[DesWithdrawalChargeAlreadyExistsResponse](res)
        case (BAD_REQUEST, _)         => DesBadRequestResponse
        case (FORBIDDEN, true)        => parseDesResponse[DesWithdrawalChargeAlreadySupersededResponse](res)
        case (SERVICE_UNAVAILABLE, _) => DesUnavailableResponse
        case _                        => parseDesResponse[DesTransactionResponse](res)
      }
    }
  }

  /** Attempts to details on a transaction from ETMP
    */
  def getTransaction(lisaManager: String, accountId: String, transactionId: String)(implicit
    hc: HeaderCarrier
  ): Future[DesResponse] = {
    val fullUrl =
      s"$lisaServiceUrl/$lisaManager/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/transaction/$transactionId/bonusChargeDetails"
    logger.info("Getting the Transaction details from des: " + fullUrl)

    val result = wsHttp.get(url"$fullUrl")
      .setHeader(desHeadersWithOriginator:_*)
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Get Transaction details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _                   => parseDesResponse[DesGetTransactionResponse](res)
      }
    }
  }

  def getBulkPayment(lisaManager: String, startDate: LocalDate, endDate: LocalDate)(implicit hc: HeaderCarrier): Future[DesResponse] = {
    val fullUrl = s"${appContext.desUrl}/enterprise/financial-data/ZISA/$lisaManager/LISA" +
      s"?dateFrom=${startDate.toString}" +
      s"&dateTo=${endDate.toString}" +
      "&onlyOpenItems=false"

    logger.info("Getting Bulk payment details from des: " + fullUrl)

    val result = wsHttp.get(url"$fullUrl")
      .setHeader(desHeadersWithOriginator:_*)
      .execute[HttpResponse]
    result.map { res =>
      logger.info("Get Bulk payment details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => DesUnavailableResponse
        case _                   => parseDesResponse[GetBulkPaymentResponse](res)
      }
    }
  }

  // scalastyle:off magic.number

  def parseDesResponse[A <: DesResponse](res: HttpResponse)(implicit reads: Reads[A]): DesResponse = {
    val contentTypeHeader = res.headers.getOrElse("Content-Type", Seq.empty[String])
    if (contentTypeHeader.contains("application/json")){
      try {
        res.json.validate[A] match {
          case JsSuccess(value, _) => value
          case JsError(er) =>
            if (res.status == 200 | res.status == 201) {
              logger.error(s"Error from DES (parsing as DesResponse): ${er.mkString(", ")}")
            }
            Json.fromJson[DesFailureResponse](res.json) match {
              case JsSuccess(data, _) =>
                logger.info(s"DesFailureResponse from DES: $data")
                data
              case JsError(ex) =>
                logger.error(s"Error from DES (parsing as DesFailureResponse): ${ex.mkString(", ")}")
                DesFailureResponse()
            }
        }
      } catch {
        case _: JsonParseException =>
          logger.error(s"Invalid JSON received from DES. Status: ${res.status}, Body: ${res.body}")
          DesFailureResponse()
      }
    }
    else{
      logger.error(s"Error from DES (parsing as DesFailureResponse): Received non-JSON content from DES, status: ${res.status}")
      DesFailureResponse()
    }
  }
}
