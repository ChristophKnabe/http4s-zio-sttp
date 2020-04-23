package http4s_zio_sttp

import sttp.client.{Identity, IsOption, Request, RequestT, Response, ResponseError, SttpBackend, basicRequest}
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe._
import sttp.model.Uri
// For io.circe.Encoder and Decoder:
import io.circe._
import io.circe.generic.auto._
import sttp.client.asynchttpclient.WebSocketHandler
import zio._

//Numbered step annotations according to https://zio.dev/docs/howto/howto_use_layers
package object restclient {

  //4. Define a type alias like: type ModuleName = Has[Service]
  /* @tparam A The type of resources accessed by this REST client */
  type RestClient[A] = Has[RestClient.Service[A]]

  //1. Define an object that gives the name to the module, this can be (not necessarily) a package object.
  object RestClient {

    //2. Within the module object define a trait Service that defines the interface our module is exposing,
    // in our case methods to manage resources by the typical HTTP operations as GET, PUT, POST, DELETE.
    // Each function does not need an environment, but the layer as a whole needs one.
    /** The interface of a REST client for managing resources of type A */
    trait Service[A] {

      /** GETs a representation of the resource of type `A` from the given `uri`. */
      def get(uri: Uri): ZIO[Any, Throwable, A]

      /** POSTs a representation of the resource of type `A` to the given `uri`.
       *
       * @return a representation of the resource of type `A` as created on the server */
      def post(uri: Uri, a: A): ZIO[Any, Throwable, A]

      def delete(id: Int): Task[Boolean]
    }

    /** Creates a REST client `ZLayer` for management of resources of type `A`.
     * The REST client depends on `SttpClient`.
     * But `SttpClient` does not follow the `ZLayer` convention, as it is defined as Has[SttpBackend[Task, Nothing, WebSocketHandler]]`
     * instead of the conventional `Has[SttpClient.Service]`.
     * That is why we have to use different type parameters in the result type's RIn, and in the fromService's A. */
    def make[A: Encoder: Decoder : IsOption : Tagged]: ZLayer[SttpClient, Nothing, RestClient[A]]
    = ZLayer.fromService[SttpBackend[Task, Nothing, WebSocketHandler], RestClient.Service[A]] {
      backend =>
        new RestClient.Service[A] {

          //TODO Discuss if moving the base URI of the RestClient to the method RestClient.make is better.
          //It would be less redundant. But for that we would need a key parameter for each method.
          override def get(uri: Uri): ZIO[Any, Throwable, A] = {
            val request: Request[Either[ResponseError[Error], A], Nothing] = basicRequest
              .get(uri)
              .response(asJson[A])
            val result: Task[Response[Either[ResponseError[Error], A]]] = backend.send(request)
            moveResponseError(result)
          }

          override def post(uri: Uri, a: A): ZIO[Any, Throwable, A] = {
            val request: Request[Either[ResponseError[Error], A], Nothing] = basicRequest
              .body(a)
              .post(uri)
              .response(asJson[A])
            val result: Task[Response[Either[ResponseError[Error], A]]] = backend.send(request)
            moveResponseError(result)
          }

          override def delete(id: Int): zio.Task[Boolean] = ???

          /** Moves the `ResponseError` from the ZIO success channel into the error channel. */
          private def moveResponseError(task: Task[Response[Either[ResponseError[Error], A]]]): Task[A] = {
            task.flatMap { response =>
              response.body match {
                case Right(a) => ZIO.succeed(a)
                case Left(error) => ZIO.fail(error)
              }
            }
          }
        }

    }

    val userLive = make[User]

    def getUser(uri: Uri): RIO[RestClient[User], User] = RIO.accessM(_.get.get(uri))

    def createUser(uri: Uri, a: User): RIO[RestClient[User], User] = RIO.accessM(_.get.post(uri, a))

    def deleteUser(id: Int): RIO[RestClient[User], Boolean] = RIO.accessM(_.get.delete(id))

  } //object RestClient
}
