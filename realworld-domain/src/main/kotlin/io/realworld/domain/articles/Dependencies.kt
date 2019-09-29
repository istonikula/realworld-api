package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import arrow.effects.IO
import io.realworld.domain.users.User

typealias CreateArticle = (ValidArticleCreation, User) -> IO<Article>

typealias ValidateArticleUpdate = (ArticleUpdate, slug: String, User) ->
  IO<Either<ArticleUpdateError, ValidArticleUpdate>>
typealias UpdateArticle = (ValidArticleUpdate, User) -> IO<Article>

typealias CreateUniqueSlug = (title: String) -> IO<String>
typealias ExistsBySlug = (slug: String) -> IO<Boolean>

typealias GetArticleBySlug = (slug: String, Option<User>) -> IO<Option<Article>>
typealias GetArticles = (ArticleFilter, Option<User>) -> IO<List<Article>>
typealias GetArticlesCount = (ArticleFilter) -> IO<Long>
typealias GetFeeds = (FeedFilter, User) -> IO<List<Article>>
typealias GetFeedsCount = (user: User) -> IO<Long>

typealias DeleteArticle = (ArticleId) -> IO<Int>

typealias AddFavorite = (ArticleId, User) -> IO<Int>
typealias RemoveFavorite = (ArticleId, User) -> IO<Int>

typealias AddComment = (ArticleId, comment: String, User) -> IO<Comment>
typealias DeleteComment = (ArticleId, ArticleScopedCommentId) -> IO<Int>
typealias GetComment = (ArticleId, ArticleScopedCommentId, User) -> IO<Option<Comment>>
typealias GetComments = (ArticleId, Option<User>) -> IO<List<Comment>>

typealias GetTags = () -> IO<Set<String>>
