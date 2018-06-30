package io.realworld.domain.articles

import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.typeclasses.binding
import com.github.slugify.Slugify
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
