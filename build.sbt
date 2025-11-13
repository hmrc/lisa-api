ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "2.13.16"

lazy val microservice = Project("lisa-api", file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Wconf:cat=unused-imports&src=views/.*:s",
      "-Wconf:msg=match may not be exhaustive:s"
    ),
    libraryDependencies ++= AppDependencies(),
    PlayKeys.playDefaultPort := 9667
  )
  .settings(CodeCoverageSettings())
