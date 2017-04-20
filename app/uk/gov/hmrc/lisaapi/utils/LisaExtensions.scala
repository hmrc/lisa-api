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

package uk.gov.hmrc.lisaapi.utils

import org.joda.time.DateTime

object LisaExtensions {

  implicit class RichProduct(val cc: Product) extends AnyVal {
    def toStringMap = flattenMap(getMap(cc))
  }

  private def getMap(p: Product): Map[String, Any] = {
    p.getClass.getDeclaredFields
      .map(_.getName) // all field names
      .zip(p.productIterator.toList.map(
      {
        case Some(y: Product) => getMap(y);
        case Some(v) => v
        case None => None
        case x: Product => getMap(x)
        case v => v
      }))
      .toMap
  }

  private def flattenMap(mp: Map[String, Any]): Map[String, String] = {
    def helper(keys: List[String], acc: Map[String, String], mp: Map[String, Any]): Map[String, String] = {
      keys match {
        case Nil => acc
        case x :: xs => mp.get(x) match {
          case Some(None) => helper(xs, acc, mp)
          case Some(y: Map[String, Any]) => helper(xs, acc ++ helper(y.keys.toList, Map(), y), mp)
          case Some(v) => helper(xs, acc + (x -> convertToString(v)), mp)
        }
      }
    }

    helper(mp.keys.toList, Map(), mp)
  }

  private def convertToString(value: Any): String = {
    value match {
      case x: String => x
      case x: DateTime => x.toString("yyyy-MM-dd")
      case x: Float => x.toString
      case x: Int => x.toString
      case _ => value.toString
    }
  }
}
