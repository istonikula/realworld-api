package io.realworld

import io.realworld.articles.ArticleResponse
import io.realworld.articles.ArticleResponseDto
import io.realworld.articles.CreationDto
import io.realworld.articles.UpdateDto
import io.realworld.domain.articles.slugify
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
data class UpdateRequest(val article: UpdateDto)

object TestUsers {
  object Author {
    val email = "foo@realworld.io"
    val username = "foo"
  }

  object NonAuthor {
    val email = "bar@realworld.io"
    val username = "bar"
  }
}

object TestArticles {
  object Dragon {
    val creation = CreationDto(
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "You have to believe",
      tagList = listOf("reactjs", "angularjs", "dragons")
    )

    val response = ArticleResponseDto(
      slug = "how-to-train-your-dragon",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = TestUsers.Author.username,
        following = false
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  }
}

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

    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response

    client.post("/api/articles", req)
      .then()
      .statusCode(201)
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected, "createdAt", "updatedAt")
        assertThat(article.createdAt).isNotNull()
        assertThat(article.createdAt).isEqualTo(article.updatedAt)
      }

    client.get("/api/articles/${expected.title.slugify()}")
      .then()
      .statusCode(200)
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected, "createdAt", "updatedAt")
      }
  }

  @Test
  fun `create article without tags`() {
    val client = ApiClient(spec, userAuthor.token)

    val req = CreationRequest(TestArticles.Dragon.creation.copy(tagList = listOf()))

    client.post("/api/articles", req)
      .then()
      .statusCode(201)
      .toDto<ArticleResponse>().apply {
        assertThat(article.tagList).isEmpty()
      }

    val bodyJson = req.toObjectNode().apply {
      pathToObject("article").remove("tagList")
    }.toString()
    client.post("/api/articles", bodyJson)
      .then()
      .statusCode(201)
      .toDto<ArticleResponse>().apply {
        assertThat(article.tagList).isEmpty()
      }
  }

  @Test
  fun `creation of multiple articles with same title results in unique slugs`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)

    val slug1 = client.post("/api/articles", req).then().toDto<ArticleResponse>().article.slug
    val slug2 = client.post("/api/articles", req).then().toDto<ArticleResponse>().article.slug
    val slug3 = client.post("/api/articles", req).then().toDto<ArticleResponse>().article.slug

    assertThat(slug1).isEqualTo("how-to-train-your-dragon")
    assertThat(slug2).contains("how-to-train-your-dragon-")
    assertThat(slug3).contains("how-to-train-your-dragon-")
    assertThat(slug2).isNotEqualTo(slug3)
  }

  @Test
  fun `create requires title, description and body`() {
    val client = ApiClient(spec, userAuthor.token)

    val req = CreationRequest(TestArticles.Dragon.creation)

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
    val req = CreationRequest(TestArticles.Dragon.creation)
    client.post("/api/articles", req)
      .then()
      .statusCode(401)
  }

  @Test
  fun `get by slug`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response
    val slug = client.post("/api/articles", req).then().toDto<ArticleResponse>().article.slug

    client.get("/api/articles/$slug", token = null)
      .then()
      .toDto<ArticleResponse>().apply {
      assertThat(article).isEqualToIgnoringGivenFields(expected,
        "author", "createdAt", "updatedAt")
      assertThat(article.author).isEqualToIgnoringGivenFields(expected.author, "following")
      assertThat(article.author.following).isNull()
    }

    val notAuthor = with(TestUsers.NonAuthor) { fixtures.validTestUserRegistration(username, email) }
    userRepo.create(notAuthor).unsafeRunSync()
    client.get("/api/articles/$slug", token = notAuthor.token)
      .then()
      .toDto<ArticleResponse>().apply {
      assertThat(article).isEqualToIgnoringGivenFields(expected,
        "author", "createdAt", "updatedAt")
      assertThat(article.author).isEqualToIgnoringGivenFields(expected.author, "following")
      assertThat(article.author.following).isFalse()
    }

    userRepo.addFollower(userAuthor.username, notAuthor.username).unsafeRunSync()
    client.get("/api/articles/$slug", token = notAuthor.token)
      .then()
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected,
          "author", "createdAt", "updatedAt")
        assertThat(article.author).isEqualToIgnoringGivenFields(expected.author, "following")
        assertThat(article.author.following).isTrue()
      }
  }

  @Test
  fun `get by slug, not found`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    client.post("/api/articles", req).then().statusCode(201)

    client.get("/api/articles/not-found").then().statusCode(404)
  }

  @Test
  fun `delete by slug`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    client.delete("/api/articles/$slug").then().statusCode(204)
    client.get("/api/articles/$slug").then().statusCode(404)
  }

  @Test
  fun `delete by slug, not found`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    client.post("/api/articles", req).then().statusCode(201)

    client.delete("/api/articles/not-found").then().statusCode(404)
  }

  @Test
  fun `delete by slug, not author`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val notAuthor = with(TestUsers.NonAuthor) { fixtures.validTestUserRegistration(username, email) }
    userRepo.create(notAuthor).unsafeRunSync()

    client.delete("/api/articles/$slug", notAuthor.token).then().statusCode(403)
  }

  @Test
  fun `delete by slug requires auth`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    client.delete("/api/articles/$slug", token = null).then().statusCode(401)
    client.get("/api/articles/$slug").then().statusCode(200)
  }

  @Test
  fun `update article title`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val updateReq = UpdateRequest(UpdateDto(title = "updated.${req.article.title}"))
    client.put("/api/articles/$slug", updateReq)
      .then()
      .statusCode(200)
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected,
          "slug",
          "title",
          "createdAt",
          "updatedAt"
        )
        assertThat(article.slug).isEqualTo("updated-${expected.slug}")
        assertThat(article.title).isEqualTo("updated.${expected.title}")
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article description`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val updateReq = UpdateRequest(UpdateDto(description = "updated.${req.article.description}"))
    client.put("/api/articles/$slug", updateReq)
      .then()
      .statusCode(200)
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected,
          "description",
          "createdAt",
          "updatedAt"
        )
        assertThat(article.description).isEqualTo("updated.${expected.description}")
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article body`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val updateReq = UpdateRequest(UpdateDto(body = "updated.${req.article.body}"))
    client.put("/api/articles/$slug", updateReq)
      .then()
      .statusCode(200)
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected,
          "body",
          "createdAt",
          "updatedAt"
        )
        assertThat(article.body).isEqualTo("updated.${expected.body}")
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article title, description and body`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val updateReq = UpdateRequest(UpdateDto(
      title = "updated.${req.article.title}",
      description = "updated.${req.article.description}",
      body = "updated.${req.article.body}"
    ))
    client.put("/api/articles/$slug", updateReq)
      .then()
      .statusCode(200)
      .toDto<ArticleResponse>().apply {
        assertThat(article).isEqualToIgnoringGivenFields(expected,
          "slug",
          "title",
          "description",
          "body",
          "createdAt",
          "updatedAt"
        )
        assertThat(article.slug).isEqualTo("updated-${expected.slug}")
        assertThat(article.title).isEqualTo("updated.${expected.title}")
        assertThat(article.description).isEqualTo("updated.${expected.description}")
        assertThat(article.body).isEqualTo("updated.${expected.body}")
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article, not found`() {
    val client = ApiClient(spec, userAuthor.token)
    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    client.put("/api/articles/not-found", updateReq)
      .then()
      .statusCode(404)
  }

  @Test
  fun `update article, not author`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val notAuthor = with(TestUsers.NonAuthor) { fixtures.validTestUserRegistration(username, email) }
    userRepo.create(notAuthor).unsafeRunSync()

    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    client.put("/api/articles/$slug", updateReq, notAuthor.token).then().statusCode(403)
  }

  @Test
  fun `update article requires auth`() {
    val client = ApiClient(spec, userAuthor.token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response.slug
    client.post("/api/articles", req).then().statusCode(201)

    val updateReq = UpdateRequest(UpdateDto(description = "updated.${req.article.description}"))
    client.put("/api/articles/$slug", updateReq, null)
      .then()
      .statusCode(401)
  }
}
