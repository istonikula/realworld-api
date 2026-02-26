package io.realworld.domain.articles

import io.realworld.domain.profiles.Profile
import java.time.Instant
import java.util.UUID

@JvmInline value class ArticleId(val value: UUID)
fun UUID.articleId() = ArticleId(this)

data class ArticleCreation(
  val title: String,
  val description: String,
  val body: String,
  val tagList: List<String>
)

data class ValidArticleCreation(
  val id: ArticleId,
  val slug: String,
  val title: String,
  val description: String,
  val body: String,
  val tagList: List<String>
)

data class ArticleUpdate(
  val title: String?,
  val description: String?,
  val body: String?
)

data class ValidArticleUpdate(
  val id: ArticleId,
  val slug: String,
  val title: String,
  val description: String,
  val body: String
)

data class Article(
  val id: ArticleId,
  val slug: String,
  val title: String,
  val description: String,
  val body: String,
  val tagList: List<String>,
  val favorited: Boolean,
  val favoritesCount: Long,
  val author: Profile,
  val createdAt: Instant,
  val updatedAt: Instant
) { companion object }

@JvmInline value class ArticleScopedCommentId(val value: Long)
fun Long.articleScopedCommentId() = ArticleScopedCommentId(this)
data class Comment(
  val id: ArticleScopedCommentId,
  val createdAt: Instant,
  val updatedAt: Instant,
  val body: String,
  val author: Profile
) { companion object }

data class ArticleFilter(
  val limit: Int = 20,
  val offset: Int = 0,
  val author: String?,
  val tag: String?, // TODO tags
  val favorited: String?
)

data class FeedFilter(
  val limit: Int = 20,
  val offset: Int = 0
)
