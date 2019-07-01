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

package unit.utils


  import org.scalatest.{MustMatchers, WordSpec}
  import play.api.test.{HasApp, Injecting}
  import scala.io.Source

  trait BaseSpec extends WordSpec with MustMatchers with Injecting {
    this: HasApp =>
    def getJsonFile(path: String) = {
      val resource = Source.fromFile(s"./test/testJson/$path")
      try {
        resource.getLines().mkString
      } finally {
        resource.close()
      }
    }
  //  class WrapRequestAction(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext) extends ActionTransformer[Request, RequestWithHeaderCarrier] with ActionBuilder[RequestWithHeaderCarrier, AnyContent] {
  //    override def transform[A](request: Request[A]): Future[RequestWithHeaderCarrier[A]] = {
  //      Future.successful(RequestWithHeaderCarrier(request, HeaderCarrier()))
  //    }
  //  }
  }