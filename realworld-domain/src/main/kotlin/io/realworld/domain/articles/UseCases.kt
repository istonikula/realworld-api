package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.core.some
import arrow.data.EitherT
import arrow.data.value
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.effects.monad
import arrow.instances.ForEitherT
import arrow.typeclasses.binding
import io.realworld.domain.users.User
import java.util.UUID

data class CreateArticleCommand(val data: ArticleCreation, val user: User)
data class DeleteArticleCommand(val slug: String, val user: User)
data class GetArticleCommand(val slug: String, val user: Option<User>)
data class UpdateArticleCommand(val data: ArticleUpdate, val slug: String, val user: User)

sealed class ArticleUpdateError {
  object NotFound : ArticleUpdateError()
  object NotOwner : ArticleUpdateError()
}

sealed class ArticleDeleteError {
  object NotFound : ArticleDeleteError()
  object NotOwner : ArticleDeleteError()
}

interface CreateArticleUseCase {
  val createUniqueSlug: CreateUniqueSlug
  val createArticle: CreateArticle

  fun CreateArticleCommand.runUseCase(): IO<Article> {
    val cmd = this
    return ForIO extensions {
      binding {
        val slug = createUniqueSlug(cmd.data.title).bind()
        createArticle(
          ValidArticleCreation(
            id = UUID.randomUUID(),
            slug = slug,
            title = cmd.data.title,
            description = cmd.data.description,
            body = cmd.data.body,
            tagList = cmd.data.tagList
          ),
          cmd.user
        ).bind()
      }.fix()
    }
  }
}

interface GetArticleUseCase {
  val getArticleBySlug: GetArticleBySlug

  fun GetArticleCommand.runUseCase(): IO<Option<Article>> {
    return getArticleBySlug(slug, user)
  }
}

interface DeleteArticleUseCase {
  val getArticleBySlug: GetArticleBySlug
  val deleteArticle: DeleteArticle

  fun DeleteArticleCommand.runUseCase(): IO<Either<ArticleDeleteError, Int>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getArticleBySlug(cmd.slug, user.some()).bind().fold(
          { ArticleDeleteError.NotFound.left() },
          {
            if (it.author.username != cmd.user.username) ArticleDeleteError.NotOwner.left()
            else deleteArticle(it.id).bind().right()
          }
        )
      }.fix()
    }
  }
}

interface UpdateArticleUseCase {
  val validateUpdate: ValidateArticleUpdate
  val updateArticle: UpdateArticle

  fun UpdateArticleCommand.runUseCase(): IO<Either<ArticleUpdateError, Article>> {
    val cmd = this
    return ForEitherT<ForIO, ArticleUpdateError>(IO.monad()) extensions {
      binding {
        val validUpdate = EitherT(validateUpdate(cmd.data, cmd.slug, cmd.user)).bind()
        EitherT(updateArticle(validUpdate, cmd.user).map { it.right() }).bind()
      }.value().fix()
    }
  }
}
