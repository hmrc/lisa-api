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
 * distributed under the License is distributed on an "AS IS" BASIS,xc
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

scalaVersion := "2.12.11"

lazy val lisaapi = (project in file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    name := "lisa-api",
    majorVersion := 2,
    PlayKeys.playDefaultPort := 9667,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    evictionWarningOptions in update := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    unmanagedResourceDirectories in Compile += baseDirectory.value / "resources",
    fork in Test := true,
    resolvers ++= Seq(
      Resolver.bintrayRepo("hmrc", "releases"),
      Resolver.jcenterRepo
    ))

lazy val scoverageSettings = {
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;uk.gov.hmrc.lisaapi.views.txt;uk.gov.hmrc;definition;uk.gov.hmrc.lisaapi.config.*;uk.gov.hmrc.lisaapi.models.*;uk.gov.hmrc.lisaapi.metrics.*;prod",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    parallelExecution in Test := false
  )
}

scalacOptions ++= Seq(
  "-P:silencer:pathFilters=views;routes"
)
