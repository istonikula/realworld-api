package io.realworld.domain.articles

import arrow.core.Option
import io.realworld.domain.profiles.Profile
import java.time.Instant
import java.util.UUID

data class ArticleCreation(
  val title: String,
  val description: String,
  val body: String,
  val tagList: List<String>
)

data class ValidArticleCreation(
  val id: UUID,
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
  val slug: String,
  val title: String,
  val description: String,
  val body: String
)

data class Article(
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
