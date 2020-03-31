package http4s_zio_sttp

import sttp.client.Response
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.model.Uri
import zio.{Has, RIO, Task, ZIO}

package object restclient {

  object RestClient {
    /** The interface of a REST client for managing resources of type A */
    trait Service[A] {
      def get(uri: Uri): ZIO[SttpClient, Throwable, Response[A]]
      def post(uri: Uri, a: A): ZIO[SttpClient, Throwable, Response[A]]
      def delete(id: Int): Task[Boolean]
    }
  }

  type RestClient[A] = Has[RestClient.Service[A]]

  def getUser(uri: Uri): RIO[RestClient[User], User] = RIO.accessM(_.get.get(uri))
  def createUser(a: User): RIO[RestClient[User], User] = RIO.accessM(_.get.create(a))
  def deleteUser(id: Int): RIO[RestClient, Boolean] = RIO.accessM(_.get.delete(id))
}
