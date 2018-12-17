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

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.4.0",
    "uk.gov.hmrc" %% "auth-client" % "2.4.0",
    "uk.gov.hmrc" %% "domain" % "5.0.0",
    "uk.gov.hmrc" %% "play-hmrc-api" % "3.3.0-play-25",
    "com.codahale.metrics" % "metrics-graphite" % "3.0.2"
  )

  val test = Seq(
    "org.scalatest" %% "scalatest" % "2.2.6",
    "org.pegdown" % "pegdown" % "1.6.0",
    "com.typesafe.play" %% "play-test" % PlayVersion.current,
    "org.mockito" % "mockito-core" % "1.9.0",
    "info.cukes" %% "cucumber-scala" % "1.2.4",
    "info.cukes" % "cucumber-junit" % "1.2.4",
    "org.scalaj" %% "scalaj-http" % "1.1.5",
    "com.github.tomakehurst" % "wiremock" % "1.57" ,
    "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"
  ).map(_ % Test)

  def apply() = compile ++ test
}
