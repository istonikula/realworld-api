package io.realworld.articles

import io.realworld.domain.articles.Article
import io.realworld.domain.articles.CreateArticleCommand
import io.realworld.domain.articles.CreateArticleUseCase
import io.realworld.domain.articles.CreateUniqueSlugService
import io.realworld.domain.users.User
import io.realworld.persistence.ArticleRepository
import io.realworld.runWriteTx
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

data class ArticleResponse(val article: ArticleResponseDto) {
  companion object {
    fun fromDomain(domain: Article) = ArticleResponse(ArticleResponseDto.fromDomain(domain))
  }
}

@RestController
class ArticleController(
  private val repo: ArticleRepository,
  private val txManager: PlatformTransactionManager
) {
  @PostMapping("/api/articles")
  fun create(@Valid @RequestBody dto: CreationDto, user: User): ResponseEntity<ArticleResponse> {
    val createUniqueSlugSrv = object : CreateUniqueSlugService {
      override val existsBySlug = repo::existsBySlug
    }

    return object : CreateArticleUseCase {
      override val createUniqueSlug = createUniqueSlugSrv::slufigy
      override val createArticle = repo::create
    }.run {
      CreateArticleCommand(
        data = dto.toDomain(),
        user = user
      ).runUseCase()
    }.runWriteTx(txManager).let {
      ResponseEntity.status(HttpStatus.CREATED).body(ArticleResponse.fromDomain(it))
    }
  }
}
