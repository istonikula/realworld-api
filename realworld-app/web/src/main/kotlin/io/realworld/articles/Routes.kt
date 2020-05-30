package io.realworld.articles

import io.realworld.JwtTokenResolver
import io.realworld.authHeader
import io.realworld.domain.articles.Article
import io.realworld.domain.articles.ArticleCommentDeleteError
import io.realworld.domain.articles.ArticleCommentError
import io.realworld.domain.articles.ArticleFilter
import io.realworld.domain.articles.ArticleUnfavoriteError
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
import io.realworld.errors.toRestException
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import io.realworld.runReadTx
import io.realworld.runWriteTx
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
import javax.validation.Valid

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
    val createUniqueSlugSrv = object : CreateUniqueSlugService {
      override val existsBySlug = articleRepo::existsBySlug
    }

    return object : CreateArticleUseCase {
      override val createUniqueSlug = createUniqueSlugSrv::slugify
      override val createArticle = articleRepo::create
    }.run {
      CreateArticleCommand(
        data = dto.toDomain(),
        user = user
      ).runUseCase()
    }.runWriteTx(txManager).let {
      ResponseEntity.status(HttpStatus.CREATED).body(ArticleResponse.fromDomain(it))
    }
  }

  @GetMapping("/api/articles")
  fun listArticles(
    filter: ArticleFilter,
    webRequest: NativeWebRequest
  ): ResponseEntity<ArticlesResponse> {

    val user = JwtTokenResolver(auth::parse)(
      webRequest.authHeader()
    ).toOption().flatMap {
      userRepo.findById(it.id).unsafeRunSync().map { it.user }
    }

    return object : GetArticlesUseCase {
      override val getArticles = articleRepo::getArticles
      override val getArticlesCount = articleRepo::getArticlesCount
    }.run {
      GetArticlesCommand(filter, user).runUseCase()
    }.runReadTx(txManager).let {
      ResponseEntity.ok(ArticlesResponse.fromDomain(it))
    }
  }

  @GetMapping("/api/articles/feed")
  fun articlesFeed(
    filter: FeedFilter,
    user: User
  ): ResponseEntity<ArticlesResponse> {
    return object : GetFeedsUseCase {
      override val getFeeds = articleRepo::getFeeds
      override val getFeedsCount = articleRepo::getFeedsCount
    }.run {
      GetFeedsCommand(filter, user).runUseCase()
    }.runReadTx(txManager).let {
      ResponseEntity.ok(ArticlesResponse.fromDomain(it))
    }
  }

  @GetMapping("/api/articles/{slug}")
  fun getBySlug(
    @PathVariable("slug") slug: String,
    webRequest: NativeWebRequest
  ): ResponseEntity<ArticleResponse> {

    val user = JwtTokenResolver(auth::parse)(
      webRequest.authHeader()
    ).toOption().flatMap {
      userRepo.findById(it.id).unsafeRunSync().map { it.user }
    }

    return object : GetArticleUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
    }.run {
      GetArticleCommand(slug, user).runUseCase()
    }.runReadTx(txManager).fold(
      { ResponseEntity.notFound().build() },
      { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
    )
  }

  @DeleteMapping("/api/articles/{slug}")
  fun deleteBySlug(
    @PathVariable("slug") slug: String,
    user: User
  ): ResponseEntity<Unit> {
    return object : DeleteArticleUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
      override val deleteArticle = articleRepo::deleteArticle
    }.run {
      DeleteArticleCommand(slug, user).runUseCase()
    }.runWriteTx(txManager).fold(
      { it.toRestException() },
      { ResponseEntity.noContent().build() }
    )
  }

  @PutMapping("/api/articles/{slug}")
  fun update(
    @PathVariable("slug") slug: String,
    @Valid @RequestBody update: UpdateDto,
    user: User
  ): ResponseEntity<ArticleResponse> {
    val createUniqueSlugSrv = object : CreateUniqueSlugService {
      override val existsBySlug = articleRepo::existsBySlug
    }

    val validateUpdateSrv = object : ValidateArticleUpdateService {
      override val createUniqueSlug = createUniqueSlugSrv::slugify
      override val getArticleBySlug = articleRepo::getBySlug
    }

    return object : UpdateArticleUseCase {
      override val validateUpdate: ValidateArticleUpdate = { x, y, z -> validateUpdateSrv.run { x.validate(y, z) } }
      override val updateArticle = articleRepo::updateArticle
    }.run {
      UpdateArticleCommand(update.toDomain(), slug, user).runUseCase()
    }.runWriteTx(txManager).fold(
      { it.toRestException() },
      { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
    )
  }

  @PostMapping("/api/articles/{slug}/favorite")
  fun favorite(
    @PathVariable("slug") slug: String,
    user: User
  ): ResponseEntity<ArticleResponse> {

    return object : FavoriteUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
      override val addFavorite = articleRepo::addFavorite
    }.run {
      FavoriteArticleCommand(slug, user).runUseCase()
    }.runWriteTx(txManager).fold(
      { it.toRestException() },
      { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
    )
  }

  @DeleteMapping("/api/articles/{slug}/favorite")
  fun unfavorite(
    @PathVariable("slug") slug: String,
    user: User
  ): ResponseEntity<ArticleResponse> {
    return object : UnfavoriteUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
      override val removeFavorite = articleRepo::removeFavorite
    }.run {
      UnfavoriteArticleCommand(slug, user).runUseCase()
    }.runWriteTx(txManager).fold(
      {
        when (it) {
          is ArticleUnfavoriteError.NotFound -> ResponseEntity.notFound().build()
        }
      },
      { ResponseEntity.ok(ArticleResponse.fromDomain(it)) }
    )
  }

  @GetMapping("/api/articles/{slug}/comments")
  fun getComments(
    @PathVariable("slug") slug: String,
    webRequest: NativeWebRequest
  ): ResponseEntity<CommentsResponse> {

    val user = JwtTokenResolver(auth::parse)(
      webRequest.authHeader()
    ).toOption().flatMap {
      userRepo.findById(it.id).unsafeRunSync().map { it.user }
    }

    return object : GetCommentsUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
      override val getComments = articleRepo::getComments
    }.run {
      GetCommentsCommand(slug, user).runUseCase()
    }.runReadTx(txManager).fold(
      { ResponseEntity.notFound().build() },
      { ResponseEntity.ok(CommentsResponse.fromDomain(it)) }
    )
  }

  @PostMapping("/api/articles/{slug}/comments")
  fun comment(
    @PathVariable("slug") slug: String,
    @Valid @RequestBody comment: CommentDto,
    user: User
  ): ResponseEntity<CommentResponse> {
    return object : CommentUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
      override val addComment = articleRepo::addComment
    }.run {
      CommentArticleCommand(slug, comment.body, user).runUsecase()
    }.runWriteTx(txManager).fold(
      {
        when (it) {
          is ArticleCommentError.NotFound -> ResponseEntity.notFound().build()
        }
      },
      { ResponseEntity.status(HttpStatus.CREATED).body(CommentResponse.fromDomain(it)) }
    )
  }

  @DeleteMapping("/api/articles/{slug}/comments/{id}")
  fun deleteComment(
    @PathVariable("slug") slug: String,
    @PathVariable("id") commentId: Long,
    user: User
  ): ResponseEntity<Void> {
    return object : DeleteCommentUseCase {
      override val getArticleBySlug = articleRepo::getBySlug
      override val deleteComment = articleRepo::deleteComment
      override val getComment = articleRepo::getComment
    }.run {
      DeleteCommentCommand(slug, commentId.articleScopedCommentId(), user).runUseCase()
    }.runWriteTx(txManager).fold(
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

  @GetMapping("/api/tags")
  fun getTags(): ResponseEntity<TagsResponse> {
    return object : GetTagsUseCase {
      override val getTags = articleRepo::getTags
    }.run {
      GetTagsCommand.runUseCase()
    }.runReadTx(txManager).let {
      ResponseEntity.ok(TagsResponse(it))
    }
  }
}
