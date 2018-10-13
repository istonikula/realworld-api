package io.realworld

import io.realworld.articles.ArticleResponse
import io.realworld.articles.ArticleResponseDto
import io.realworld.articles.ArticlesResponse
import io.realworld.articles.CreationDto
import io.realworld.articles.UpdateDto
import io.realworld.domain.articles.slugify
import io.realworld.domain.common.Auth
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

interface TestUser {
  val email: String
  val username: String
}

object TestUsers {
  object Jane : TestUser {
    override val username = "jane"
    override val email = "$username@realworld.io"
  }

  object Cheeta : TestUser {
    override val username = "cheeta"
    override val email = "$username@realworld.io"
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

    fun response(user: TestUser) = ArticleResponseDto(
      slug = "how-to-train-your-dragon",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = user.username,
        following = false
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  }

  object Angular {
    val creation = CreationDto(
      title = "Angular 101",
      description = "Learn Angular in 101 days",
      body = "Ever worder how?",
      tagList = listOf("angularjs", "101")
    )

    fun response(user: TestUser) = ArticleResponseDto(
      slug = "angular-101",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = user.username,
        following = false
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  }

  object React {
    val creation = CreationDto(
      title = "React 101",
      description = "Learn React in 101 hours",
      body = "Or maybe switch to Elm?",
      tagList = listOf("reactjs", "101")
    )

    fun response(user: TestUser) = ArticleResponseDto(
      slug = "react-101",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = user.username,
        following = false
      ),
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )
  }

  object Elm {
    val creation = CreationDto(
      title = "Elm",
      description = "A delightful language for reliable webapps.",
      body = "Generate JavaScript with great performance and no runtime exceptions.",
      tagList = listOf("Elm", "NoExceptions")
    )

    fun response(user: TestUser) = ArticleResponseDto(
      slug = "elm",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = user.username,
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
  fun deleteArticles() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `create article`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)

    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane)

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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)

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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)

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
    val janeClient = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane)
    val slug = janeClient.post("/api/articles", req).then().toDto<ArticleResponse>().article.slug

    janeClient.get("/api/articles/$slug", token = null)
      .then()
      .toDto<ArticleResponse>().apply {
      assertThat(article).isEqualToIgnoringGivenFields(expected,
        "author", "createdAt", "updatedAt")
      assertThat(article.author).isEqualToIgnoringGivenFields(expected.author, "following")
      assertThat(article.author.following).isNull()
    }

    val cheetaClient = ApiClient(spec, createUser(TestUsers.Cheeta).token)
    cheetaClient.get("/api/articles/$slug")
      .then()
      .toDto<ArticleResponse>().apply {
      assertThat(article).isEqualToIgnoringGivenFields(expected,
        "author", "createdAt", "updatedAt")
      assertThat(article.author).isEqualToIgnoringGivenFields(expected.author, "following")
      assertThat(article.author.following).isFalse()
    }

    cheetaClient.post<Any>("/api/profiles/${TestUsers.Jane.username}/follow")
    cheetaClient.get("/api/articles/$slug")
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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    client.post("/api/articles", req).then().statusCode(201)

    client.get("/api/articles/not-found").then().statusCode(404)
  }

  @Test
  fun `delete by slug`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response(TestUsers.Jane).slug
    client.post("/api/articles", req).then().statusCode(201)

    client.delete("/api/articles/$slug").then().statusCode(204)
    client.get("/api/articles/$slug").then().statusCode(404)
  }

  @Test
  fun `delete by slug, not found`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    client.post("/api/articles", req).then().statusCode(201)

    client.delete("/api/articles/not-found").then().statusCode(404)
  }

  @Test
  fun `delete by slug, not author`() {
    val janeClient = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response(TestUsers.Jane).slug
    janeClient.post("/api/articles", req).then().statusCode(201)

    val cheetaClient = ApiClient(spec, createUser(TestUsers.Cheeta).token)
    cheetaClient.delete("/api/articles/$slug").then().statusCode(403)
  }

  @Test
  fun `delete by slug requires auth`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response(TestUsers.Jane).slug
    client.post("/api/articles", req).then().statusCode(201)

    client.delete("/api/articles/$slug", token = null).then().statusCode(401)
    client.get("/api/articles/$slug").then().statusCode(200)
  }

  @Test
  fun `update article title`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane)
    val slug = expected.slug
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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane)
    val slug = expected.slug
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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane)
    val slug = expected.slug
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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane)
    val slug = expected.slug
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
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    client.put("/api/articles/not-found", updateReq)
      .then()
      .statusCode(404)
  }

  @Test
  fun `update article, not author`() {
    val janeClient = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response(TestUsers.Jane).slug
    janeClient.post("/api/articles", req).then().statusCode(201)

    val cheetaClient = ApiClient(spec, createUser(TestUsers.Cheeta).token)
    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    cheetaClient.put("/api/articles/$slug", updateReq).then().statusCode(403)
  }

  @Test
  fun `update article requires auth`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)
    val req = CreationRequest(TestArticles.Dragon.creation)
    val slug = TestArticles.Dragon.response(TestUsers.Jane).slug
    client.post("/api/articles", req).then().statusCode(201)

    val updateReq = UpdateRequest(UpdateDto(description = "updated.${req.article.description}"))
    client.put("/api/articles/$slug", updateReq, null)
      .then()
      .statusCode(401)
  }

  @Test
  fun `list articles`() {
    val cheeta = createUser(TestUsers.Cheeta)
    val jane = createUser(TestUsers.Jane)

    val cheetaClient = ApiClient(spec, cheeta.token)
    val janeClient = ApiClient(spec, jane.token)

    val dragonReq = CreationRequest(TestArticles.Dragon.creation)
    val dragonExpected = TestArticles.Dragon.response(TestUsers.Jane)
    val dragonSlug = janeClient.post("/api/articles", dragonReq).then().toDto<ArticleResponse>().article.slug

    val reactReq = CreationRequest(TestArticles.React.creation)
    val reactExpected = TestArticles.React.response(TestUsers.Jane)
    val reactSlug = janeClient.post("/api/articles", reactReq).then().toDto<ArticleResponse>().article.slug

    val angularReq = CreationRequest(TestArticles.Angular.creation)
    val angularExpected = TestArticles.Angular.response(TestUsers.Jane)
    val angularSlug = janeClient.post("/api/articles", angularReq).then().toDto<ArticleResponse>().article.slug

    val elmReq = CreationRequest(TestArticles.Elm.creation)
    val elmExpected = TestArticles.Elm.response(TestUsers.Jane)
    val elmSlug = janeClient.post("/api/articles", elmReq).then().toDto<ArticleResponse>().article.slug

    cheetaClient.get("/api/articles", null)
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(elmExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(elmExpected.author.copy(following = null))

        assertThat(articles[1]).isEqualToIgnoringGivenFields(angularExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[1].author).isEqualTo(angularExpected.author.copy(following = null))

        assertThat(articles[2]).isEqualToIgnoringGivenFields(reactExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[2].author).isEqualTo(reactExpected.author.copy(following = null))

        assertThat(articles[3]).isEqualToIgnoringGivenFields(dragonExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[3].author).isEqualTo(dragonExpected.author.copy(following = null))
      }

    cheetaClient.get("/api/articles")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(elmExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(elmExpected.author.copy(following = false))

        assertThat(articles[1]).isEqualToIgnoringGivenFields(angularExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[1].author).isEqualTo(angularExpected.author.copy(following = false))

        assertThat(articles[2]).isEqualToIgnoringGivenFields(reactExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[2].author).isEqualTo(reactExpected.author.copy(following = false))

        assertThat(articles[3]).isEqualToIgnoringGivenFields(dragonExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[3].author).isEqualTo(dragonExpected.author.copy(following = false))
      }

    cheetaClient.post<Any>("/api/articles/${dragonSlug}/favorite").then().statusCode(200)
    cheetaClient.post<Any>("/api/profiles/${jane.username}/follow").then().statusCode(200)

    cheetaClient.get("/api/articles")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(elmExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(elmExpected.author.copy(following = true))

        assertThat(articles[1]).isEqualToIgnoringGivenFields(angularExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[1].author).isEqualTo(angularExpected.author.copy(following = true))

        assertThat(articles[2]).isEqualToIgnoringGivenFields(reactExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[2].author).isEqualTo(reactExpected.author.copy(following = true))

        assertThat(articles[3]).isEqualToIgnoringGivenFields(dragonExpected.copy(favorited = true, favoritesCount = 1L),
          "author", "createdAt", "updatedAt")
        assertThat(articles[3].author).isEqualTo(dragonExpected.author.copy(following = true))
      }

    // by author
    cheetaClient.get("/api/articles?author=${TestUsers.Cheeta.username}")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }

    cheetaClient.get("/api/articles?author=${TestUsers.Jane.username}")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(elmExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(elmExpected.author.copy(following = true))

        assertThat(articles[1]).isEqualToIgnoringGivenFields(angularExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[1].author).isEqualTo(angularExpected.author.copy(following = true))

        assertThat(articles[2]).isEqualToIgnoringGivenFields(reactExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[2].author).isEqualTo(reactExpected.author.copy(following = true))

        assertThat(articles[3]).isEqualToIgnoringGivenFields(dragonExpected.copy(favorited = true, favoritesCount = 1L),
          "author", "createdAt", "updatedAt")
        assertThat(articles[3].author).isEqualTo(dragonExpected.author.copy(following = true))
      }

    // by tag
    cheetaClient.get("/api/articles?tag=foo")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }

    cheetaClient.get("/api/articles?tag=dragons")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(dragonExpected.copy(favorited = true, favoritesCount = 1L),
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(dragonExpected.author.copy(following = true))
      }

    cheetaClient.get("/api/articles?tag=reactjs")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(2))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(reactExpected,
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(reactExpected.author.copy(following = true))

        assertThat(articles[1]).isEqualToIgnoringGivenFields(dragonExpected.copy(favorited = true, favoritesCount = 1L),
          "author", "createdAt", "updatedAt")
        assertThat(articles[1].author).isEqualTo(dragonExpected.author.copy(following = true))
      }

    // by author and tag
    cheetaClient.get("/api/articles?author=${TestUsers.Jane.username}&tag=dragons")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(dragonExpected.copy(favorited = true, favoritesCount = 1L),
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(dragonExpected.author.copy(following = true))
      }

    // by author, tag and favorited
    cheetaClient.get("/api/articles?author=${TestUsers.Jane.username}&tag=dragons&favorited=${TestUsers.Cheeta.username}")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        assertThat(articles[0]).isEqualToIgnoringGivenFields(dragonExpected.copy(favorited = true, favoritesCount = 1L),
          "author", "createdAt", "updatedAt")
        assertThat(articles[0].author).isEqualTo(dragonExpected.author.copy(following = true))
      }
    cheetaClient.delete("/api/articles/${dragonSlug}/favorite").then().statusCode(200)
    cheetaClient.get("/api/articles?author=${TestUsers.Jane.username}&tag=dragons&favorited=${TestUsers.Cheeta.username}")
      .then()
      .statusCode(200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }
  }

  private fun createUser(user: TestUser) =
    userRepo.create(fixtures.validTestUserRegistration(user.username, user.email)).unsafeRunSync()
}

