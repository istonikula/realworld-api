package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
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
data class FavoriteArticleCommand(val slug: String, val user: User)
data class UnfavoriteArticleCommand(val slug: String, val user: User)
data class CommentArticleCommand(val slug: String, val comment: String, val user: User)
data class DeleteCommentCommand(val slug: String, val commentId: Long, val user: User)

sealed class ArticleUpdateError {
  object NotAuthor : ArticleUpdateError()
  object NotFound : ArticleUpdateError()
}

sealed class ArticleDeleteError {
  object NotAuthor : ArticleDeleteError()
  object NotFound : ArticleDeleteError()
}

sealed class ArticleFavoriteError {
  object Author : ArticleFavoriteError()
  object NotFound : ArticleFavoriteError()
}

sealed class ArticleUnfavoriteError {
  object NotFound : ArticleUnfavoriteError()
}

sealed class ArticleCommentError {
  object NotFound : ArticleCommentError()
}

sealed class ArticleCommentDeleteError {
  object ArticleNotFound : ArticleCommentDeleteError()
  object CommentNotFound : ArticleCommentDeleteError()
  object NotAuthor : ArticleCommentDeleteError()
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

  fun GetArticleCommand.runUseCase(): IO<Option<Article>> =
    getArticleBySlug(slug, user)
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
            if (it.author.username != cmd.user.username) ArticleDeleteError.NotAuthor.left()
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

interface FavoriteUseCase {
  val getArticleBySlug: GetArticleBySlug
  val addFavorite: AddFavorite

  fun FavoriteArticleCommand.runUseCase(): IO<Either<ArticleFavoriteError, Article>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getArticleBySlug(cmd.slug, cmd.user.some()).bind().fold(
          { ArticleFavoriteError.NotFound.left() },
          {
            when {
              it.author.username == cmd.user.username ->
                ArticleFavoriteError.Author.left()
              it.favorited ->
                it.right()
              else -> {
                addFavorite(it.id, cmd.user).bind()
                getArticleBySlug(cmd.slug, cmd.user.some()).bind().getOrSystemError(cmd.slug).right()
              }
            }
          }
        )
      }.fix()
    }
  }
}

interface UnfavoriteUseCase {
  val getArticleBySlug: GetArticleBySlug
  val removeFavorite: RemoveFavorite

  fun UnfavoriteArticleCommand.runUseCase(): IO<Either<ArticleUnfavoriteError, Article>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getArticleBySlug(cmd.slug, cmd.user.some()).bind().fold(
          { ArticleUnfavoriteError.NotFound.left() },
          {
            when {
              !it.favorited ->
                it.right()
              else -> {
                removeFavorite(it.id, cmd.user).bind()
                getArticleBySlug(cmd.slug, cmd.user.some()).bind().getOrSystemError(cmd.slug).right()
              }
            }
          }
        )
      }.fix()
    }
  }
}

interface CommentUseCase {
  val getArticleBySlug: GetArticleBySlug
  val addComment: AddComment

  fun CommentArticleCommand.runUsecase(): IO<Either<ArticleCommentError, Comment>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getArticleBySlug(cmd.slug, cmd.user.some()).bind().fold(
          { ArticleCommentError.NotFound.left() },
          { addComment(it.id, cmd.comment, cmd.user).bind().right() }
        )
      }.fix()
    }
  }
}

interface DeleteCommentUseCase {
  val getArticleBySlug: GetArticleBySlug
  val getComment: GetComment
  val deleteComment: DeleteComment

  fun DeleteCommentCommand.runUseCase(): IO<Either<ArticleCommentDeleteError, Int>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getArticleBySlug(cmd.slug, cmd.user.some()).bind().fold(
          { ArticleCommentDeleteError.ArticleNotFound.left() },
          {
            val comment = getComment(cmd.commentId, cmd.user).bind()
            comment.fold(
              { ArticleCommentDeleteError.CommentNotFound.left() },
              {
                if (it.author.username != cmd.user.username)
                  ArticleCommentDeleteError.NotAuthor.left()
                else
                  deleteComment(cmd.commentId).bind().right()
              }
            )
          }
        )
      }.fix()
    }
  }
}

private fun Option<Article>.getOrSystemError(slug: String) = this.getOrElse {
  throw RuntimeException("System error: article '$slug' should have been found")
}
