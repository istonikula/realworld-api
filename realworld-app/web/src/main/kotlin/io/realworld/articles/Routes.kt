package io.realworld.articles

import io.realworld.ForbiddenException
import io.realworld.JwtTokenResolver
import io.realworld.authHeader
import io.realworld.domain.articles.Article
import io.realworld.domain.articles.ArticleCommentError
import io.realworld.domain.articles.ArticleDeleteError
import io.realworld.domain.articles.ArticleFavoriteError
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
import io.realworld.domain.articles.FavoriteArticleCommand
import io.realworld.domain.articles.FavoriteUseCase
import io.realworld.domain.articles.GetArticleCommand
import io.realworld.domain.articles.GetArticleUseCase
import io.realworld.domain.articles.UnfavoriteArticleCommand
import io.realworld.domain.articles.UnfavoriteUseCase
import io.realworld.domain.articles.UpdateArticleCommand
import io.realworld.domain.articles.UpdateArticleUseCase
import io.realworld.domain.articles.ValidateArticleUpdate
import io.realworld.domain.articles.ValidateArticleUpdateService
import io.realworld.domain.common.Auth
import io.realworld.domain.users.User
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
      override val createUniqueSlug = createUniqueSlugSrv::slufigy
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
      {
        when (it) {
          is ArticleDeleteError.NotAuthor -> throw ForbiddenException()
          is ArticleDeleteError.NotFound -> ResponseEntity.notFound().build()
        }
      },
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
      override val createUniqueSlug = createUniqueSlugSrv::slufigy
      override val getArticleBySlug = articleRepo::getBySlug
    }

    return object : UpdateArticleUseCase {
      override val validateUpdate: ValidateArticleUpdate = { x, y, z -> validateUpdateSrv.run { x.validate(y, z) } }
      override val updateArticle = articleRepo::updateArticle
    }.run {
      UpdateArticleCommand(update.toDomain(), slug, user).runUseCase()
    }.runWriteTx(txManager).fold(
      {
        when (it) {
          is ArticleUpdateError.NotAuthor -> throw ForbiddenException()
          is ArticleUpdateError.NotFound -> ResponseEntity.notFound().build()
        }
      },
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
      {
        when (it) {
          is ArticleFavoriteError.Author -> throw ForbiddenException()
          is ArticleFavoriteError.NotFound -> ResponseEntity.notFound().build()
        }
      },
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
}
