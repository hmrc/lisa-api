package unit.utils


  import org.scalatest.{MustMatchers, WordSpec}
  import play.api.test.{HasApp, Injecting}
  import scala.io.Source

  trait BaseSpec extends WordSpec with MustMatchers with Injecting {
    this: HasApp =>
    def getJsonFile(path: String) = {
      val resource = Source.fromFile(s"./test/testJson/$path")
      try {
        resource.getLines().mkString
      } finally {
        resource.close()
      }
    }
  //  class WrapRequestAction(val parser: BodyParsers.Default)(implicit val executionContext: ExecutionContext) extends ActionTransformer[Request, RequestWithHeaderCarrier] with ActionBuilder[RequestWithHeaderCarrier, AnyContent] {
  //    override def transform[A](request: Request[A]): Future[RequestWithHeaderCarrier[A]] = {
  //      Future.successful(RequestWithHeaderCarrier(request, HeaderCarrier()))
  //    }
  //  }
  }