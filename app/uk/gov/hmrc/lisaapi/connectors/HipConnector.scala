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
import jdk.internal.net.http.common.Log.headers
import play.api.Logging
import play.api.http.Status
import play.api.http.Status.{BAD_REQUEST, CREATED, FORBIDDEN, INTERNAL_SERVER_ERROR, NOT_FOUND, OK, SERVICE_UNAVAILABLE, UNAUTHORIZED, UNPROCESSABLE_ENTITY}
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.{JsError, JsSuccess, JsValue, Json, Reads}
import play.mvc.Http.{HeaderNames, MimeTypes}
import play.utils.UriEncoding
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.HttpReads.Implicits.readRaw
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.lisaapi.config.AppContext
import uk.gov.hmrc.lisaapi.models.{GetTransactionResponse, LisaManagerReferenceNumber}
import uk.gov.hmrc.lisaapi.models.des.{DesFailureResponse, DesResponse}
import uk.gov.hmrc.lisaapi.models.hip.{HipBadRequest, HipFailureResponse, HipFailures, HipForbidden, HipGetTransactionResponse, HipNotFound, HipOriginUnknown, HipOtherErrorResponse, HipResponse, HipServerError, HipServiceUnavailable, HipUnauthorized, HipValidationError, HodError, HodErrorResponse}

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.UUID.randomUUID
import scala.:+
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

  def parseResponse[A <: HipResponse](res: HttpResponse, originCheck: Boolean = false)(implicit reads: Reads[A]): HipResponse = {

    def validateContentType: Either[HipResponse, Unit] = {
      if (hasJsonContent(res)) Right(())
      else {
        logger.error(s"[HipConnector][parseResponse] Non-JSON response, status: ${res.status}")
        Left(HipOtherErrorResponse)
      }
    }


    def getAndValidateOrigin: Either[HipResponse, String] = {
      val origin = (res.json \ "origin").asOpt[String]
      origin match {
        case Some(value) if value == "HIP" || value == "HOD" => Right(value)
        case _ if res.status == 500 || res.status == 400 => Left(HipOriginUnknown)
        case _  => Right("HIP")
      }
    }

    def parseJson(origin: String): Either[HipOtherErrorResponse.type , HipResponse] = {
      
    val validation =   if(origin == "HOD") {
        res.json.validate[HodErrorResponse]
      } else {
        res.json.validate[A]
        
      }
      
      validation match {
        case JsSuccess(value, _) => Right(value: HipResponse)
        case JsError(errors) =>
          logger.error(s"[HipConnector][parseResponse] JSON parsing error: ${errors.mkString(", ")}")
          Left(HipOtherErrorResponse)
      }
      }
      

//      if(origin == "HOD") {
//        res.json.validate[A] match {
//          case JsSuccess(value, _) => Right(value)
//          case JsError(errors) =>
//            logger.error(s"[HipConnector][parseResponse] JSON parsing error: ${errors.mkString(", ")}")
//            Left(HipOtherErrorResponse)
//        }
//      } else {
//
//
//        res.json.validate[A] match {
//          case JsSuccess(value, _) => Right(value)
//          case JsError(errors) =>
//            logger.error(s"[HipConnector][parseResponse] JSON parsing error: ${errors.mkString(", ")}")
//            Left(HipOtherErrorResponse)
//        }
      //}
//    }




     def hasJsonContent(res: HttpResponse): Boolean = {
       res.headers
         .getOrElse(HeaderNames.CONTENT_TYPE, Seq.empty)
         .exists(_.toLowerCase.contains(MimeTypes.JSON.toLowerCase))
     }


    (for {
      _ <- validateContentType
      origin <-  getAndValidateOrigin

      value <- parseJson(origin)
    } yield value).getOrElse(HipOtherErrorResponse)
  }





//   def parseResponse[A <: HipResponse](res: HttpResponse, originCheck: Boolean = false)(implicit reads: Reads[A]): HipResponse = {
//
//
//     val isJson = res.headers
//      .getOrElse(HeaderNames.CONTENT_TYPE, Seq.empty[String])
//      .exists(_.toLowerCase.contains(MimeTypes.JSON.toLowerCase))
//
//     if (isJson) {
//       if (!(res.json \ "origin").asOpt[String].contains("HIP") && originCheck) {
//         return HipOriginUnknown
//       }
//      res.json.validate[A] match {
//        case JsSuccess(value, _) => value
//        case JsError(er)         =>
//            logger.error(
//              s"[HipConnector][parseJsonResponse] Error from HIP (parsing as HipResponse): ${er.mkString(", ")}"
//            )
//          HipOtherErrorResponse
//    }} else {
//      logger.error(
//        s"[HipConnector][parseJsonResponse] Error from HIP (parsing as HipFailureResponse): Received non-JSON content from HIP, status: ${res.status}"
//      )
//      HipOtherErrorResponse
//    }
// }

  
  
  
  
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
        case OK => parseResponse[HipGetTransactionResponse](res, false)
        case BAD_REQUEST => parseResponse[HipBadRequest](res, true)
        case SERVICE_UNAVAILABLE => parseResponse[HipServiceUnavailable](res, true)
        case INTERNAL_SERVER_ERROR => parseResponse[HipServerError](res, true)
        case UNPROCESSABLE_ENTITY => parseResponse[HipValidationError](res, false)
        case NOT_FOUND => HipNotFound
        case UNAUTHORIZED => HipUnauthorized
        case FORBIDDEN => HipForbidden
        case _ => HipOtherErrorResponse
      }
    }
  }

}
