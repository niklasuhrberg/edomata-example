package dev.hnaderi.example

import cats.effect.{Async, IO, Sync}
import cats.syntax.all.*
import dev.hnaderi.example.metadata.MetadataApp
import edomata.backend.eventsourcing.AggregateState
import edomata.core.CommandMessage
import io.circe.generic.auto.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl

import java.time.Instant
import java.util.UUID

object MetadataRoutes:

  def processError[F[_] : Async](arg: metadata.Rejection) =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    arg match {
      case metadata.Rejection.IllegalState => PreconditionFailed("Metadata is not in a compatible state")
      case _ => InternalServerError("An error occurred")
    }

  def metadataRoutes[F[_] : Async](M: MetadataApp[F[_]]): HttpRoutes[F] =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "metadata" / id =>
        for {
          md <- M.storage.repository.get(id).map(m => m match {
            case AggregateState.Valid(state, version) =>
              state
            case AggregateState.Conflicted(last, _ , _) => last
          })
          response <- Ok(md)
        } yield response

      case arg@POST -> Root / "metadata" =>
        for {
          mc <- arg.as[MetadataCreation]
          creationResult <- M.service(CommandMessage(UUID.randomUUID().toString, Instant.now(),
            mc.metadataId.toString, metadata.Command.Create(mc.entityId, mc.parent, mc.category)))
          response <- creationResult.fold(
            rejection => processError[F](rejection.head),
            un => Ok("Metadata created"))
        } yield response
      case arg@POST -> Root / "metadata" / id / "items" =>
        for {
          mc <- arg.as[MetadataItemCreation]
          creationResult <- M.service(CommandMessage(UUID.randomUUID().toString, Instant.now(),
            id, metadata.Command.AddItem(DataConversion.itemCreationToItem(mc))))
          response <- creationResult.fold(rejection => processError[F](rejection.head), un => Ok("Metadata item created"))
        } yield (response)
    }


