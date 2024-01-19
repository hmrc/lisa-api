import sbt.*

object AppDependencies {

  val bootstrapVersion = "7.23.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-hmrc-api"             % "7.2.0-play-28"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-28" % bootstrapVersion,
    "org.scalatest"          %% "scalatest"              % "3.2.17",
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "5.1.0",
    "org.scalaj"             %% "scalaj-http"            % "2.4.2",
    "org.pegdown"             % "pegdown"                % "1.6.0",
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
