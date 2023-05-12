import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

scalaSettings
defaultSettings()

name := "lisa-api"
majorVersion := 2
scalaVersion := "2.13.10"
PlayKeys.playDefaultPort := 9667

lazy val lisaapi = project in file(".")

enablePlugins(PlayScala, SbtDistributablesPlugin)

libraryDependencies ++= AppDependencies()
libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
retrieveManaged := true

Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
Test / fork := true

ScoverageKeys.coverageExcludedPackages := "<empty>;testOnlyDoNotUseInAppConf.*;uk.gov.hmrc.lisaapi.views.txt;uk.gov.hmrc;definition;uk.gov.hmrc.lisaapi.config.*;uk.gov.hmrc.lisaapi.models.*;uk.gov.hmrc.lisaapi.metrics.*;prod"
ScoverageKeys.coverageMinimumStmtTotal := 89
ScoverageKeys.coverageFailOnMinimum := true
ScoverageKeys.coverageHighlighting := true

scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s",
  "-Wconf:cat=unused-imports&src=views/.*:s",
  "-Wconf:msg=match may not be exhaustive:s"
)

addCommandAlias("scalastyleAll", "all scalastyle test:scalastyle")
