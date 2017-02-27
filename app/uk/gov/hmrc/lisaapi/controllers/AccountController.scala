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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.lisaapi.models._
import uk.gov.hmrc.lisaapi.services.AccountService
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AccountController extends LisaController {

  val service: AccountService = AccountService

  implicit val hc: HeaderCarrier = new HeaderCarrier()

  def createOrTransferLisaAccount(lisaManager: String): Action[AnyContent] = validateAccept(acceptHeaderValidationRules).async { implicit request =>
    Logger.debug(s"LISA HTTP Request: ${request.uri}  and method: ${request.method}" )
    withValidJson[CreateLisaAccountRequest] {
      req => {
        req match {
          case transferRequest: CreateLisaAccountTransferRequest => Future.successful(NotImplemented(Json.toJson(ErrorNotImplemented)))
          case createRequest: CreateLisaAccountCreationRequest => {
            service.createAccount(lisaManager, createRequest).map { result =>
              result match {
                case CreateLisaAccountSuccessResponse(accountId) => {
                  val data = ApiResponseData(message = "Account Created.", accountId = Some(accountId))

                  Created(Json.toJson(ApiResponse(data = Some(data), success = true, status = 201)))
                }
                case CreateLisaAccountInvestorNotFoundResponse => Forbidden(Json.toJson(ErrorInvestorNotFound))
                case CreateLisaAccountInvestorNotEligibleResponse => Forbidden(Json.toJson(ErrorInvestorNotEligible))
                case CreateLisaAccountInvestorComplianceCheckFailedResponse => Forbidden(Json.toJson(ErrorInvestorComplianceCheckFailed))
                case CreateLisaAccountInvestorPreviousAccountDoesNotExistResponse => Forbidden(Json.toJson(ErrorPreviousAccountDoesNotExist))
                case CreateLisaAccountAlreadyExistsResponse => Forbidden(Json.toJson(ErrorAccountAlreadyExists))
                case CreateLisaAccountErrorResponse => InternalServerError(Json.toJson(ErrorInternalServerError))
              }
            }
          }
        }
      }
    }
  }

}
