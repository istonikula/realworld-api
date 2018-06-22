package io.realworld

import io.realworld.articles.ArticleResponse
import io.realworld.articles.ArticleResponseDto
import io.realworld.articles.CreationDto
import io.realworld.domain.common.Auth
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.realworld.profiles.ProfileResponseDto
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
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
import java.time.Instant

data class CreationRequest(val article: CreationDto)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ArticleTests {
  @LocalServerPort
  var port: Int = 0

  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var auth: Auth

  @Autowired
  lateinit var userRepo: UserRepository

  lateinit var spec: RequestSpecification
  lateinit var fixtures: FixtureFactory
  lateinit var userAuthor: ValidUserRegistration

  @BeforeEach
  fun init() {
    spec = initSpec()
    fixtures = FixtureFactory(auth)
    userAuthor = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    userRepo.create(userAuthor).unsafeRunSync()
  }

  fun initSpec() = RequestSpecBuilder()
    .setContentType(ContentType.JSON)
    .setBaseUri("http://localhost:$port")
    .addFilter(RequestLoggingFilter())
    .addFilter(ResponseLoggingFilter())
    .build()

  @AfterEach
  fun deleteArticles() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `create article`() {
    val client = ApiClient(spec, userAuthor.token)

    val req = CreationRequest(CreationDto(
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "You have to believe",
      tagList = listOf("reactjs", "angularjs", "dragons")
    ))
    val expected = ArticleResponseDto(
      slug = "how-to-train-your-dragon",
      title = req.article.title,
      description = req.article.description,
      body = req.article.body,
      tagList = req.article.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = userAuthor.username,
        following = false
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val actual: ArticleResponse = client.post("/api/articles", req)
      .then()
      .statusCode(201)
      .toDto()
    assertThat(actual.article).isEqualToIgnoringGivenFields(expected, "createdAt", "updatedAt")
    assertThat(actual.article.createdAt).isNotNull()
    assertThat(actual.article.createdAt).isEqualTo(actual.article.updatedAt)
  }

  @Test
  fun `create article without tags`() {
    val client = ApiClient(spec, userAuthor.token)

    val req = CreationRequest(CreationDto(
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "You have to believe"
    ))

    val actual: ArticleResponse = client.post("/api/articles", req)
      .then()
      .statusCode(201)
      .toDto()
    assertThat(actual.article.tagList).isEmpty()

    val bodyJson = req.toObjectNode().apply {
      pathToObject("article").remove("tagList")
    }.toString()
    client.post("/api/articles", bodyJson)
      .then()
      .statusCode(201)
      .toDto<ArticleResponse>().apply {
        assertThat(actual.article.tagList).isEmpty()
      }
  }

  @Test
  fun `create requires title, description and body`() {
    val client = ApiClient(spec, userAuthor.token)

    val req = CreationRequest(CreationDto(
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "You have to believe"
    ))

    req.toObjectNode().apply {
      pathToObject("article").replace("title", textNode(""))
      pathToObject("article").replace("description", textNode(" "))
      pathToObject("article").replace("body", textNode("  "))
    }.toString().let {
      client.post("/api/articles", it)
        .then()
        .statusCode(422)
        .body("errors.title.message", Matchers.equalTo("must not be blank"))
        .body("errors.description.message", Matchers.equalTo("must not be blank"))
        .body("errors.body.message", Matchers.equalTo("must not be blank"))
    }

    listOf("title", "description", "body").map { prop ->
      req.toObjectNode().apply {
        pathToObject("article").remove(prop)
      }.toString().let {
        client.post("/api/articles", it)
          .then()
          .statusCode(422)
          .body("errors.$prop.type", Matchers.equalTo("TypeMismatch"))
      }
    }
  }

  @Test
  fun `create article requires auth`() {
    val client = ApiClient(spec)
    val req = CreationRequest(CreationDto(
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "You have to believe"
    ))
    client.post("/api/articles", req)
      .then()
      .statusCode(401)
  }
}
