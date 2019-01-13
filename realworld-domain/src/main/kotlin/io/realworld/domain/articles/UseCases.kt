package io.realworld.domain.articles

import arrow.Kind
import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import arrow.data.EitherT
import arrow.data.value
import arrow.effects.typeclasses.MonadDefer
import arrow.instances.monad
import arrow.typeclasses.binding
import io.realworld.domain.users.User
import java.util.UUID

data class CreateArticleCommand(val data: ArticleCreation, val user: User)
data class DeleteArticleCommand(val slug: String, val user: User)
data class GetArticleCommand(val slug: String, val user: Option<User>)
data class GetArticlesCommand(val filter: ArticleFilter, val user: Option<User>)
data class GetFeedsCommand(val filter: FeedFilter, val user: User)
data class UpdateArticleCommand(val data: ArticleUpdate, val slug: String, val user: User)
data class FavoriteArticleCommand(val slug: String, val user: User)
data class UnfavoriteArticleCommand(val slug: String, val user: User)
data class CommentArticleCommand(val slug: String, val comment: String, val user: User)
data class DeleteCommentCommand(val slug: String, val commentId: Long, val user: User)
data class GetCommentsCommand(val slug: String, val user: Option<User>)
object GetTagsCommand

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

interface CreateArticleUseCase<F> {
  val createUniqueSlug: CreateUniqueSlug<F>
  val createArticle: CreateArticle<F>

  fun CreateArticleCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Article> {
    val cmd = this
    return MD.binding {
      val slug = createUniqueSlug(cmd.data.title).bind()
      createArticle(
        ValidArticleCreation(
          id = UUID.randomUUID().articleId(),
          slug = slug,
          title = cmd.data.title,
          description = cmd.data.description,
          body = cmd.data.body,
          tagList = cmd.data.tagList
        ),
        cmd.user
      ).bind()
    }
  }
}

interface GetArticleUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>

  fun GetArticleCommand.runUseCase(): Kind<F, Option<Article>> =
    getArticleBySlug(slug, user)
}

interface DeleteArticleUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>
  val deleteArticle: DeleteArticle<F>

  fun DeleteArticleCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Either<ArticleDeleteError, Int>> {
    val cmd = this
    return MD.binding {
      getArticleBySlug(cmd.slug, user.some()).bind().fold(
        { ArticleDeleteError.NotFound.left() },
        {
          if (it.author.username != cmd.user.username) ArticleDeleteError.NotAuthor.left()
          else deleteArticle(it.id).bind().right()
        }
      )
    }
  }
}

interface GetArticlesUseCase<F> {
  val getArticles: GetArticles<F>
  val getArticlesCount: GetArticlesCount<F>

  fun GetArticlesCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Pair<List<Article>, Long>> {
    val cmd = this
    return MD.binding {
      val count = getArticlesCount(cmd.filter).bind()
      if (count == 0L)
        Pair(listOf(), 0L)
      else {
        val articles = getArticles(cmd.filter, cmd.user).bind()
        Pair(articles, count)
      }
    }
  }
}

interface GetFeedsUseCase<F> {
  val getFeeds: GetFeeds<F>
  val getFeedsCount: GetFeedsCount<F>

  fun GetFeedsCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Pair<List<Article>, Long>> {
    val cmd = this
    return MD.binding {
      val count = getFeedsCount(cmd.user).bind()
      if (count == 0L) Pair(listOf(), 0L)
      else {
        val feeds = getFeeds(cmd.filter, cmd.user).bind()
        Pair(feeds, count)
      }
    }
  }
}

interface UpdateArticleUseCase<F> {
  val validateUpdate: ValidateArticleUpdate<F>
  val updateArticle: UpdateArticle<F>

  fun UpdateArticleCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Either<ArticleUpdateError, Article>> {
    val cmd = this
    return EitherT.monad<F, ArticleUpdateError>(MD).binding {
      val validUpdate = EitherT(validateUpdate(cmd.data, cmd.slug, cmd.user)).bind()
      EitherT(
        MD.run { updateArticle(validUpdate, cmd.user).map { it.right() } }
      ).bind()
    }.value()
  }
}

interface FavoriteUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>
  val addFavorite: AddFavorite<F>

  fun FavoriteArticleCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Either<ArticleFavoriteError, Article>> {
    val cmd = this
    return MD.binding {
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
    }
  }
}

interface UnfavoriteUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>
  val removeFavorite: RemoveFavorite<F>

  fun UnfavoriteArticleCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Either<ArticleUnfavoriteError, Article>> {
    val cmd = this
    return MD.binding {
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
    }
  }
}

interface CommentUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>
  val addComment: AddComment<F>

  fun CommentArticleCommand.runUsecase(MD: MonadDefer<F>): Kind<F, Either<ArticleCommentError, Comment>> {
    val cmd = this
    return MD.binding {
      getArticleBySlug(cmd.slug, cmd.user.some()).bind().fold(
        { ArticleCommentError.NotFound.left() },
        { addComment(it.id, cmd.comment, cmd.user).bind().right() }
      )
    }
  }
}

interface DeleteCommentUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>
  val getComment: GetComment<F>
  val deleteComment: DeleteComment<F>

  fun DeleteCommentCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Either<ArticleCommentDeleteError, Int>> {
    val cmd = this
    return MD.binding {
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
    }
  }
}

interface GetCommentsUseCase<F> {
  val getArticleBySlug: GetArticleBySlug<F>
  val getComments: GetComments<F>

  fun GetCommentsCommand.runUseCase(MD: MonadDefer<F>): Kind<F, Option<List<Comment>>> {
    val cmd = this
    return MD.binding {
      getArticleBySlug(cmd.slug, cmd.user).bind().fold(
        { none<List<Comment>>() },
        { getComments(it.id, cmd.user).bind().some() }
      )
    }
  }
}

interface GetTagsUseCase<F> {
  val getTags: GetTags<F>

  fun GetTagsCommand.runUseCase(): Kind<F, Set<String>> = getTags()
}

private fun Option<Article>.getOrSystemError(slug: String) = this.getOrElse {
  throw RuntimeException("System error: article '$slug' should have been found")
}
