package io.realworld.persistence

import arrow.core.toOption
import arrow.effects.IO
import io.realworld.domain.articles.Article
import io.realworld.domain.articles.ValidArticleCreation
import io.realworld.domain.profiles.Profile
import io.realworld.domain.users.User
import io.realworld.persistence.ArticleTbl.eq
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class ArticleRow(
  val id: UUID,
  val slug: String,
  val title: String,
  val description: String,
  val body: String,
  val authorId: UUID,
  val createdAt: Instant,
  val updatedAt: Instant
) {
  companion object {
    fun fromRs(rs: ResultSet) = with(ArticleTbl) {
      ArticleRow(
        id = UUID.fromString(rs.getString(id)),
        slug = rs.getString(slug),
        title = rs.getString(title),
        description = rs.getString(description),
        body = rs.getString(body),
        authorId = UUID.fromString(rs.getString(author)),
        createdAt = rs.getTimestamp(created_at).toInstant(),
        updatedAt = rs.getTimestamp(updated_at).toInstant()
      )
    }
  }
}

data class ArticleDeps(
  val tagList: MutableList<String> = mutableListOf(),
  var author: Profile? = null,
  var favorited: Boolean = false,
  var favoritesCount: Long = 0L
)

class ArticleRepository(val jdbcTemplate: NamedParameterJdbcTemplate) {

  fun create(article: ValidArticleCreation, user: User): IO<Article> {
    val u = UserTbl
    val sql = with(ArticleTbl) {
      """
      INSERT INTO $table (
        $id, $slug, $title, $description, $body, $author
      ) VALUES (
        :$id, :$slug, :$title, :$description, :$body,
        (SELECT ${u.id} FROM ${u.table} WHERE ${u.username} = :authorUsername)
      )
      RETURNING *
      """
    }

    val params = with(ArticleTbl) {
      mapOf(
        id to article.id,
        slug to article.slug,
        title to article.title,
        description to article.description,
        body to article.body,
        "authorUsername" to user.username
      )
    }

    return IO {
      val row = jdbcTemplate.queryForObject(
        sql,
        params,
        { rs, _ -> ArticleRow.fromRs(rs) }
      )!!

      val deps = ArticleDeps()
      deps.author = Profile(
        username = user.username,
        bio = user.bio.toOption(),
        image = user.image.toOption(),
        following = false.toOption()
      )

      if (article.tagList.isNotEmpty()) {
        insertTags(article.tagList)
        insertArticleTags(article.id, article.tagList)
        deps.tagList.addAll(article.tagList)
      }

      Article(
        slug = row.slug,
        title = row.title,
        description = row.description,
        body = row.body,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,

        tagList = deps.tagList,
        favoritesCount = deps.favoritesCount,
        favorited = deps.favorited,
        author = deps.author!!
      )
    }
  }

  fun existsBySlug(slug: String): IO<Boolean> = ArticleTbl.let {
    queryIfExists(it.table, "${it.slug.eq()}", mapOf(it.slug to slug))
  }

  private fun insertTags(tags: List<String>) = with(TagTbl) {
    val sql = "INSERT INTO $table ($name) VALUES (:$name) ON CONFLICT ($name) DO NOTHING"
    val params = tags.map { mapOf(name to it) }.toTypedArray()
    jdbcTemplate.batchUpdate(sql, params)
  }

  private fun insertArticleTags(articleId: UUID, tags: List<String>) = with(ArticleTagTbl) {
    val sql = "INSERT INTO $table ($article_id, $tag) VALUES (:$article_id, :$tag)"
    val params = tags.map {
      mapOf(
        article_id to articleId,
        tag to it
      )
    }.toTypedArray()
    jdbcTemplate.batchUpdate(sql, params)
  }

  // TODO extract util
  private fun queryIfExists(table: String, where: String, params: Map<String, Any>): IO<Boolean> =
    IO {
      jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM $table WHERE $where",
        params,
        { rs, _ -> rs.getInt("count") > 0 }
      )!!
    }
}
