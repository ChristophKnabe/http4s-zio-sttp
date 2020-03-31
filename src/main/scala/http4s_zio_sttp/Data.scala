package http4s_zio_sttp

final case class User(id: Int, name: String)

final case class UserNotFound(id: Int) extends Exception
