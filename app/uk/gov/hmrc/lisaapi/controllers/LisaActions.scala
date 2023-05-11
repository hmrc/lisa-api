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

package uk.gov.hmrc.lisaapi.controllers

import play.api.mvc.Results._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

trait LisaActions {

  def validateLMRN(lisaManager: String)(implicit ec: ExecutionContext): ActionRefiner[Request, LMRNRequest] =
    new ActionRefiner[Request, LMRNRequest] {
      override def executionContext: ExecutionContext       = ec
      override protected def refine[A](request: Request[A]) = Future.successful {
        if (lisaManager.matches("^Z([0-9]{4}|[0-9]{6})$")) {
          Right(LMRNRequest(request, lisaManager))
        } else {
          Left(BadRequest(ErrorBadRequestLmrn.asJson))
        }
      }
    }

  def validateAccountId(
    accountId: String
  )(implicit ec: ExecutionContext): ActionRefiner[LMRNRequest, LMRNWithAccountRequest] =
    new ActionRefiner[LMRNRequest, LMRNWithAccountRequest] {
      override def executionContext: ExecutionContext           = ec
      override protected def refine[A](request: LMRNRequest[A]) = Future.successful {
        if (accountId.matches("^[a-zA-Z0-9 :/-]{1,20}$")) {
          Right(LMRNWithAccountRequest(request.request, request.lmrn, accountId))
        } else {
          Left(BadRequest(ErrorBadRequestAccountId.asJson))
        }
      }
    }

}

case class LMRNRequest[A](request: Request[A], lmrn: String) extends WrappedRequest[A](request)

case class LMRNWithAccountRequest[A](request: Request[A], lmrn: String, accountId: String)
    extends WrappedRequest[A](request)
