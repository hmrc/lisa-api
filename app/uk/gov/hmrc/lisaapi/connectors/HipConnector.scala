/*
 * Copyright 2026 HM Revenue & Customs
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

import com.google.inject.{Inject, Singleton}
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{NOT_FOUND, SERVICE_UNAVAILABLE}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsError, JsSuccess, Reads}
import play.mvc.Http.{HeaderNames, MimeTypes}
import play.utils.UriEncoding
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.models.{GetTransactionResponse, LisaManagerReferenceNumber}
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesResponse}
import uk.gov.hmrc.lisaapi.models.hip.{HipFailureResponse, HipGetTransactionResponse, HipResponse, HipUnavailableResponse}

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID.randomUUID
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HipConnector @Inject() ( wsHttp: HttpClientV2,
                     appContext: AppContext)(implicit ec: ExecutionContext) extends Logging{









  val urlEncodingFormat: String = "utf-8"
  lazy val lisaServiceUrl: String = s"${appContext.hipUrl}/RESTAdapter/lisa/bonus-charge/manager"

  private def headers(implicit hc: HeaderCarrier): Seq[(String, String)] = Seq(
    "X-Originating-System" -> appContext.appName,
    "correlationid" -> correlationId,
    "X-Receipt-Date" -> DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS)),
    "X-Transmitting-System" -> "HIP"
  )

  private[connectors] def correlationId(implicit hc: HeaderCarrier): String = {
    val CorrelationIdPattern = """.*([A-Za-z0-9]{8}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}-[A-Za-z0-9]{4}).*""".r

    hc.requestId match {
      case Some(requestId) =>
        requestId.value match {
          case CorrelationIdPattern(prefix) => prefix + "-" + randomUUID.toString.substring(24)
          case _ => randomUUID.toString
        }
      case _ => randomUUID.toString
    }
  }

  def parseResponse[A <: HipResponse](res: HttpResponse)(implicit reads: Reads[A]): HipResponse = {
    val isJson = res.headers
      .getOrElse(HeaderNames.CONTENT_TYPE, Seq.empty[String])
      .map(_.toLowerCase)
      .exists(_.contains(MimeTypes.JSON.toLowerCase))

    if (isJson) {
      res.json.validate[A] match {
        case JsSuccess(value, _) => value
        case JsError(er)         =>
          if (res.status == Status.OK || res.status == Status.CREATED) {
            logger.error(
              s"[HipConnector][parseResponse] Error from HIP (parsing as HipResponse): ${er.mkString(", ")}"
            )
          }
          res.json.validate[HipFailureResponse] match {
            case JsSuccess(data, _) =>
              logger.info(s"[HipConnector][parseResponse] HipFailureResponse from HIP: $data")
              data
            case JsError(ex)        =>
              logger.error(
                s"[HipConnector][parseResponse] Error from HIP (parsing as HipFailureResponse): ${ex.mkString(", ")}"
              )
              HipFailureResponse()
          }
      }
    } else {
      
      //HIP does have valid non json response. therefore below log message is not applicable
      
//      logger.error(
//        s"[HipConnector][parseResponse] Error from HIP (parsing as HipFailureResponse): Received non-JSON content from HIP, status: ${res.status}"
//      )
      HipFailureResponse()
    }
  }
  
  
  
  
  private def headersWithOriginator(implicit hc: HeaderCarrier): Seq[(String, String)] =
    headers :+ ("OriginatorId" -> "DA2_LISA")

  def getTransaction(lisaManagerReferenceNumber: LisaManagerReferenceNumber, accountId: String, transactionId: String)(implicit
                                                                                                                       hc: HeaderCarrier
  ): Future[HipResponse] = {

     val fullUrl =
      s"$lisaServiceUrl/$lisaManagerReferenceNumber/accounts/${UriEncoding.encodePathSegment(accountId, urlEncodingFormat)}/transaction/$transactionId/bonusChargeDetails"

    logger.info("[HipConnector][getTransaction] Getting the Transaction details from hip: " + fullUrl)



    val result = wsHttp
      .get(url"$fullUrl")
      .setHeader(headersWithOriginator: _*)
      .execute[HttpResponse]

    result.map { res =>
      logger.info("[HipConnector][getTransaction] Get Transaction details returned status: " + res.status)
      res.status match {
        case SERVICE_UNAVAILABLE => HipFailureResponse(SERVICE_UNAVAILABLE.toString)
        case NOT_FOUND => HipFailureResponse(NOT_FOUND.toString)
        
        case _ => parseResponse[HipGetTransactionResponse](res)
      }
    }
  }

}
