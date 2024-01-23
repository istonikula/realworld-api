package io.realworld

import io.realworld.articles.CommentDto
import io.realworld.articles.CommentResponse
import io.realworld.articles.CommentsResponse
import io.realworld.domain.common.Auth
import io.realworld.persistence.ArticleRepository
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.restassured.specification.RequestSpecification
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
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
  lateinit var articleRepo: ArticleRepository

  @Autowired
  lateinit var auth: Auth

  @Autowired
  lateinit var userRepo: UserRepository

  lateinit var spec: RequestSpecification
  lateinit var fixtures: FixtureFactory

  @BeforeEach
  fun init() {
    spec = initSpec(port).build()
    fixtures = FixtureFactory(auth)
  }

  @AfterEach
  fun deleteUser() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `comment article`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .verifyResponse(Schemas.comment, 201)
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

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val janeClient = ApiClient(spec, jane.token)
    janeClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(1L)
      }
  }

  @Test
  fun `comment article multiple times`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val cheetaClient = ApiClient(spec, cheeta.token)

    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(1L)
      }

    val req = CommentRequest(CommentDto("... or a chimpanzee"))
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", req)
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(2L)
      }
  }

  @Test
  fun `comment article requires auth`() {
    val jane = createUser("jane")
    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    ApiClient(spec).post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .statusCode(401)
  }

  @Test
  fun `delete comment`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }
    val cheetasArticle = runBlocking {
      articleRepo.create(
        fixtures.validTestArticleCreation().copy(slug = "banana-split", description = "Banana Split"),
        cheeta
      )
    }

    val cheetaClient = ApiClient(spec, cheeta.token)

    cheetaClient.post(
      "/api/articles/${janesArticle.slug}/comments",
      CommentRequest(CommentDto("comment 1"))
    )
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(1L)
      }

    cheetaClient.post(
      "/api/articles/${janesArticle.slug}/comments",
      CommentRequest(CommentDto("comment 2"))
    )
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(2L)
      }

    cheetaClient.post(
      "/api/articles/${janesArticle.slug}/comments",
      CommentRequest(CommentDto("comment 3"))
    )
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(3L)
      }

    cheetaClient.post(
      "/api/articles/${cheetasArticle.slug}/comments",
      CommentRequest(CommentDto("I'm the author"))
    )
      .then()
      .verifyResponse(Schemas.comment, 201)
      .toDto<CommentResponse>().comment.apply {
        assertThat(id).isEqualTo(1L)
      }

    cheetaClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .toDto<CommentsResponse>().apply {
        assertThat(comments)
          .extracting("id", "body")
          .contains(
            tuple(1L, "comment 1"),
            tuple(2L, "comment 2"),
            tuple(3L, "comment 3")
          )
      }

    cheetaClient.get("/api/articles/${cheetasArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .toDto<CommentsResponse>().apply {
        assertThat(comments)
          .extracting("id", "body")
          .contains(
            tuple(1L, "I'm the author")
          )
      }

    cheetaClient.delete("/api/articles/${janesArticle.slug}/comments/2")
      .then()
      .statusCode(204)

    cheetaClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .toDto<CommentsResponse>().apply {
        assertThat(comments)
          .extracting("id", "body")
          .contains(
            tuple(1L, "comment 1"),
            tuple(3L, "comment 3")
          )
      }

    cheetaClient.get("/api/articles/${cheetasArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .toDto<CommentsResponse>().apply {
        assertThat(comments)
          .extracting("id", "body")
          .contains(
            tuple(1L, "I'm the author")
          )
      }
  }

  @Test
  fun `delete comment, article not found`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val cheetaClient = ApiClient(spec, cheeta.token)

    val commentId = cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .verifyResponse(Schemas.comment, 201)
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

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.delete("/api/articles/${janesArticle.slug}/comments/${Long.MAX_VALUE}")
      .then()
      .statusCode(404)
  }

  @Test
  fun `delete comment, not comment author`() {
    val cheeta = createUser("cheeta")
    val jane = createUser("jane")

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val janeClient = ApiClient(spec, jane.token)
    val cheetaClient = ApiClient(spec, cheeta.token)

    val commentId = cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .verifyResponse(Schemas.comment, 201)
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

    val janesArticle = runBlocking { articleRepo.create(fixtures.validTestArticleCreation(), jane) }

    val tarzanClient = ApiClient(spec, tarzan.token)
    tarzanClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .body("comments.isEmpty()", equalTo(true))

    val cheetaClient = ApiClient(spec, cheeta.token)
    cheetaClient.post("/api/articles/${janesArticle.slug}/comments", TestComments.Jacobian.req)
      .then()
      .verifyResponse(Schemas.comment, 201)

    tarzanClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .body("comments[0].author.username", equalTo("cheeta"))
      .body("comments[0].author.following", equalTo(false))

    tarzanClient.post<Any>("/api/profiles/cheeta/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    tarzanClient.get("/api/articles/${janesArticle.slug}/comments")
      .then()
      .verifyResponse(Schemas.comments, 200)
      .body("comments[0].author.username", equalTo("cheeta"))
      .body("comments[0].author.following", equalTo(true))

    tarzanClient.get("/api/articles/${janesArticle.slug}/comments", null)
      .then()
      .verifyResponse(Schemas.comments, 200)
      .body("comments[0].author.username", equalTo("cheeta"))
      .body("comments[0].author.following", equalTo(null))

    tarzanClient.get("/api/articles/not-found/comments")
      .then()
      .statusCode(404)
  }

  private fun createUser(username: String) =
    runBlocking { userRepo.create(fixtures.validTestUserRegistration(username, "$username@realworld.io")) }
}
