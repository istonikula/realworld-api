package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.some
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.typeclasses.binding
import com.github.slugify.Slugify
import io.realworld.domain.users.User
import java.util.UUID

private val slugifier = Slugify()
fun String.slugify() = slugifier.slugify(this)

interface CreateUniqueSlugService {
  val existsBySlug: ExistsBySlug

  fun slufigy(s: String): IO<String> = ForIO extensions {
    binding {
      val slugified = s.slugify()
      var slugCandidate = slugified
      while (existsBySlug(slugCandidate).bind()) {
        slugCandidate = "$slugified-${UUID.randomUUID().toString().substring(0, 8)}"
      }
      slugCandidate
    }.fix()
  }
}

interface ValidateArticleUpdateService {
  val createUniqueSlug: CreateUniqueSlug
  val getArticleBySlug: GetArticleBySlug

  fun ArticleUpdate.validate(slug: String, user: User): IO<Either<ArticleUpdateError, ValidArticleUpdate>> {
    val cmd = this
    return ForIO extensions {
      binding {
        getArticleBySlug(slug, user.some()).bind().fold(
          { ArticleUpdateError.NotFound.left() },
          {
            when {
              it.author.username != user.username ->
                ArticleUpdateError.NotOwner.left()
              else ->
                ValidArticleUpdate(
                  id = it.id,
                  slug = cmd.title.fold({ it.slug }, { createUniqueSlug(it).bind() }),
                  title = cmd.title.getOrElse { it.title },
                  body = cmd.body.getOrElse { it.body },
                  description = cmd.description.getOrElse { it.description }
                ).right()
            }
          }
        )
      }.fix()
    }
  }
}
