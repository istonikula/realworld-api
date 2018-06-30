package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import arrow.effects.IO
import io.realworld.domain.users.User
import java.util.UUID

typealias CreateArticle = (article: ValidArticleCreation, user: User) -> IO<Article>

typealias ValidateArticleUpdate = (update: ArticleUpdate, user: User) ->
  IO<Either<ArticleUpdateError, ValidArticleUpdate>>
typealias UpdateArticle = (update: ValidArticleUpdate, currentSlug: String) -> IO<Article>

typealias CreateUniqueSlug = (title: String) -> IO<String>
typealias ExistsBySlug = (slug: String) -> IO<Boolean>

typealias GetArticleBySlug = (slug: String, user: Option<User>) -> IO<Option<Article>>

typealias DeleteArticle = (id: UUID) -> IO<Int>
