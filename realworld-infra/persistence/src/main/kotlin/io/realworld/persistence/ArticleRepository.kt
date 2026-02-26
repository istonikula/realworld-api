package io.realworld.persistence

import io.realworld.domain.articles.Article
import io.realworld.domain.articles.ArticleFilter
import io.realworld.domain.articles.ArticleId
import io.realworld.domain.articles.ArticleScopedCommentId
import io.realworld.domain.articles.Comment
import io.realworld.domain.articles.FeedFilter
import io.realworld.domain.articles.ValidArticleCreation
import io.realworld.domain.articles.ValidArticleUpdate
import io.realworld.domain.articles.articleId
import io.realworld.domain.articles.articleScopedCommentId
import io.realworld.domain.profiles.Profile
import io.realworld.domain.users.User
import io.realworld.domain.users.UserId
import io.realworld.domain.users.userId
import io.realworld.persistence.Dsl.eq
import io.realworld.persistence.Dsl.insert
import io.realworld.persistence.Dsl.now
import io.realworld.persistence.Dsl.set
import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

private data class ArticleRow(
  val id: ArticleId,
  val slug: String,
  val title: String,
  val description: String,
  val body: String,
  val authorId: UserId,
  val createdAt: Instant,
  val updatedAt: Instant
) {
  companion object {
    fun fromRs(rs: ResultSet) = with(ArticleTbl) {
      ArticleRow(
        id = UUID.fromString(rs.getString(id)).articleId(),
        slug = rs.getString(slug),
        title = rs.getString(title),
        description = rs.getString(description),
        body = rs.getString(body),
        authorId = UUID.fromString(rs.getString(author)).userId(),
        createdAt = rs.getTimestamp(created_at).toInstant(),
        updatedAt = rs.getTimestamp(updated_at).toInstant()
      )
    }
  }
}

private data class ArticleDeps(
  val tagList: MutableList<String> = mutableListOf(),
  var author: Profile? = null,
  var favorited: Boolean = false,
  var favoritesCount: Long = 0L
)

private fun Article.Companion.from(row: ArticleRow, deps: ArticleDeps) = Article(
  id = row.id,
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

private data class CommentRow(
  val id: Long,
  val articleScopedId: Long,
  val createdAt: Instant,
  val updatedAt: Instant,
  val body: String,
  val authorId: UserId
) {
  companion object {
    fun fromRs(rs: ResultSet) = with(ArticleCommentTbl) {
      CommentRow(
        id = rs.getLong(id),
        articleScopedId = rs.getLong(article_scoped_id),
        createdAt = rs.getTimestamp(ArticleTbl.created_at).toInstant(),
        updatedAt = rs.getTimestamp(ArticleTbl.updated_at).toInstant(),
        body = rs.getString(body),
        authorId = UUID.fromString(rs.getString(author)).userId()
      )
    }
  }
}

private data class CommentDeps(
  val author: Profile
)

private fun Comment.Companion.from(row: CommentRow, deps: CommentDeps) = Comment(
  id = row.articleScopedId.articleScopedCommentId(),
  createdAt = row.createdAt,
  updatedAt = row.updatedAt,
  body = row.body,
  author = deps.author
)

class ArticleRepository(
  private val jdbcTemplate: NamedParameterJdbcTemplate,
  private val userRepo: UserRepository
) {

  suspend fun create(article: ValidArticleCreation, user: User): Article {
    val row = insertArticleRow(article, user)

    val deps = ArticleDeps()
    deps.author = Profile(
      username = user.username,
      bio = user.bio,
      image = user.image,
      following = false
    )

    if (article.tagList.isNotEmpty()) {
      insertTags(article.tagList)
      insertArticleTags(article.id, article.tagList)
      deps.tagList.addAll(article.tagList)
    }

    return Article.from(row, deps)
  }

  suspend fun existsBySlug(slug: String): Boolean = ArticleTbl.let {
    jdbcTemplate.queryIfExists(it.table, it.slug.eq(), mapOf(it.slug to slug))
  }

  suspend fun getBySlug(slug: String, user: User?): Article? =
    fetchRowBySlug(slug)?.let { Article.from(it, loadArticleDeps(it, user)) }

  suspend fun deleteArticle(articleId: ArticleId): Int = with(ArticleTbl) {
    val sql = "DELETE FROM $table WHERE ${id.eq()}"
    val params = mapOf(id to articleId.value)
    jdbcTemplate.update(sql, params)
  }

  suspend fun updateArticle(update: ValidArticleUpdate, user: User): Article {
    val row = updateArticleRow(update)
    return Article.from(row, loadArticleDeps(row, user))
  }

  suspend fun addFavorite(articleId: ArticleId, user: User): Int = with(ArticleFavoriteTbl) {
    val sql = "${table.insert(article_id, user_id)} ON CONFLICT ($article_id, $user_id) DO NOTHING"
    val params = mapOf(article_id to articleId.value, user_id to user.id.value)
    jdbcTemplate.update(sql, params)
  }

  suspend fun removeFavorite(articleId: ArticleId, user: User): Int = with(ArticleFavoriteTbl) {
    val sql = "DELETE FROM $table WHERE ${article_id.eq()} AND ${user_id.eq()}"
    val params = mapOf(article_id to articleId.value, user_id to user.id.value)
    jdbcTemplate.update(sql, params)
  }

  suspend fun getComment(articleId: ArticleId, commentId: ArticleScopedCommentId, user: User): Comment? =
    with(ArticleCommentTbl) {
      val sql =
        """
        SELECT *
        FROM $view
        WHERE
          ${article_id.eq()} AND
          ${article_scoped_id.eq()} AND
          ${deleted.eq()}
        """.trimIndent()
      val params = mapOf(
        article_id to articleId.value,
        article_scoped_id to commentId.value,
        deleted to false
      )
      DataAccessUtils.singleResult(
        jdbcTemplate.query(sql, params) { rs, _ -> CommentRow.fromRs(rs) }
      )
    }?.let {
      Comment.from(it, CommentDeps(fetchAuthor(it.authorId, user)))
    }

  suspend fun addComment(articleId: ArticleId, comment: String, user: User): Comment {
    val row = insertCommentRow(articleId, comment, user)
    val deps = CommentDeps(fetchAuthor(row.authorId, user))
    return Comment.from(row, deps)
  }

  suspend fun deleteComment(articleId: ArticleId, commentId: ArticleScopedCommentId): Int = with(ArticleCommentTbl) {
    jdbcTemplate.update(
      """
      UPDATE $table
        SET ${deleted.eq()}
      FROM $view
      WHERE
        $table.$id = $view.$id AND
        $view.${article_id.eq()} AND
        $view.${article_scoped_id.eq()}
      """.trimIndent(),
      mapOf(
        deleted to true,
        article_id to articleId.value,
        article_scoped_id to commentId.value
      )
    )
  }

  suspend fun getComments(articleId: ArticleId, user: User?): List<Comment> = with(ArticleCommentTbl) {
    val sql = "SELECT * from $view WHERE ${article_id.eq()} AND ${deleted.eq()}"
    val params = mapOf(
      article_id to articleId.value,
      deleted to false
    )
    jdbcTemplate.query(sql, params) { rs, _ -> CommentRow.fromRs(rs) }.map {
      Comment.from(it, CommentDeps(fetchAuthor(it.authorId, user)))
    }
  }

  suspend fun getArticles(filter: ArticleFilter, user: User?): List<Article> {
    val rows = fetchArticleRows(filter.toQueryParts(), filter.limit, filter.offset)
    // NOTE: opt for simplicity (query limit defaults to 20), thus let's loop
    return rows.map { row -> Article.from(row, loadArticleDeps(row, user)) }
  }

  suspend fun getArticlesCount(filter: ArticleFilter): Long =
    fetchArticleRowCount(filter.toQueryParts())

  suspend fun getFeeds(filter: FeedFilter, user: User): List<Article> {
    val rows = fetchArticleRows(user.toFeedsQueryParts(), filter.limit, filter.offset)
    // NOTE: opt for simplicity (query limit defaults to 20), thus let's loop
    return rows.map { row -> Article.from(row, loadArticleDeps(row, user)) }
  }

  suspend fun getFeedsCount(user: User): Long =
    fetchArticleRowCount(user.toFeedsQueryParts())

  suspend fun getTags(): Set<String> = with(TagTbl) {
    jdbcTemplate.query("SELECT $name FROM $table") { rs, _ -> rs.getString(name) }.toSet()
  }

  private suspend fun loadArticleDeps(row: ArticleRow, user: User?): ArticleDeps =
    ArticleDeps().apply {
      favorited = user?.let { isFavorited(row.id, it) } ?: false
      favoritesCount = fetchFavoritesCount(row.id)
      tagList.addAll(fetchArticleTags(row.id))
      author = fetchAuthor(row.authorId, user)
    }

  private suspend fun insertCommentRow(articleId: ArticleId, comment: String, user: User) = with(ArticleCommentTbl) {
    val sql = "${table.insert(body, author, article_id)} RETURNING $id"
    val params = mapOf(
      body to comment,
      author to user.id.value,
      article_id to articleId.value
    )
    val commentId = jdbcTemplate.queryForObject(sql, params) { rs, _ -> rs.getLong(id) }!!

    jdbcTemplate.queryForObject(
      "SELECT * FROM $view WHERE ${id.eq()}",
      mapOf(id to commentId)
    ) { rs, _ -> CommentRow.fromRs(rs) }!!
  }

  private suspend fun updateArticleRow(update: ValidArticleUpdate): ArticleRow = with(ArticleTbl) {
    val sql =
      """
      UPDATE $table
      SET
        ${slug.set()},
        ${title.set()},
        ${description.set()},
        ${body.set()},
        ${updated_at.now()}
      WHERE
        ${id.eq()}
      RETURNING *
      """
    val params = mapOf(
      slug to update.slug,
      title to update.title,
      description to update.description,
      body to update.body,
      id to update.id.value
    )
    jdbcTemplate.queryForObject(sql, params) { rs, _ -> ArticleRow.fromRs(rs) }!!
  }

  private suspend fun fetchArticleTags(articleId: ArticleId): List<String> = with(ArticleTagTbl) {
    val sql = "SELECT $tag FROM $table WHERE ${article_id.eq()}"
    val params = mapOf(article_id to articleId.value)
    jdbcTemplate.query(sql, params) { rs, _ -> rs.getString(tag) }
  }

  private suspend fun fetchRowBySlug(slug: String): ArticleRow? = ArticleTbl.let {
    val sql = "SELECT * FROM ${it.table} WHERE ${it.slug.eq()}"
    val params = mapOf(it.slug to slug)
    DataAccessUtils.singleResult(
      jdbcTemplate.query(sql, params) { rs, _ -> ArticleRow.fromRs(rs) }
    )
  }

  private suspend fun fetchArticleRows(
    queryParts: ArticlesQueryParts,
    limit: Int,
    offset: Int
  ): List<ArticleRow> = with(ArticleTbl) {
    val sql = """
      SELECT a.* FROM $table a ${queryParts.joinsSql} ${queryParts.wheresSql}
      ORDER BY $updated_at DESC
      LIMIT :limit
      OFFSET :offset
    """
    val params = queryParts.params.apply {
      put("limit", limit)
      put("offset", offset)
    }
    jdbcTemplate.query(sql, params) { rs, _ -> ArticleRow.fromRs(rs) }
  }

  private suspend fun fetchArticleRowCount(queryParts: ArticlesQueryParts): Long = with(ArticleTbl) {
    val sql = "SELECT COUNT(a.*) FROM $table a ${queryParts.joinsSql} ${queryParts.wheresSql}"
    jdbcTemplate.queryForObject(sql, queryParts.params) { rs, _ -> rs.getLong("count") }!!
  }

  private data class ArticlesQueryParts(
    val joinsSql: String,
    val wheresSql: String,
    val params: MutableMap<String, Any>
  )
  private fun ArticleFilter.toQueryParts(): ArticlesQueryParts {
    val filter = this
    val a = ArticleTbl
    val u = UserTbl
    val t = ArticleTagTbl
    val f = ArticleFavoriteTbl

    val joins = mutableListOf<String>()
    if (filter.author != null) {
      joins += "${u.table} u ON (u.${u.id} = a.${a.author})"
    }
    if (filter.tag != null) {
      joins += "${t.table} t ON (t.${t.article_id} = a.${a.id})"
    }
    if (filter.favorited != null) {
      joins += "${f.table} f ON (f.${f.article_id} = a.${a.id})"
    }

    val wheres = mutableListOf<String>()
    if (filter.author != null) {
      wheres += "u.${u.username} = :author"
    }
    if (filter.tag != null) {
      wheres += "t.${t.tag} = :tag"
    }
    if (filter.favorited != null) {
      wheres += "f.${f.user_id} = (SELECT ${u.id} from ${u.table} WHERE ${u.username} = :favorited)"
    }

    val joinsSql = if (joins.isEmpty()) "" else joins.joinToString(prefix = "JOIN ", separator = " JOIN ")
    val wheresSql = if (wheres.isEmpty()) "" else wheres.joinToString(prefix = "WHERE ", separator = " AND ")

    val params = mutableMapOf<String, Any>().apply {
      if (filter.author != null) {
        put("author", filter.author!!)
      }
      if (filter.tag != null) {
        put("tag", filter.tag!!)
      }
      if (filter.favorited != null) {
        put("favorited", filter.favorited!!)
      }
    }

    return ArticlesQueryParts(joinsSql, wheresSql, params)
  }
  private fun User.toFeedsQueryParts(): ArticlesQueryParts {
    val a = ArticleTbl
    val f = FollowTbl
    return ArticlesQueryParts(
      "JOIN ${f.table} f ON (f.${f.followee} = a.${a.author})",
      "WHERE f.${f.follower} = :follower",
      mutableMapOf("follower" to this.id.value)
    )
  }

  private suspend fun fetchAuthor(id: UserId, querier: User?): Profile =
    userRepo.findById(id)?.let { userAndPassword ->
      userAndPassword.user.let { author ->
        Profile(
          username = author.username,
          bio = author.bio,
          image = author.image,
          following = querier?.let { userRepo.hasFollower(author.id, it.id) }
        )
      }
    } ?: throw RuntimeException("Corrupt DB: article author $id not found")

  private suspend fun fetchFavoritesCount(articleId: ArticleId): Long = with(ArticleFavoriteTbl) {
    val sql = "SELECT COUNT(*) FROM $table WHERE ${article_id.eq()}"
    val params = mapOf(article_id to articleId.value)
    jdbcTemplate.queryForObject(sql, params) { rs, _ -> rs.getLong("count") }!!
  }

  private suspend fun isFavorited(articleId: ArticleId, user: User): Boolean = with(ArticleFavoriteTbl) {
    jdbcTemplate.queryIfExists(
      table,
      "${article_id.eq()} AND ${user_id.eq()}",
      mapOf(article_id to articleId.value, user_id to user.id.value)
    )
  }

  private suspend fun insertArticleRow(article: ValidArticleCreation, user: User): ArticleRow = with(ArticleTbl) {
    val sql = "${table.insert(id, slug, title, description, body, author)} RETURNING *"
    val params = mapOf(
      id to article.id.value,
      slug to article.slug,
      title to article.title,
      description to article.description,
      body to article.body,
      author to user.id.value
    )
    jdbcTemplate.queryForObject(sql, params) { rs, _ -> ArticleRow.fromRs(rs) }!!
  }

  private suspend fun insertTags(tags: List<String>) = with(TagTbl) {
    val sql = "${table.insert(name)} ON CONFLICT ($name) DO NOTHING"
    val params = tags.map { mapOf(name to it) }.toTypedArray()
    jdbcTemplate.batchUpdate(sql, params)
  }

  private suspend fun insertArticleTags(articleId: ArticleId, tags: List<String>) = with(ArticleTagTbl) {
    val sql = table.insert(article_id, tag)
    val params = tags.map {
      mapOf(
        article_id to articleId.value,
        tag to it
      )
    }.toTypedArray()
    jdbcTemplate.batchUpdate(sql, params)
  }
}
