package io.realworld.domain.articles

import arrow.core.Either
import arrow.core.Option
import io.realworld.domain.users.User

typealias CreateArticle = suspend (ValidArticleCreation, User) -> Article

typealias ValidateArticleUpdate = suspend (ArticleUpdate, slug: String, User) ->
  Either<ArticleUpdateError, ValidArticleUpdate>
typealias UpdateArticle = suspend (ValidArticleUpdate, User) -> Article

typealias CreateUniqueSlug = suspend (title: String) -> String
typealias ExistsBySlug = suspend (slug: String) -> Boolean

typealias GetArticleBySlug = suspend (slug: String, Option<User>) -> Option<Article>
typealias GetArticles = suspend (ArticleFilter, Option<User>) -> List<Article>
typealias GetArticlesCount = suspend (ArticleFilter) -> Long
typealias GetFeeds = suspend (FeedFilter, User) -> List<Article>
typealias GetFeedsCount = suspend (user: User) -> Long

typealias DeleteArticle = suspend (ArticleId) -> Int

typealias AddFavorite = suspend (ArticleId, User) -> Int
typealias RemoveFavorite = suspend (ArticleId, User) -> Int

typealias AddComment = suspend (ArticleId, comment: String, User) -> Comment
typealias DeleteComment = suspend (ArticleId, ArticleScopedCommentId) -> Int
typealias GetComment = suspend (ArticleId, ArticleScopedCommentId, User) -> Option<Comment>
typealias GetComments = suspend (ArticleId, Option<User>) -> List<Comment>

typealias GetTags = suspend () -> Set<String>
