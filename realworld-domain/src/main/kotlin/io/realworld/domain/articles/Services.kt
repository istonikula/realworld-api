package io.realworld.domain.articles

import arrow.Kind
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.core.some
import arrow.effects.typeclasses.MonadDefer
import arrow.typeclasses.binding
import com.github.slugify.Slugify
import io.realworld.domain.users.User
import java.util.UUID

private val slugifier = Slugify()
fun String.slugify() = slugifier.slugify(this)

interface CreateUniqueSlugService<F> {
  val existsBySlug: ExistsBySlug<F>
  val MD: MonadDefer<F>

  fun slufigy(s: String): Kind<F, String> = MD.binding {
    val slugified = s.slugify()
    var slugCandidate = slugified
    while (existsBySlug(slugCandidate).bind()) {
      slugCandidate = "$slugified-${UUID.randomUUID().toString().substring(0, 8)}"
    }
    slugCandidate
  }
}

interface ValidateArticleUpdateService<F> {
  val createUniqueSlug: CreateUniqueSlug<F>
  val getArticleBySlug: GetArticleBySlug<F>
  val MD: MonadDefer<F>

  fun ArticleUpdate.validate(slug: String, user: User): Kind<F, Either<ArticleUpdateError, ValidArticleUpdate>> {
    val cmd = this
    return MD.binding {
      getArticleBySlug(slug, user.some()).bind().fold(
        { ArticleUpdateError.NotFound.left() },
        {
          when {
            it.author.username != user.username ->
              ArticleUpdateError.NotAuthor.left()
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
    }
  }
}
