package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import arrow.core.raise.either
import arrow.core.getOrElse
import arrow.core.raise.ensure
import arrow.core.some
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
data class DeleteCommentCommand(val slug: String, val commentId: ArticleScopedCommentId, val user: User)
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

interface CreateArticleUseCase {
  val createUniqueSlug: CreateUniqueSlug
  val createArticle: CreateArticle

  suspend fun CreateArticleCommand.runUseCase(): Article {
    val cmd = this

    val slug = createUniqueSlug(cmd.data.title)
    return createArticle(
      ValidArticleCreation(
        id = UUID.randomUUID().articleId(),
        slug = slug,
        title = cmd.data.title,
        description = cmd.data.description,
        body = cmd.data.body,
        tagList = cmd.data.tagList
      ),
      cmd.user
    )
  }
}

interface GetArticleUseCase {
  val getArticleBySlug: GetArticleBySlug

  suspend fun GetArticleCommand.runUseCase(): Option<Article> =
    getArticleBySlug(slug, user)
}

interface DeleteArticleUseCase {
  val getArticleBySlug: GetArticleBySlug
  val deleteArticle: DeleteArticle

  suspend fun DeleteArticleCommand.runUseCase(): Either<ArticleDeleteError, Int> {
    val cmd = this

    return either {
      val article = getArticleBySlug(cmd.slug, cmd.user.some()).toEither { ArticleDeleteError.NotFound }.bind()
      ensure(article.author.username == cmd.user.username) { ArticleDeleteError.NotAuthor }
      deleteArticle(article.id)
    }
  }
}

interface GetArticlesUseCase {
  val getArticles: GetArticles
  val getArticlesCount: GetArticlesCount

  suspend fun GetArticlesCommand.runUseCase(): Pair<List<Article>, Long> {
    val cmd = this

    return when (val count = getArticlesCount(cmd.filter)) {
      0L -> Pair(listOf(), 0L)
      else -> {
        val articles = getArticles(cmd.filter, cmd.user)
        Pair(articles, count)
      }
    }
  }
}

interface GetFeedsUseCase {
  val getFeeds: GetFeeds
  val getFeedsCount: GetFeedsCount

  suspend fun GetFeedsCommand.runUseCase(): Pair<List<Article>, Long> {
    val cmd = this

    return when (val count = getFeedsCount(cmd.user)) {
      0L -> Pair(listOf(), 0L)
      else -> {
        val feeds = getFeeds(cmd.filter, cmd.user)
        Pair(feeds, count)
      }
    }
  }
}

interface UpdateArticleUseCase {
  val validateUpdate: ValidateArticleUpdate
  val updateArticle: UpdateArticle

  suspend fun UpdateArticleCommand.runUseCase(): Either<ArticleUpdateError, Article> {
    val cmd = this

    return either {
      val validUpdate = validateUpdate(cmd.data, cmd.slug, cmd.user).bind()
      updateArticle(validUpdate, cmd.user)
    }
  }
}

interface FavoriteUseCase {
  val getArticleBySlug: GetArticleBySlug
  val addFavorite: AddFavorite

  suspend fun FavoriteArticleCommand.runUseCase(): Either<ArticleFavoriteError, Article> {
    val cmd = this

    return either {
      val article = getArticleBySlug(cmd.slug, cmd.user.some()).toEither { ArticleFavoriteError.NotFound }.bind()
      when {
        article.author.username == cmd.user.username ->
          raise(ArticleFavoriteError.Author)
        article.favorited ->
          article
        else -> {
          addFavorite(article.id, cmd.user)
          getArticleBySlug(cmd.slug, cmd.user.some()).getOrSystemError(cmd.slug)
        }
      }
    }
  }
}

interface UnfavoriteUseCase {
  val getArticleBySlug: GetArticleBySlug
  val removeFavorite: RemoveFavorite

  suspend fun UnfavoriteArticleCommand.runUseCase(): Either<ArticleUnfavoriteError, Article> {
    val cmd = this

    return either {
      val article = getArticleBySlug(cmd.slug, cmd.user.some()).toEither { ArticleUnfavoriteError.NotFound }.bind()
      when {
        !article.favorited ->
          article
        else -> {
          removeFavorite(article.id, cmd.user)
          getArticleBySlug(cmd.slug, cmd.user.some()).getOrSystemError(cmd.slug)
        }
      }
    }
  }
}

interface CommentUseCase {
  val getArticleBySlug: GetArticleBySlug
  val addComment: AddComment

  suspend fun CommentArticleCommand.runUseCase(): Either<ArticleCommentError, Comment> {
    val cmd = this

    return either {
      val article = getArticleBySlug(cmd.slug, cmd.user.some()).toEither { ArticleCommentError.NotFound }.bind()
      addComment(article.id, cmd.comment, cmd.user)
    }
  }
}

interface DeleteCommentUseCase {
  val getArticleBySlug: GetArticleBySlug
  val getComment: GetComment
  val deleteComment: DeleteComment

  suspend fun DeleteCommentCommand.runUseCase(): Either<ArticleCommentDeleteError, Int> {
    val cmd = this

    return either {
      val article = getArticleBySlug(cmd.slug, cmd.user.some())
        .toEither { ArticleCommentDeleteError.ArticleNotFound }.bind()

      val comment = getComment(article.id, cmd.commentId, cmd.user)
        .toEither { ArticleCommentDeleteError.CommentNotFound }.bind()

      ensure(comment.author.username == cmd.user.username) { ArticleCommentDeleteError.NotAuthor }

      deleteComment(article.id, cmd.commentId)
    }
  }
}

interface GetCommentsUseCase {
  val getArticleBySlug: GetArticleBySlug
  val getComments: GetComments

  suspend fun GetCommentsCommand.runUseCase(): Option<List<Comment>> {
    val cmd = this

    return getArticleBySlug(cmd.slug, cmd.user).map { getComments(it.id, cmd.user) }
  }
}

interface GetTagsUseCase {
  val getTags: GetTags

  suspend fun GetTagsCommand.runUseCase(): Set<String> = getTags()
}

private fun Option<Article>.getOrSystemError(slug: String) = getOrElse {
  throw RuntimeException("System error: article '$slug' should have been found")
}
