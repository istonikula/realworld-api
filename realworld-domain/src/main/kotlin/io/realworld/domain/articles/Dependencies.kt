package io.realworld.domain.articles

import arrow.core.Either
import arrow.effects.IO
import io.realworld.domain.users.User

typealias CreateArticle = (article: ValidArticleCreation, user: User) -> IO<Article>

typealias ValidateArticleUpdate = (update: ArticleUpdate, user: User) ->
  IO<Either<ArticleUpdateError, ValidArticleUpdate>>
typealias UpdateArticle = (update: ValidArticleUpdate, currentSlug: String) -> IO<Article>

typealias CreateUniqueSlug = (title: String) -> IO<String>
typealias ExistsBySlug = (slug: String) -> IO<Boolean>
