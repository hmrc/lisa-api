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

import sbt._

object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport._

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-26" % "1.5.0",
    "uk.gov.hmrc" %% "auth-client" % "2.35.0-play-26",
    "uk.gov.hmrc" %% "play-hmrc-api" % "4.1.0-play-26",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % "3.0.8",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "org.mockito" % "mockito-core" % "3.3.0",
    "org.scalaj" %% "scalaj-http" % "2.4.2",
    "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.3"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
