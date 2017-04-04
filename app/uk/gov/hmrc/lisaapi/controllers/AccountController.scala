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

package uk.gov.hmrc.lisaapi.controllers

import play.api.Logger
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsObject, JsPath, JsValue, Json}
import play.api.libs.json.Json.toJson
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.config.LisaAuthConnector
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.AccountService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountController extends LisaController {
  val authConnector = LisaAuthConnector

  val service: AccountService = AccountService

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  def createOrTransferLisaAccount(lisaManager: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    withValidJson[CreateLisaAccountRequest] (
      (req) => {
        req match {
          case createRequest: CreateLisaAccountCreationRequest => {
            if (hasAccountTransferData(request.body.asJson.get.as[JsObject])) {
              Future.successful(Forbidden(toJson(ErrorTransferAccountDataProvided)))
            }
            else {
              processAccountCreation(lisaManager, createRequest)
            }
          }
          case transferRequest: CreateLisaAccountTransferRequest => processAccountTransfer(lisaManager, transferRequest)
        }
      },
      Some(
        (errors) => {
          Logger.info("The errors are " + errors.toString())

          val transferAccountDataNotProvided = errors.count {
            case (path: JsPath, errors: Seq[ValidationError]) => {
              path.toString().contains("/transferAccount") && errors.contains(ValidationError("error.path.missing"))
            }
          }

          if (transferAccountDataNotProvided > 0) {
            Future.successful(Forbidden(toJson(ErrorTransferAccountDataNotProvided)))
          }
          else {
            Future.successful(BadRequest(toJson(ErrorGenericBadRequest)))
          }
        }
      )
    )
  }

  def closeLisaAccount(lisaManager: String, accountId: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    withValidJson[CloseLisaAccountRequest] { request =>
      processAccountClosure(lisaManager, accountId, request)
    }
  }

  private def processAccountCreation(lisaManager: String, request: CreateLisaAccountCreationRequest) = {
    service.createAccount(lisaManager, request).map { result =>
      result match {
        case CreateLisaAccountSuccessResponse(accountId) => {
          val data = ApiResponseData(message = "Account Created.", accountId = Some(accountId))

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
        }
        case CreateLisaAccountInvestorNotFoundResponse => Forbidden(Json.toJson(ErrorInvestorNotFound))
        case CreateLisaAccountInvestorNotEligibleResponse => Forbidden(Json.toJson(ErrorInvestorNotEligible))
        case CreateLisaAccountInvestorComplianceCheckFailedResponse => Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailed))
        case CreateLisaAccountAlreadyExistsResponse => Conflict(Json.toJson(ErrorAccountAlreadyExists))
        case _ => InternalServerError(Json.toJson(ErrorInternalServerError))
      }
    }
  }

  private def processAccountTransfer(lisaManager: String, request: CreateLisaAccountTransferRequest) = {
    service.transferAccount(lisaManager, request).map { result =>
      result match {
        case CreateLisaAccountSuccessResponse(accountId) => {
          val data = ApiResponseData(message = "Account Transferred.", accountId = Some(accountId))

          Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
        }
        case CreateLisaAccountInvestorNotFoundResponse => Forbidden(Json.toJson(ErrorInvestorNotFound))
        case CreateLisaAccountInvestorComplianceCheckFailedResponse => Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailed))
        case CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse => Forbidden(Json.toJson(ErrorPreviousAccountDoesNotExist))
        case CreateLisaAccountAlreadyExistsResponse => Conflict(Json.toJson(ErrorAccountAlreadyExists))
        case _ => InternalServerError(Json.toJson(ErrorInternalServerError))
      }
    }
  }

  private def processAccountClosure(lisaManager: String, accountId: String, request: CloseLisaAccountRequest) = {
    service.closeAccount(lisaManager, accountId, request).map { result =>
      result match {
        case CloseLisaAccountSuccessResponse(accountId) => {
          val data = ApiResponseData(message = "LISA Account Closed", accountId = Some(accountId))

          Ok(Json.toJson(ApiResponse(data = Some(data), success = true, status = 200)))
        }
        case CloseLisaAccountAlreadyClosedResponse => Forbidden(Json.toJson(ErrorAccountAlreadyClosed))
        case CloseLisaAccountNotFoundResponse => NotFound(Json.toJson(ErrorAccountNotFound))
        case _ => InternalServerError(Json.toJson(ErrorInternalServerError))
      }
    }
  }

  private def hasAccountTransferData(js:JsObject): Boolean = {
    js.keys.contains("transferAccount")
  }

}
