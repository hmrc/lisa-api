import sbt._
import uk.gov.hmrc.SbtAutoBuildPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.versioning.SbtGitVersioning

object MicroServiceBuild extends Build with MicroService {

  val appName = "lisa-api"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {
  import play.sbt.PlayImport._
  import play.core.PlayVersion

  private val microserviceBootstrapVersion = "5.14.0"
  private val playHealthVersion = "2.0.0"
  private val logbackJsonLoggerVersion = "3.1.0"
  private val playUrlBindersVersion = "2.0.0"
  private val playConfigVersion = "3.0.0"
  private val domainVersion = "4.0.0"
  private val hmrcTestVersion = "2.2.0"
  private val scalaTestVersion = "2.2.6"
  private val pegdownVersion = "1.6.0"
  private val apiPlatformlibVersion = "1.3.0"
  private val metricsGraphiteVersion = "3.0.2"
  private val playGraphiteVersion = "3.1.0"


  val compile = Seq(

    ws,
    "uk.gov.hmrc" %% "microservice-bootstrap" % microserviceBootstrapVersion,
    "uk.gov.hmrc" %% "play-auth" % "1.0.0",
    "uk.gov.hmrc" %% "play-health" % playHealthVersion,
    "uk.gov.hmrc" %% "play-url-binders" % playUrlBindersVersion,
    "uk.gov.hmrc" %% "play-config" % playConfigVersion,
    "uk.gov.hmrc" %% "logback-json-logger" % logbackJsonLoggerVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "uk.gov.hmrc" %% "play-hmrc-api" % apiPlatformlibVersion,
    "com.codahale.metrics" % "metrics-graphite" % metricsGraphiteVersion,
    "uk.gov.hmrc" %% "play-graphite" % playGraphiteVersion
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test : Seq[ModuleID] = ???
  }

  object Test {
    def apply() = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "1.9.0" % scope,
        "info.cukes" %% "cucumber-scala" % "1.2.4" % scope,
        "info.cukes" % "cucumber-junit" % "1.2.4" % scope,
        "org.scalaj" %% "scalaj-http" % "1.1.5" % scope,
        "com.github.tomakehurst" % "wiremock" % "1.57"  % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"  % scope
      )
    }.test
  }

  object IntegrationTest {
    def apply() = new TestDependencies {

      override lazy val scope: String = "it"


      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "org.mockito" % "mockito-core" % "1.9.0" % scope,
        "org.scalaj" %% "scalaj-http" % "1.1.5" % scope,
        "com.github.tomakehurst" % "wiremock" % "1.57" % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1"  % scope,
        "info.cukes" %% "cucumber-scala" % "1.2.4" % scope,
        "info.cukes" % "cucumber-junit" % "1.2.4" % scope
      )
    }.test
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
