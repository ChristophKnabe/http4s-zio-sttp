package http4s_zio_sttp.persistence

import http4s_zio_sttp.{User, UserNotFound}
import http4s_zio_sttp.configuration.DbConfig
import zio.blocking.Blocking
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.{Cause, ZLayer}

object UserPersistenceSpec extends DefaultRunnableSpec {
  val ec = concurrent.ExecutionContext.global

  val dbConfig = ZLayer.succeed(DbConfig("jdbc:h2:~/test", "", ""))

  def spec =
    suite("Persistence integration test")(testM("Persistense Live") {
      for {
        notFound <- getUser(100).either
        created <- createUser(User(14, "usr")).either
        deleted <- deleteUser(14).either
      } yield
        assert(notFound)(isLeft(anything)) &&
          assert(created)(isRight(equalTo(User(14, "usr")))) &&
          assert(deleted)(isRight(isTrue))
    }).provideSomeLayer[TestEnvironment](
      (dbConfig ++ Blocking.live) >>> UserPersistenceService
        .live(ec)
        .mapError(_ => TestFailure.Runtime(Cause.die(new Exception("die")))))

}
