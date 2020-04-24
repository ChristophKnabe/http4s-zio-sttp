# ZIO with http4s and STTP

This is an example of calling a web service by STTP from an http4s web service.

ZIO is used as the glue effects library in order to make http4s and STTP collaborate.
As http4s and STTP that are using `cats-effect`, ZIO has a separate module called: `zio-interop-cats`
which contains instances for the Cats Effect library, 
and allows you to use ZIO with any libraries that rely on Cats Effect like in our case http4s and STTP.

**Attention: Not yet compilable!**

Checkout this blog post: [link](https://medium.com/@wiemzin/zio-with-http4s-and-doobie-952fba51d089). It explains in details how to use ZIO with http4s and doobie. 

Find the implementation in this package: `com.zio.examples.http4s_doobie`

Inspired by: Maxim Schuwalow in this project: [zio-todo-backend](https://github.com/mschuwalow/zio-todo-backend)

Credit: [@jdegoes](https://github.com/jdegoes) and [@mschuwalow](https://github.com/mschuwalow)

## Eat chocolate with ZIO

in package: [quickstart](https://github.com/wi101/zio-examples/tree/master/src/main/scala/com/zio/examples/quickstart)

This example is a quickstart on how could you repeat and retry actions using ZIO