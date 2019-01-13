package io.realworld

import arrow.effects.ForIO
import arrow.effects.fix
import io.realworld.articles.CommentDto
import io.realworld.articles.CommentResponse
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

data class CommentRequest(val comment: CommentDto)

object TestComments {
  object Jacobian {
    val req = CommentRequest(CommentDto("It takes a Jacobian"))
  }
}

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CommentTests {
  @LocalServerPort
  var port: Int = 0

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var articleRepo: ArticleRepository<ForIO>

  @Autowired
  lateinit var auth: Auth

  @Autowired
  lateinit var userRepo: UserRepository<ForIO>

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

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)
  }

  @Test
  fun `comment article, not found`() {
    val cheeta = createUser("cheeta")
    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.post("/api/articles/non-existent/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(404)
  }

  @Test
  fun `comment article, author`() {
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val janeClient = ApiClient(spec, jane.token)
    janeClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)
  }

  @Test
  fun `comment article multiple times`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)

    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)

    val req = CommentRequest(CommentDto("... or a chimpanzee"))
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", req)
      .then()
      .statusCode(201)
  }

  @Test
  fun `comment article requires auth`() {
    val jane = createUser("jane")
    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    ApiClient(spec).post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(401)
  }

  @Test
  fun `delete comment`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)

    val commentId = cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)
      .toDto<CommentResponse>()
      .comment.id

    cheetaClient.delete("/api/articles/${janesArticle.slug}/comments/$commentId")
      .then()
      .statusCode(204)
  }

  @Test
  fun `delete comment, article not found`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)

    val commentId = cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)
      .toDto<CommentResponse>()
      .comment.id

    cheetaClient.delete("/api/articles/not-found/comments/$commentId")
      .then()
      .statusCode(404)
  }

  @Test
  fun `delete comment, comment not found`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.delete("/api/articles/${janesArticle.slug}/comments/${Long.MAX_VALUE}")
      .then()
      .statusCode(404)
  }

  @Test
  fun `delete comment, not comment author`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val janeClient = ApiClient(spec, jane.token)
    val cheetaClient = ApiClient(spec, cheeta.token)

    val commentId = cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)
      .toDto<CommentResponse>()
      .comment.id

    janeClient.delete("/api/articles/${janesArticle.slug}/comments/$commentId")
      .then()
      .statusCode(403)
  }

  // TODO should article author be allowed to delete any comment?

  @Test
  fun `get comments`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")
    val tarzan = createUser("tarzan")

    val janesArticle = articleRepo.create(fixtures.validTestArticleCreation(), jane).fix().unsafeRunSync()

    val tarzanClient = ApiClient(spec, tarzan.token)
    tarzanClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .statusCode(200)
      .body("comments.isEmpty()", equalTo(true))

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(201)

    tarzanClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .statusCode(200)
      .body("comments[0].author.username", equalTo("cheeta"))
      .body("comments[0].author.following", equalTo(false))

    tarzanClient.post<Any>("/api/profiles/cheeta/follow")
      .then()
      .statusCode(200)

    tarzanClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .statusCode(200)
      .body("comments[0].author.username", equalTo("cheeta"))
      .body("comments[0].author.following", equalTo(true))

    tarzanClient.get("/api/articles/${janesArticle.slug}/comments", null)
      .then()
      .statusCode(200)
      .body("comments[0].author.username", equalTo("cheeta"))
      .body("comments[0].author.following", equalTo(null))

    tarzanClient.get("/api/articles/not-found/comments")
      .then()
      .statusCode(404)
  }

  private fun createUser(username: String) =
    userRepo.create(fixtures.validTestUserRegistration(username, "$username@realworld.io")).fix().unsafeRunSync()
}
