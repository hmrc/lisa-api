
import play.core.PlayVersion
import play.sbt.PlayImport.ws
import sbt._

object AppDependencies {

  val silencerVersion = "1.7.1"

  val compile: Seq[ModuleID] = Seq(
    ws,
    "uk.gov.hmrc"        %% "bootstrap-backend-play-28"  % "5.24.0",
    "uk.gov.hmrc"        %% "play-hmrc-api"              % "6.4.0-play-28",
    "com.typesafe.play"  %% "play-json-joda"             % "2.9.2",
    compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
    "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"           %%  "scalatest"          %  "3.2.9",
    "org.scalatestplus"       %%  "mockito-3-4"        %  "3.2.9.0",
    "org.scalatestplus.play"  %%  "scalatestplus-play" %  "5.1.0",
    "com.typesafe.play"       %%  "play-test"          %  PlayVersion.current,
    "org.scalaj"              %%  "scalaj-http"        %  "2.4.2",
    "org.pegdown"             %   "pegdown"            %  "1.6.0",
    "com.vladsch.flexmark"    %   "flexmark-all"       %  "0.36.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
