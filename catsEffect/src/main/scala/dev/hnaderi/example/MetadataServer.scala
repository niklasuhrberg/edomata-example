package dev.hnaderi.example

import cats.Applicative
import cats.effect.std.Console
import cats.effect.{Async, Resource}
import cats.syntax.all.*
import com.comcast.ip4s.*
import dev.hnaderi.example.read.MessageRetrievalOps
import edomata.backend.OutboxConsumer
import fs2.Stream
import fs2.io.net.Network
import natchez.Trace.Implicits.noop
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.middleware.Logger
import skunk.Session
object MetadataServer:

  def run[F[_] : Applicative: Async : Network : Console]: F[Nothing] = {
    for {
      client <- EmberClientBuilder.default[F].build
      metadataApp <- Application[F]()
      pool <- Session.pooled[F](
        host = "localhost",
        user = "postgres",
        password = Some("postgres"),
        database = "metadata_read",
        max = 10
      )
      retrievalOps <- Resource.make[F, MessageRetrievalOps[F]](MessageRetrievalOps.make[F](pool))(a => Applicative[F].pure(()))
      // Combine Service Routes into an HttpApp.
      // Can also be done via a Router if you
      // want to extract segments not checked
      // in the underlying routes.
      httpApp = (
          MetadataRoutes.metadataRoutes[F](metadataApp.metadataApp, retrievalOps ) //, 
        ).orNotFound

      // With Middlewares in place
      finalHttpApp = Logger.httpApp(true, true)(httpApp)

      _ <-
        EmberServerBuilder.default[F]
          .withHost(ipv4"0.0.0.0")
          .withPort(port"8080")
          .withHttpApp(finalHttpApp)
          .build
    } yield ()
  }.useForever
