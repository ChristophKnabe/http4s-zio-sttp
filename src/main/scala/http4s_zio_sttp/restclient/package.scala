package http4s_zio_sttp

import sttp.client.Response
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.model.Uri
import zio.{Has, RIO, Task, ZIO}

//Annotations according to https://zio.dev/docs/howto/howto_use_layers
package object restclient {

  //1. Define an object that gives the name to the module, this can be (not necessarily) a package object
  object RestClient {
    //2. Within the module object define a trait Service that defines the interface our module is exposing,
    // in our case 2 methods to retrieve and create a user
    /** The interface of a REST client for managing resources of type A */
    trait Service[A] {
      def get(uri: Uri): ZIO[SttpClient, Throwable, A]
      def post(uri: Uri, a: A): ZIO[SttpClient, Throwable, Response[A]]
      def delete(id: Int): Task[Boolean]
    }
  }

  //4. Define a type alias like: type ModuleName = Has[Service]
  type RestClient[A] = Has[RestClient.Service[A]]

  def getUser(uri: Uri): RIO[SttpClient with RestClient[User], User] = RIO.accessM(_.get[SttpClient].get(uri))
  def createUser(a: User): RIO[RestClient[User], User] = RIO.accessM(_.get.create(a))
  def deleteUser(id: Int): RIO[RestClient[User], Boolean] = RIO.accessM(_.get.delete(id))
}
