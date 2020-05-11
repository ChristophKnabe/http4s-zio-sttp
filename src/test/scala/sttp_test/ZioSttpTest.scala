package sttp_test

import zio.ZIO
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion.{equalTo, isGreaterThan, isGreaterThanEqualTo}
import zio.test._
import zio.test.environment.TestClock
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, configureFor, get, getRequestedFor, stubFor, urlEqualTo, verify}
import http4s_zio_sttp.restclient.RestClient
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpGet
import org.junit.Assert.assertEquals
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend

object ZioSttpTest extends DefaultRunnableSpec {

  val squareNumber = 144

  val mockServerZIO = ZIO.effect{
    new WireMockServer()
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
    testM(s"WireMock (effect)") {
      for{
        mockServer <- mockServerZIO
        _ <- ZIO.effect {
          mockServer.start()
          configureFor("localhost", 8080)
          stubFor(get(urlEqualTo("/myResource")).willReturn(aResponse.withBody("Here is your resource.")))
        }
        x <- ZIO.accessM(userRestClient(_.get.getUser(5))
        //TODO weiter vom WireMock-Beispiel übernehmen:
        _ <- ZIO.effect{
          val request = new HttpGet("http://localhost:8080/baeldung")
          val httpResponse = httpClient.execute(request)
          val stringResponse = convertResponseToString(httpResponse)
          verify(getRequestedFor(urlEqualTo(BAELDUNG_PATH)))
          assertEquals("Welcome to Baeldung!", stringResponse)

          mockServer.stop()
        }
        _ <- ZIO.effect{(duration)}
        after <- nanoTime
      } yield assert(after)(isGreaterThanEqualTo(before+nanos))
    }.provideLayer((AsyncHttpClientZioBackend.layer() >>> RestClient.userLive))
  )
}
