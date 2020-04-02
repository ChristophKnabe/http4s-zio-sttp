package http4s_zio_sttp.restclient

import http4s_zio_sttp.{User, UserNotFound}
import doobie.Transactor
import io.circe
import sttp.model._
import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio._
import io.circe.generic.auto._
import zio._

/** General REST client module for production using STTP */
final class RestClientService(tnx: Transactor[Task]) extends RestClient.Service[User] {

  override def get(uri: Uri) /*: ZIO[SttpClient, Throwable, Response[User]]*/ = {
    val request = basicRequest
      .get(uri)
      .response(asJson[User]) // needs import io.circe.generic.auto._
    val zio: ZIO[SttpClient, Throwable, Response[Either[ResponseError[circe.Error], User]]] = SttpClient.send(request)
    zio.flatMap{ response =>
      response.body match {
        case Right(user) => ZIO.succeed(user)
        case Left(error) => ZIO.fail(error)
      }
    }
  }

  override def create(user: User): Task[User] =
    SQL
      .create(user)
      .run
      .transact(tnx)
      .foldM(err => Task.fail(err), _ => Task.succeed(user))

  override def delete(id: Int): Task[Boolean] =
    SQL
      .delete(id)
      .run
      .transact(tnx)
      .fold(_ => false, _ => true)
}


