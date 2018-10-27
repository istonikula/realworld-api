package io.realworld.domain.articles

import arrow.core.Option
import io.realworld.domain.profiles.Profile
import java.time.Instant
import java.util.UUID

// TODO replace with inline class when kotlin 1.3 comes out
data class ArticleId(val value: UUID)
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
  val title: Option<String>,
  val description: Option<String>,
  val body: Option<String>
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

data class Comment(
  val id: Long,
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
