package dev.hnaderi.example

import cats.FlatMap
import cats.data.*
import cats.effect.Async
import cats.syntax.all.*
import dev.hnaderi.example.metadata.{Metadata, MetadataApp, MetadataItem, Rejection}
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

  private def processError[F[_] : Async](arg: metadata.Rejection) =
    val dsl = new Http4sDsl[F] {}
    import dsl.*
    arg match {
      case metadata.Rejection.IllegalState => PreconditionFailed("Metadata is not in a compatible state")
      case _ => InternalServerError("An error occurred")
    }

  private def validateParent[F[_] : Async : FlatMap](mc: MetadataCreation, M: MetadataApp[F]): F[EitherNec[Rejection, Unit]] =
    mc.parent.fold(().rightNec.pure[F])(parentId =>
      M.storage.repository.get(parentId.toString).map {
        case AggregateState.Valid(Metadata.Initialized(_, _, _, _), version) =>
          ().rightNec
        case AggregateState.Valid(Metadata.New, version) =>
          Rejection.IllegalState.leftNec[Unit]
        case AggregateState.Conflicted(last, _, _) =>
          ().rightNec
      })

  def metadataRoutes[F[_] : Async : FlatMap](M: MetadataApp[F[_]]): HttpRoutes[F] =


    val dsl = new Http4sDsl[F] {}
    import dsl.*
    HttpRoutes.of[F] {
      case GET -> Root / "metadata" / id =>
        for {
          md <- M.storage.repository.get(id).map {
            case AggregateState.Valid(state, version) =>
              state
            case AggregateState.Conflicted(last, _, _) => last
          }
          response <- Ok(md)
        } yield response

      case arg@POST -> Root / "metadata" =>
        (for {
          mc <- EitherT[F, NonEmptyChain[Rejection], MetadataCreation](arg.as[MetadataCreation].map(m => m.asRight))
          validationResult <- EitherT[F, NonEmptyChain[Rejection], Unit](validateParent[F](mc, M))
          creationResult <- EitherT[F, NonEmptyChain[Rejection], Unit](
            M.service(CommandMessage(UUID.randomUUID().toString, Instant.now(),
              mc.metadataId.toString, metadata.Command.Create(mc.entityId, mc.parent, mc.category, "default user",
                mc.items.map(ic => DataConversion.itemCreationToItem(ic))))))
          response <- EitherT[F, NonEmptyChain[Rejection], Response[F]](Ok("Metadata created").map(_.asRight))
        } yield response).fold(r => InternalServerError("Try again"), a => a.pure[F]).flatten

      case arg@POST -> Root / "metadata" / id / "items" =>
        for {
          mc <- arg.as[MetadataItemCreation]
          creationResult <- M.service(CommandMessage(UUID.randomUUID().toString, Instant.now(),
            id, metadata.Command.AddItem(DataConversion.itemCreationToItem(mc), "default user")))
          response <- creationResult.fold(rejection => processError[F](rejection.head), un => Ok("Metadata item created"))
        } yield response
    }


