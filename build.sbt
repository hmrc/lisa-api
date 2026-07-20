ThisBuild / majorVersion := 2
ThisBuild / scalaVersion := "3.3.8"

lazy val microservice = Project("lisa-api", file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:src=routes/.*:s",
      "-Wconf:msg=unused import&src=views/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    PlayKeys.playDefaultPort := 9667
  )
  .settings(CodeCoverageSettings())

addCommandAlias("scalafmtAll", "all scalafmtSbt scalafmt Test/scalafmt")
