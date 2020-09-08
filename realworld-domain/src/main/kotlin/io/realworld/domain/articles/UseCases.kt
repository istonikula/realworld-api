package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.none
import arrow.core.right
import arrow.core.some
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.fx
import arrow.fx.extensions.io.monad.monad
import arrow.fx.fix
import arrow.mtl.EitherT
import io.realworld.domain.common.toEither
import io.realworld.domain.users.User
import java.util.UUID
import arrow.mtl.extensions.eithert.monad.monad
import arrow.mtl.value

data class CreateArticleCommand(val data: ArticleCreation, val user: User)
data class DeleteArticleCommand(val slug: String, val user: User)
data class GetArticleCommand(val slug: String, val user: Option<User>)
data class GetArticlesCommand(val filter: ArticleFilter, val user: Option<User>)
data class GetFeedsCommand(val filter: FeedFilter, val user: User)
data class UpdateArticleCommand(val data: ArticleUpdate, val slug: String, val user: User)
data class FavoriteArticleCommand(val slug: String, val user: User)
data class UnfavoriteArticleCommand(val slug: String, val user: User)
data class CommentArticleCommand(val slug: String, val comment: String, val user: User)
data class DeleteCommentCommand(val slug: String, val commentId: ArticleScopedCommentId, val user: User)
data class GetCommentsCommand(val slug: String, val user: Option<User>)
object GetTagsCommand

interface CreateArticleUseCase {
  val createUniqueSlug: CreateUniqueSlug
  val createArticle: CreateArticle

  fun CreateArticleCommand.runUseCase(): IO<Article> {
    val cmd = this
    return IO.fx {
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
    return IO.fx {
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

interface GetArticlesUseCase {
  val getArticles: GetArticles
  val getArticlesCount: GetArticlesCount

  fun GetArticlesCommand.runUseCase(): IO<Pair<List<Article>, Long>> {
    val cmd = this
    return IO.fx {
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

interface GetFeedsUseCase {
  val getFeeds: GetFeeds
  val getFeedsCount: GetFeedsCount

  fun GetFeedsCommand.runUseCase(): IO<Pair<List<Article>, Long>> {
    val cmd = this
    return IO.fx {
      val count = getFeedsCount(cmd.user).bind()
      if (count == 0L)
        Pair(listOf(), 0L)
      else {
        val feeds = getFeeds(cmd.filter, cmd.user).bind()
        Pair(feeds, count)
      }
    }
  }
}

interface UpdateArticleUseCase {
  val validateUpdate: ValidateArticleUpdate
  val updateArticle: UpdateArticle

  fun UpdateArticleCommand.runUseCase(): IO<Either<ArticleUpdateError, Article>> {
    val cmd = this
    return EitherT.monad<ArticleUpdateError, ForIO>(IO.monad()).fx.monad {
      val validUpdate = EitherT(validateUpdate(cmd.data, cmd.slug, cmd.user)).bind()
      EitherT(updateArticle(validUpdate, cmd.user).map { it.right() }).bind()
    }.value().fix()
  }
}

interface FavoriteUseCase {
  val getArticleBySlug: GetArticleBySlug
  val addFavorite: AddFavorite

  fun FavoriteArticleCommand.runUseCase(): IO<Either<ArticleFavoriteError, Article>> {
    val cmd = this
    return IO.fx {
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

interface UnfavoriteUseCase {
  val getArticleBySlug: GetArticleBySlug
  val removeFavorite: RemoveFavorite

  fun UnfavoriteArticleCommand.runUseCase(): IO<Either<ArticleUnfavoriteError, Article>> {
    val cmd = this
    return IO.fx {
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

interface CommentUseCase {
  val getArticleBySlug: GetArticleBySlug
  val addComment: AddComment

  fun CommentArticleCommand.runUsecase(): IO<Either<ArticleCommentError, Comment>> {
    val cmd = this
    return IO.fx {
      getArticleBySlug(cmd.slug, cmd.user.some()).bind().fold(
        { ArticleCommentError.NotFound.left() },
        { addComment(it.id, cmd.comment, cmd.user).bind().right() }
      )
    }
  }
}

interface DeleteCommentUseCase {
  val getArticleBySlug: GetArticleBySlug
  val getComment: GetComment
  val deleteComment: DeleteComment

  fun DeleteCommentCommand.runUseCase(): IO<Either<ArticleCommentDeleteError, Int>> {
    val cmd = this
    return EitherT.monad<ArticleCommentDeleteError, ForIO>(IO.monad()).fx.monad {
      val article = EitherT(
        getArticleBySlug(cmd.slug, cmd.user.some()).map {
          it.toEither { ArticleCommentDeleteError.ArticleNotFound }
        }
      ).bind()

      val comment = EitherT(
        getComment(article.id, cmd.commentId, cmd.user).map {
          it.toEither { ArticleCommentDeleteError.CommentNotFound }
        }
      ).bind()

      EitherT(
        IO { (comment.author.username == cmd.user.username).toEither { ArticleCommentDeleteError.NotAuthor } }
      ).bind()

      EitherT(
        deleteComment(article.id, cmd.commentId).map { it.right() }
      ).bind()
    }.value().fix()
  }
}

interface GetCommentsUseCase {
  val getArticleBySlug: GetArticleBySlug
  val getComments: GetComments

  fun GetCommentsCommand.runUseCase(): IO<Option<List<Comment>>> {
    val cmd = this
    return IO.fx {
      getArticleBySlug(cmd.slug, cmd.user).bind().fold(
        { none<List<Comment>>() },
        { getComments(it.id, cmd.user).bind().some() }
      )
    }
  }
}

interface GetTagsUseCase {
  val getTags: GetTags

  fun GetTagsCommand.runUseCase(): IO<Set<String>> = getTags()
}

private fun Option<Article>.getOrSystemError(slug: String) = this.getOrElse {
  throw RuntimeException("System error: article '$slug' should have been found")
}
