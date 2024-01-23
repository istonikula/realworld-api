package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.right
import arrow.core.some
import com.github.slugify.Slugify
import io.realworld.domain.users.User
import java.util.UUID

private val slugifier = Slugify.builder().build()
fun String.slugify(): String = slugifier.slugify(this)

interface CreateUniqueSlugService {
  val existsBySlug: ExistsBySlug

  suspend fun slugify(s: String): String {
    val slugified = s.slugify()
    var slugCandidate = slugified
    while (existsBySlug(slugCandidate)) {
      slugCandidate = "$slugified-${UUID.randomUUID().toString().substring(0, 8)}"
    }
    return slugCandidate
  }
}

interface ValidateArticleUpdateService {
  val createUniqueSlug: CreateUniqueSlug
  val getArticleBySlug: GetArticleBySlug

  suspend fun ArticleUpdate.validate(slug: String, user: User): Either<ArticleUpdateError, ValidArticleUpdate> {
    val cmd = this

    return either {
      val article = getArticleBySlug(slug, user.some()).toEither { ArticleUpdateError.NotFound }.bind()
      ensure( article.author.username == user.username) { ArticleUpdateError.NotAuthor }
      ValidArticleUpdate(
        id = article.id,
        slug = cmd.title.fold({ article.slug }, { createUniqueSlug(it) }),
        title = cmd.title.getOrElse { article.title },
        body = cmd.body.getOrElse { article.body },
        description = cmd.description.getOrElse { article.description }
      )

    }
  }
}
