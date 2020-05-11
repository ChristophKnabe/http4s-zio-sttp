package http4s_zio_sttp.zio_test

import zio.ZIO
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion.{equalTo, isGreaterThan, isGreaterThanEqualTo}
import zio.test._
import zio.test.environment.TestClock

object ZioPlatformTest extends DefaultRunnableSpec {

  val squareNumber = 144

  def spec = suite("ZIO Test")(
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
    }
  )
}
