package io.realworld.domain.articles

import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.typeclasses.binding
import io.realworld.domain.users.User
import java.util.UUID

data class CreateArticleCommand(val data: ArticleCreation, val user: User)

sealed class ArticleUpdateError {
  object NotAllowed : ArticleUpdateError()
  object NotFound : ArticleUpdateError()
}

interface CreateArticleUseCase {
  val createUniqueSlug: CreateUniqueSlug
  val createArticle: CreateArticle

  fun CreateArticleCommand.runUseCase(): IO<Article> {
    val cmd = this
    return ForIO extensions {
      binding {
        val slug = createUniqueSlug(cmd.data.title).bind()
        createArticle(
          ValidArticleCreation(
            id = UUID.randomUUID(),
            slug = slug,
            title = cmd.data.title,
            description = cmd.data.description,
            body = cmd.data.body,
            tagList = cmd.data.tagList
          ),
          cmd.user
        ).bind()
      }.fix()
    }
  }
}
