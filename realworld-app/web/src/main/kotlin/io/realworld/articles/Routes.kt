package io.realworld.articles

import io.realworld.JwtTokenResolver
import io.realworld.authHeader
import io.realworld.domain.articles.Article
import io.realworld.domain.articles.CreateArticleCommand
import io.realworld.domain.articles.CreateArticleUseCase
import io.realworld.domain.articles.CreateUniqueSlugService
import io.realworld.domain.articles.GetArticleCommand
import io.realworld.domain.articles.GetArticleUseCase
import io.realworld.domain.common.Auth
import io.realworld.domain.users.User
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import io.realworld.runReadTx
import io.realworld.runWriteTx
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.NativeWebRequest
import javax.validation.Valid

data class ArticleResponse(val article: ArticleResponseDto) {
  companion object {
    fun fromDomain(domain: Article) = ArticleResponse(ArticleResponseDto.fromDomain(domain))
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
}
