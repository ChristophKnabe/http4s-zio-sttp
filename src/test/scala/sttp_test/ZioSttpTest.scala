package sttp_test

import zio.ZIO
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion.{equalTo, isGreaterThan, isGreaterThanEqualTo}
import zio.test._
import zio.test.environment.TestClock
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, configureFor, get, getRequestedFor, stubFor, urlEqualTo, verify}
import http4s_zio_sttp.User
import http4s_zio_sttp.restclient.RestClient
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.Assert.assertEquals
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.model.Uri

object ZioSttpTest extends DefaultRunnableSpec {

  val squareNumber = 144

  val mockServerZIO = ZIO.effect{
    new WireMockServer(8080) //no HTTPS
  }

  def spec = suite("ZIO STTP Test")(
    test(s"sqrt($squareNumber) (pure)") {
      assert(math.sqrt(squareNumber))(equalTo(12.0))
    },
    testM(s"sqrt($squareNumber) (effect)") {
      assertM(ZIO.succeed(math.sqrt(squareNumber)))(equalTo(12.0))
    },
    testM("live Clock.nanoTime greater zero (effect)") {
      import zio.clock.nanoTime
      assertM(nanoTime)(isGreaterThan(0L))
        .provideSomeLayer[Clock](Clock.live)
    },
    testM("TestClock nanoTime starts with zero (effect)") {
      import zio.clock.nanoTime
      for{
        before <- nanoTime
      } yield assert(before)(equalTo(0L))
    },
    //Following Example 1 of https://github.com/zio/zio/blob/master/docs/howto/test_effects.md#examples
    testM("TestClock adjust changes the test time (effect)") {
      import zio.clock.nanoTime
      val nanos = 100000L
      val duration = Duration.fromNanos(nanos)
      for{
        before <- nanoTime
        _ <- TestClock.adjust(duration)
        _ <- ZIO.sleep(duration)
        after <- nanoTime
      } yield assert(after)(isGreaterThanEqualTo(before+nanos))
    },
    testM(s"RestClient[User] by WireMock (effect)") {
      for{
        mock <- mockServerZIO
        _ <- ZIO.effect {
          mock.start()
          mock.stubFor(get(urlEqualTo("/users/1")).willReturn(aResponse.withBody(User(1, "Christoph").toString))) //TODO convert User to JSON
        }
        uri <- ZIO.fromEither(Uri.parse("http://localhost:8080/users/1"))
        user <- ZIO.accessM[RestClient[User]](_.get.get(uri))
        _ <- ZIO.effect{
          mock.verify(getRequestedFor(urlEqualTo("/users/1")))
          mock.stop()
        }
      } yield assert(user)(equalTo(User(1, "Christoph")))
    }.provideLayer(AsyncHttpClientZioBackend.layer() >>> RestClient.userLive)
  )
}
