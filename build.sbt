import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}

scalaSettings
defaultSettings()

name := "lisa-api"
majorVersion := 2
scalaVersion := "2.13.16"
PlayKeys.playDefaultPort := 9667

lazy val lisaapi = project in file(".")

enablePlugins(PlayScala, SbtDistributablesPlugin)
disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

libraryDependencies ++= AppDependencies()
libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
retrieveManaged := true

Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
Test / fork := true

CodeCoverageSettings()
scalacOptions ++= Seq(
  "-Wconf:src=routes/.*:s",
  "-Wconf:cat=unused-imports&src=views/.*:s",
  "-Wconf:msg=match may not be exhaustive:s"
)
