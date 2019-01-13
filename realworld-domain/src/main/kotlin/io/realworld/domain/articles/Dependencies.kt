package io.realworld.domain.articles

import arrow.Kind
import arrow.core.Either
import arrow.core.Option
import io.realworld.domain.users.User

typealias CreateArticle<F> = (ValidArticleCreation, User) -> Kind<F, Article>

typealias ValidateArticleUpdate<F> = (ArticleUpdate, slug: String, User) ->
  Kind<F, Either<ArticleUpdateError, ValidArticleUpdate>>
typealias UpdateArticle<F> = (ValidArticleUpdate, User) -> Kind<F, Article>

typealias CreateUniqueSlug<F> = (title: String) -> Kind<F, String>
typealias ExistsBySlug<F> = (slug: String) -> Kind<F, Boolean>

typealias GetArticleBySlug<F> = (slug: String, Option<User>) -> Kind<F, Option<Article>>
typealias GetArticles<F> = (ArticleFilter, Option<User>) -> Kind<F, List<Article>>
typealias GetArticlesCount<F> = (ArticleFilter) -> Kind<F, Long>
typealias GetFeeds<F> = (FeedFilter, User) -> Kind<F, List<Article>>
typealias GetFeedsCount<F> = (user: User) -> Kind<F, Long>

typealias DeleteArticle<F> = (ArticleId) -> Kind<F, Int>

typealias AddFavorite<F> = (ArticleId, User) -> Kind<F, Int>
typealias RemoveFavorite<F> = (ArticleId, User) -> Kind<F, Int>

typealias AddComment<F> = (ArticleId, comment: String, User) -> Kind<F, Comment>
typealias DeleteComment<F> = (id: Long) -> Kind<F, Int>
typealias GetComment<F> = (id: Long, User) -> Kind<F, Option<Comment>>
typealias GetComments<F> = (ArticleId, Option<User>) -> Kind<F, List<Comment>>

typealias GetTags<F> = () -> Kind<F, Set<String>>
