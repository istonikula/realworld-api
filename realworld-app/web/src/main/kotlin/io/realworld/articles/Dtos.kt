package io.realworld.articles

import arrow.core.toOption
import com.fasterxml.jackson.annotation.JsonRootName
import io.realworld.domain.articles.Article
import io.realworld.domain.articles.ArticleCreation
import io.realworld.domain.articles.ArticleUpdate
import io.realworld.domain.articles.Comment
import io.realworld.profiles.ProfileResponseDto
import java.time.Instant
import javax.validation.constraints.NotBlank

@JsonRootName("article")
data class CreationDto(
  @field:NotBlank
  val title: String,

  @field:NotBlank
  val description: String,

  @field:NotBlank
  val body: String,

  val tagList: List<String> = listOf()
) {
  fun toDomain() = ArticleCreation(
    title = title,
    description = description,
    body = body,
    tagList = tagList
  )
}

data class ArticleResponseDto(
  val slug: String,
  val title: String,
  val description: String,
  val body: String,
  val tagList: List<String>,
  val favorited: Boolean,
  val favoritesCount: Long,
  val author: ProfileResponseDto,
  val createdAt: Instant,
  val updatedAt: Instant
) {
  companion object {
    fun fromDomain(domain: Article) = with(domain) {
      ArticleResponseDto(
        slug = slug,
        title = title,
        description = description,
        body = body,
        tagList = tagList,
        favorited = favorited,
        favoritesCount = favoritesCount,
        author = ProfileResponseDto.fromDomain(author),
        createdAt = createdAt,
        updatedAt = updatedAt
      )
    }
  }
}

@JsonRootName("article")
data class UpdateDto(
  val title: String? = null,
  val description: String? = null,
  val body: String? = null
) {
  fun toDomain() = ArticleUpdate(
    title = title.toOption(),
    description = description.toOption(),
    body = body.toOption()
  )
}

@JsonRootName("comment")
data class CommentDto(val body: String)

data class CommentResponseDto(
  val id: Long,
  val createdAt: Instant,
  val updatedAt: Instant,
  val body: String,
  val author: ProfileResponseDto
) {
  companion object {
    fun fromDomain(domain: Comment) = with(domain) {
      CommentResponseDto(
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        body = body,
        author = ProfileResponseDto.fromDomain(author)
      )
    }
  }
}
