package io.realworld

import io.realworld.articles.CommentDto
import io.realworld.domain.common.Auth
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
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

data class CommentRequest(val comment: CommentDto)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommentTests {
  @LocalServerPort
  var port: Int = 0

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var articleRepo: ArticleRepository

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
  fun `comment article`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)
    val req = CommentRequest(CommentDto("It takes a Jacobian"))

    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", req)
      .then()
      .statusCode(201)
  }

  @Test
  fun `comment article, not found`() {
    val cheeta = createUser("cheeta")
    val cheetaClient = ApiClient(spec, cheeta.token)
    val req = CommentRequest(CommentDto("It takes a Jacobian"))
    cheetaClient.post("/api/articles/non-existent/comments", req)
      .then()
      .statusCode(404)
  }

  @Test
  fun `comment article, author`() {
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()

    val janeClient = ApiClient(spec, jane.token)
    val req = CommentRequest(CommentDto("It takes a Jacobian"))

    janeClient.post("/api/articles/${janesArticle.slug}/comments", req)
      .then()
      .statusCode(201)
  }

  @Test
  fun `comment article multiple times`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)

    var req = CommentRequest(CommentDto("It takes a Jacobian"))
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", req)
      .then()
      .statusCode(201)

    req = CommentRequest(CommentDto("... or a chimpanzee"))
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", req)
      .then()
      .statusCode(201)
  }

  private fun createUser(username: String) =
    userRepo.create(fixtures.validTestUserRegistration(username, "$username@realworld.io")).unsafeRunSync()

}
