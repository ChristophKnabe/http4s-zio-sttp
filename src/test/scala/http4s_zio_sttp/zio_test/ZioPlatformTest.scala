package http4s_zio_sttp.zio_test

import zio.ZIO
import zio.clock.Clock
import zio.duration.Duration
import zio.test.Assertion._
import zio.test._
import zio.test.environment.{TestClock, TestEnvironment}

object ZioPlatformTest extends DefaultRunnableSpec {

  def spec = suite("ZIO Test")(
    test("sqrt(144) (pure)") {
      assert(math.sqrt(144))(equalTo(12.0))
    },
    testM("sqrt(144) (effect))") {
      assertM(ZIO.succeed(math.sqrt(144)))(equalTo(12.0))
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
    testM("TestClock adjust changes the test time (effect)") {
      import zio.clock.nanoTime
      for{
        before <- nanoTime
        _ <- TestClock.adjust(Duration.fromNanos(100000L))
        after <- nanoTime
      } yield assert(after)(equalTo(before+100))
    },
    /*,
      testM("create a user then get it ") {
        for {
          created <- createUser(User(14, "usr"))
          user <- getUser(14)
        } yield
          assert(created)(equalTo(User(14, "usr"))) &&
            assert(user)(equalTo(User(14, "usr")))
      },
      testM("delete user") {
        for {
          deleted <- deleteUser(14).either
          notFound <- getUser(14).either
        } yield
          assert(deleted)(isRight(isTrue)) &&
            assert(notFound)(isLeft(anything))
      }*/
  )
}
