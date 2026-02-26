package io.realworld.articles

import io.realworld.ForbiddenException
import io.realworld.JwtTokenResolver
import io.realworld.authHeader
import io.realworld.domain.articles.Article
import io.realworld.domain.articles.ArticleCommentDeleteError
import io.realworld.domain.articles.ArticleCommentError
import io.realworld.domain.articles.ArticleDeleteError
import io.realworld.domain.articles.ArticleFavoriteError
import io.realworld.domain.articles.ArticleFilter
import io.realworld.domain.articles.ArticleUnfavoriteError
import io.realworld.domain.articles.ArticleUpdateError
import io.realworld.domain.articles.Comment
import io.realworld.domain.articles.CommentArticleCommand
import io.realworld.domain.articles.CommentUseCase
import io.realworld.domain.articles.CreateArticleCommand
import io.realworld.domain.articles.CreateArticleUseCase
import io.realworld.domain.articles.CreateUniqueSlugService
import io.realworld.domain.articles.DeleteArticleCommand
import io.realworld.domain.articles.DeleteArticleUseCase
import io.realworld.domain.articles.DeleteCommentCommand
import io.realworld.domain.articles.DeleteCommentUseCase
import io.realworld.domain.articles.FavoriteArticleCommand
import io.realworld.domain.articles.FavoriteUseCase
import io.realworld.domain.articles.FeedFilter
import io.realworld.domain.articles.GetArticleCommand
import io.realworld.domain.articles.GetArticleUseCase
import io.realworld.domain.articles.GetArticlesCommand
import io.realworld.domain.articles.GetArticlesUseCase
import io.realworld.domain.articles.GetCommentsCommand
import io.realworld.domain.articles.GetCommentsUseCase
import io.realworld.domain.articles.GetFeedsCommand
import io.realworld.domain.articles.GetFeedsUseCase
import io.realworld.domain.articles.GetTagsCommand
import io.realworld.domain.articles.GetTagsUseCase
import io.realworld.domain.articles.UnfavoriteArticleCommand
import io.realworld.domain.articles.UnfavoriteUseCase
import io.realworld.domain.articles.UpdateArticleCommand
import io.realworld.domain.articles.UpdateArticleUseCase
import io.realworld.domain.articles.ValidateArticleUpdate
import io.realworld.domain.articles.ValidateArticleUpdateService
import io.realworld.domain.articles.articleScopedCommentId
import io.realworld.domain.common.Auth
import io.realworld.domain.users.User
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import io.realworld.runReadTx
import io.realworld.runWriteTx
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.NativeWebRequest

data class ArticlesResponse(
  val articles: List<ArticleResponseDto>,
  val articlesCount: Long
) {
  companion object {
    fun fromDomain(domain: Pair<List<Article>, Long>) = ArticlesResponse(
      domain.first.map { ArticleResponseDto.fromDomain(it) },
      domain.second
    )
  }
}

data class ArticleResponse(val article: ArticleResponseDto) {
  companion object {
    fun fromDomain(domain: Article) = ArticleResponse(ArticleResponseDto.fromDomain(domain))
  }
}

data class CommentResponse(val comment: CommentResponseDto) {
  companion object {
    fun fromDomain(domain: Comment) = CommentResponse(CommentResponseDto.fromDomain(domain))
  }
}

data class CommentsResponse(val comments: List<CommentResponseDto>) {
  companion object {
    fun fromDomain(domain: List<Comment>) = CommentsResponse(
      domain.map { CommentResponseDto.fromDomain(it) }
    )
  }
}

data class TagsResponse(val tags: Set<String>)

@RestController
class ArticleController(
  private val auth: Auth,
  private val articleRepo: ArticleRepository,
  private val userRepo: UserRepository,
  private val txManager: PlatformTransactionManager
) {
  @PostMapping("/api/articles")
  fun create(@Valid @RequestBody dto: CreationDto, user: User): ResponseEntity<ArticleResponse> {
    return runWriteTx(txManager) {
      val existsBySlug = articleRepo::existsBySlug
      val createUniqueSlugSrv = object : CreateUniqueSlugService { override val existsBySlug = existsBySlug }
      val createUniqueSlug = createUniqueSlugSrv::slugify
      val createArticle = articleRepo::create
      object : CreateArticleUseCase {
        override val createUniqueSlug = createUniqueSlug
        override val createArticle = createArticle
      }.run {
        CreateArticleCommand(
          data = dto.toDomain(),
          user = user
        ).runUseCase()
      }.let {
        ResponseEntity.status(HttpStatus.CREATED).body(ArticleResponse.fromDomain(it))
      }
    }
  }

  @GetMapping("/api/articles")
  fun listArticles(
    filter: ArticleFilter,
    webRequest: NativeWebRequest
  ): ResponseEntity<ArticlesResponse> {
    return runReadTx(txManager) {
      val user = JwtTokenResolver(auth::parse)(webRequest.authHeader()).getOrNull()?.let { token ->
        userRepo.findById(token.id)?.user
      }
      val getArticles = articleRepo::getArticles
      val getArticlesCount = articleRepo::getArticlesCount
      object : GetArticlesUseCase {
        override val getArticles = getArticles
        override val getArticlesCount = getArticlesCount
      }.run {
        GetArticlesCommand(filter, user).runUseCase()
      }.let {
        ResponseEntity.ok(ArticlesResponse.fromDomain(it))
      }
    }
  }

  @GetMapping("/api/articles/feed")
  fun articlesFeed(
    filter: FeedFilter,
    user: User
  ): ResponseEntity<ArticlesResponse> {
    return runReadTx(txManager) {
      val getFeeds = articleRepo::getFeeds
      val getFeedsCount = articleRepo::getFeedsCount
      object : GetFeedsUseCase {
        override val getFeeds = getFeeds
        override val getFeedsCount = getFeedsCount
      }.run {
        GetFeedsCommand(filter, user).runUseCase()
      }.let {
        ResponseEntity.ok(ArticlesResponse.fromDomain(it))
      }
    }
  }

  @GetMapping("/api/articles/{slug}")
  fun getBySlug(
    @PathVariable("slug") slug: String,
    webRequest: NativeWebRequest
  ): ResponseEntity<ArticleResponse> {
    return runReadTx(txManager) {
      val user = JwtTokenResolver(auth::parse)(
        webRequest.authHeader()
      ).getOrNull()?.let { token ->
        userRepo.findById(token.id)?.user
      }
      val getArticleBySlug = articleRepo::getBySlug
      object : GetArticleUseCase {
        override val getArticleBySlug = getArticleBySlug
      }.run {
        GetArticleCommand(slug, user).runUseCase()
      }?.let {
        ResponseEntity.ok(ArticleResponse.fromDomain(it))
      } ?: ResponseEntity.notFound().build()
    }
  }

  @DeleteMapping("/api/articles/{slug}")
  fun deleteBySlug(
    @PathVariable("slug") slug: String,
    user: User
  ): ResponseEntity<Unit> {
    return runWriteTx(txManager) {
      val getArticleBySlug = articleRepo::getBySlug
      val deleteArticle = articleRepo::deleteArticle
      object : DeleteArticleUseCase {
        override val getArticleBySlug = getArticleBySlug
        override val deleteArticle = deleteArticle
      }.run {
        DeleteArticleCommand(slug, user).runUseCase()
      }.fold(
        {
          when (it) {
            is ArticleDeleteError.NotAuthor -> throw ForbiddenException()
            is ArticleDeleteError.NotFound -> ResponseEntity.notFound().build()
          }
        },
        { ResponseEntity.noContent().build() }
      )
    }
  }

  @PutMapping("/api/articles/{slug}")
  fun update(
    @PathVariable("slug") slug: String,
    @Valid @RequestBody update: UpdateDto,
    user: User
  ): ResponseEntity<ArticleResponse> {
    return runWriteTx(txManager) {
      val existsBySlug = articleRepo::existsBySlug
      val createUniqueSlugSrv = object : CreateUniqueSlugService { override val existsBySlug = existsBySlug }
      val createUniqueSlug = createUniqueSlugSrv::slugify
      val getArticleBySlug = articleRepo::getBySlug
      val validateUpdateSrv = object : ValidateArticleUpdateService {
        override val createUniqueSlug = createUniqueSlug
        override val getArticleBySlug = getArticleBySlug
      }
      val updateArticle = articleRepo::updateArticle
      object : UpdateArticleUseCase {
        override val validateUpdate: ValidateArticleUpdate = { x, y, z -> validateUpdateSrv.run { x.validate(y, z) } }
        override val updateArticle = updateArticle
      }.run {
        UpdateArticleCommand(update.toDomain(), slug, user).runUseCase()
      }.fold(
        {
          when (it) {
            is ArticleUpdateError.NotAuthor -> throw ForbiddenException()
            is ArticleUpdateError.NotFound -> ResponseEntity.notFound().build()
          }
        },
        { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
      )
    }
  }

  @PostMapping("/api/articles/{slug}/favorite")
  fun favorite(
    @PathVariable("slug") slug: String,
    user: User
  ): ResponseEntity<ArticleResponse> {
    return runWriteTx(txManager) {
      val getArticleBySlug = articleRepo::getBySlug
      val addFavorite = articleRepo::addFavorite
      object : FavoriteUseCase {
        override val getArticleBySlug = getArticleBySlug
        override val addFavorite = addFavorite
      }.run {
        FavoriteArticleCommand(slug, user).runUseCase()
      }.fold(
        {
          when (it) {
            is ArticleFavoriteError.Author -> throw ForbiddenException()
            is ArticleFavoriteError.NotFound -> ResponseEntity.notFound().build()
          }
        },
        { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
      )
    }
  }

  @DeleteMapping("/api/articles/{slug}/favorite")
  fun unfavorite(
    @PathVariable("slug") slug: String,
    user: User
  ): ResponseEntity<ArticleResponse> {
    return runWriteTx(txManager) {
      val getArticleBySlug = articleRepo::getBySlug
      val removeFavorite = articleRepo::removeFavorite
      object : UnfavoriteUseCase {
        override val getArticleBySlug = getArticleBySlug
        override val removeFavorite = removeFavorite
      }.run {
        UnfavoriteArticleCommand(slug, user).runUseCase()
      }.fold(
        {
          when (it) {
            is ArticleUnfavoriteError.NotFound -> ResponseEntity.notFound().build()
          }
        },
        { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
      )
    }
  }

  @GetMapping("/api/articles/{slug}/comments")
  fun getComments(
    @PathVariable("slug") slug: String,
    webRequest: NativeWebRequest
  ): ResponseEntity<CommentsResponse> {
    return runReadTx(txManager) {
      val user = JwtTokenResolver(auth::parse)(
        webRequest.authHeader()
      ).getOrNull()?.let { token ->
        userRepo.findById(token.id)?.user
      }
      val getArticleBySlug = articleRepo::getBySlug
      val getComments = articleRepo::getComments
      object : GetCommentsUseCase {
        override val getArticleBySlug = getArticleBySlug
        override val getComments = getComments
      }.run {
        GetCommentsCommand(slug, user).runUseCase()
      }?.let {
        ResponseEntity.ok(CommentsResponse.fromDomain(it))
      } ?: ResponseEntity.notFound().build()
    }
  }

  @PostMapping("/api/articles/{slug}/comments")
  fun comment(
    @PathVariable("slug") slug: String,
    @Valid @RequestBody comment: CommentDto,
    user: User
  ): ResponseEntity<CommentResponse> {
    return runWriteTx(txManager) {
      val getArticleBySlug = articleRepo::getBySlug
      val addComment = articleRepo::addComment
      object : CommentUseCase {
        override val getArticleBySlug = getArticleBySlug
        override val addComment = addComment
      }.run {
        CommentArticleCommand(slug, comment.body, user).runUseCase()
      }.fold(
        {
          when (it) {
            is ArticleCommentError.NotFound -> ResponseEntity.notFound().build()
          }
        },
        { ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.fromDomain(it)) }
      )
    }
  }

  @DeleteMapping("/api/articles/{slug}/comments/{id}")
  fun deleteComment(
    @PathVariable("slug") slug: String,
    @PathVariable("id") commentId: Long,
    user: User
  ): ResponseEntity<Void> {
    return runWriteTx(txManager) {
      val getArticleBySlug = articleRepo::getBySlug
      val deleteComment = articleRepo::deleteComment
      val getComment = articleRepo::getComment
      object : DeleteCommentUseCase {
        override val getArticleBySlug = getArticleBySlug
        override val deleteComment = deleteComment
        override val getComment = getComment
      }.run {
        DeleteCommentCommand(slug, commentId.articleScopedCommentId(), user).runUseCase()
      }.fold(
        {
          when (it) {
            is ArticleCommentDeleteError.ArticleNotFound -> ResponseEntity.notFound().build()
            is ArticleCommentDeleteError.CommentNotFound -> ResponseEntity.notFound().build()
            is ArticleCommentDeleteError.NotAuthor -> ResponseEntity.status(403).build()
          }
        },
        { ResponseEntity.noContent().build() }
      )
    }
  }

  @GetMapping("/api/tags")
  fun getTags(): ResponseEntity<TagsResponse> {
    return runReadTx(txManager) {
      val getTags = articleRepo::getTags
      object : GetTagsUseCase {
        override val getTags = getTags
      }.run {
        GetTagsCommand.runUseCase()
      }.let {
        ResponseEntity.ok(TagsResponse(it))
      }
    }
  }
}
