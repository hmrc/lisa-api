import sbt.*

object AppDependencies {

  val bootstrapVersion = "8.5.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "uk.gov.hmrc"       %% "play-hmrc-api-play-30"     % "8.0.0",
    "io.dropwizard.metrics" % "metrics-graphite" % "3.2.5",
    "io.dropwizard.metrics" % "metrics-core" % "4.2.25"
  )

  val test: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"            %% "bootstrap-test-play-30" % bootstrapVersion,
    "org.scalatest"          %% "scalatest"              % "3.2.18",
    "org.scalatestplus"      %% "mockito-3-4"            % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.1",
    "org.scalaj"             %% "scalaj-http"            % "2.4.2",
    "org.pegdown"             % "pegdown"                % "1.6.0",
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.64.8"
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
