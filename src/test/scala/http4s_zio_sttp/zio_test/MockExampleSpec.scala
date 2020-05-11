package http4s_zio_sttp.zio_test


import zio.test.Assertion._
import zio.test.mock.Expectation.{ value, valueF }
import zio.test.mock.{ MockClock, MockConsole, MockRandom }
import zio.test.{ assertM, suite, testM, DefaultRunnableSpec }
import zio.{ clock, console, random }

// Taken from https://github.com/zio/zio/blob/master/examples/shared/src/test/scala/zio/examples/test/MockExampleSpec.scala
// but corrected to work with ZIO 1.0 RC 18-2
// This will fail some test cases, as it wants to show the usage of ZIO Test.
object MockExampleSpec extends DefaultRunnableSpec {

  def spec = suite("suite with mocks")(
    testM("expect call returning output") {
      val env = MockClock.NanoTime.returns(value(1000L))
      val app = clock.nanoTime
      val out = app.provideLayer(env)
      assertM(out)(equalTo(1000L))
    },
    testM("expect call with input satisfying assertion") {
      val app = console.putStrLn("foo")
      val env = MockConsole.PutStrLn(equalTo("foo")).returns(value())
      val out = app.provideLayer(env)
      assertM(out)(isUnit)
    },
    testM("expect call with input satisfying assertion and transforming it into output") {
      val app = random.nextInt(1)
      val env = MockRandom.NextInt._0(equalTo(1)).returns(valueF(_ + 41))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("expect call with input satisfying assertion and returning output") {
      val app = random.nextInt(1)
      val env = MockRandom.NextInt._0(equalTo(1)).returns(value(42))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("expect call for overloaded method") {
      val app = random.nextInt
      val env = MockRandom.NextInt._1.returns(value(42))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("expect calls from multiple modules") {
      val app =
        for {
          n <- random.nextInt
          _ <- console.putStrLn(n.toString)
        } yield ()

      val env = MockRandom.NextInt._1.returns(value(42)) andThen
        MockConsole.PutStrLn(equalTo("42")).returns(value())
      val out = app.provideLayer(env)
      assertM(out)(isUnit)
    },
    testM("expect repeated calls") {
      val app = random.nextInt *> random.nextInt
      val env = MockRandom.NextInt._1.returns((value(42))).repeats(1 to 3)
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("expect all of calls, in sequential order") {
      val app = random.nextInt *> random.nextLong
      val env = MockRandom.NextInt._1.returns(value(42)) andThen MockRandom.NextLong._0.returns(value(42L))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42L))
    },
    testM("expect all of calls, in any order") {
      val app = random.nextLong *> random.nextInt
      val env = MockRandom.NextInt._1.returns(value(42)) and MockRandom.NextLong._0.returns(value(42L))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("expect one of calls") {
      val app = random.nextLong
      val env = MockRandom.NextInt._1.returns(value(42)) or MockRandom.NextLong._0.returns(value(42L))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42L))
    },
    testM("failure if invalid method") {
      val app = random.nextInt
      val env = MockRandom.NextLong._0.returns(value(42L))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("failure if invalid arguments") {
      val app = console.putStrLn("foo")
      val env = MockConsole.PutStrLn(equalTo("bar")).returns(value())
      val out = app.provideLayer(env)
      assertM(out)(isUnit)
    },
    testM("failure if unmet expectations") {
      val app = random.nextInt
      val env = MockRandom.NextInt._1.returns(value(42)) andThen MockRandom.NextInt._1.returns(value(42))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42))
    },
    testM("failure if unexpected calls") {
      val app = random.nextInt *> random.nextLong
      val env = MockRandom.NextInt._1.returns(value(42))
      val out = app.provideLayer(env)
      assertM(out)(equalTo(42L))
    }
  )
}