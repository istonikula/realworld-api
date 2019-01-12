package io.realworld

import arrow.effects.ForIO
import io.realworld.articles.UpdateDto
import io.realworld.domain.common.Auth
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.jdbc.JdbcTestUtils

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FavoriteTests {
  @LocalServerPort
  var port: Int = 0

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var articleRepo: ArticleRepository<ForIO>

  @Autowired
  lateinit var auth: Auth

  @Autowired
  lateinit var userRepo: UserRepository

  lateinit var spec: RequestSpecification
  lateinit var fixtures: FixtureFactory

  @BeforeEach
  fun init() {
    spec = initSpec()
    fixtures = FixtureFactory(auth)
  }

  fun initSpec() = RequestSpecBuilder()
    .setContentType(ContentType.JSON)
    .setBaseUri("http://localhost:$port")
    .addFilter(RequestLoggingFilter())
    .addFilter(ResponseLoggingFilter())
    .build()

  @AfterEach
  fun deleteUser() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `favorite article`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")
    val tarzan = createUser("tarzan")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()

    val tarzanClient = ApiClient(spec, tarzan.token)
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
    tarzanClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
    cheetaClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(2))
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(2))
  }

  @Test
  fun `favorite article, not found`() {
    val tarzan = createUser("tarzan")
    val tarzanClient = ApiClient(spec, tarzan.token)
    tarzanClient.post<Any>("/api/articles/non-existent/favorite")
      .then()
      .statusCode(404)
  }

  @Test
  fun `favorite article, author`() {
    val jane = createUser("jane")
    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()
    val janeClient = ApiClient(spec, jane.token)

    janeClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(0))
    janeClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(403)
    janeClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(0))
  }

  @Test
  fun `favoriting already favorited acticle has no effect`() {
    val jane = createUser("jane")
    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()

    val tarzan = createUser("tarzan")
    val tarzanClient = ApiClient(spec, tarzan.token)

    tarzanClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    tarzanClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
  }

  @Test
  fun `unfavorite article`() {
    val jane = createUser("jane")
    val tarzan = createUser("tarzan")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()

    val tarzanClient = ApiClient(spec, tarzan.token)
    tarzanClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    tarzanClient.delete("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(0))
    tarzanClient.get("/api/articles/${janesArticle.slug}")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(0))
  }

  @Test
  fun `unfavorite article, not found`() {
    val tarzan = createUser("tarzan")
    val tarzanClient = ApiClient(spec, tarzan.token)
    tarzanClient.delete("/api/articles/non-existent/favorite")
      .then()
      .statusCode(404)
  }

  @Test
  fun `article update response includes favorite info`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")
    val tarzan = createUser("tarzan")

    val cheetaClient = ApiClient(spec, cheeta.token)
    val janeClient = ApiClient(spec, jane.token)
    val tarzanClient = ApiClient(spec, tarzan.token)

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()
    val updateReq = UpdateRequest(UpdateDto(description = "updated.${janesArticle.description}"))

    janeClient.put("/api/articles/${janesArticle.slug}", updateReq)
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(0))

    cheetaClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(1))
    janeClient.put("/api/articles/${janesArticle.slug}", updateReq)
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(1))

    tarzanClient.post<Any>("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(true))
      .body("article.favoritesCount", equalTo(2))
    janeClient.put("/api/articles/${janesArticle.slug}", updateReq)
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(2))

    cheetaClient.delete("/api/articles/${janesArticle.slug}/favorite")
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(1))
    janeClient.put("/api/articles/${janesArticle.slug}", updateReq)
      .then()
      .statusCode(200)
      .body("article.favorited", equalTo(false))
      .body("article.favoritesCount", equalTo(1))
  }

  private fun createUser(username: String) =
    userRepo.create(fixtures.validTestUserRegistration(username, "$username@realworld.io")).unsafeRunSync()
}
