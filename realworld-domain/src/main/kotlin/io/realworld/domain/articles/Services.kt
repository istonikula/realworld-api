package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.some
import com.github.slugify.Slugify
import io.realworld.domain.users.User
import java.util.UUID

private val slugifier = Slugify()
fun String.slugify() = slugifier.slugify(this)

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

    return getArticleBySlug(slug, user.some()).fold(
      { ArticleUpdateError.NotFound.left() },
      {
        when {
          it.author.username != user.username ->
            ArticleUpdateError.NotAuthor.left()
          else ->
            ValidArticleUpdate(
              id = it.id,
              slug = cmd.title.fold({ it.slug }, { createUniqueSlug(it) }),
              title = cmd.title.getOrElse { it.title },
              body = cmd.body.getOrElse { it.body },
              description = cmd.description.getOrElse { it.description }
            ).right()
        }
      }
    )
  }
}
