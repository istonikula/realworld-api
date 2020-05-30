package io.realworld

import io.realworld.articles.ArticleResponse
import io.realworld.articles.ArticleResponseDto
import io.realworld.articles.ArticlesResponse
import io.realworld.articles.CreationDto
import io.realworld.articles.TagsResponse
import io.realworld.articles.UpdateDto
import io.realworld.domain.articles.slugify
import io.realworld.domain.common.Auth
import io.realworld.persistence.TagTbl
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.realworld.profiles.ProfileResponseDto
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

  object Tarzan : TestUser {
    override val username = "tarzan"
    override val email = "$username@realworld.io"
  }
}

data class ArticleSpec(
  val req: CreationDto,
  val resp: ArticleResponseDto
)
fun dragonSpec(author: String) = ArticleSpec(TestArticles.Dragon.creation, TestArticles.Dragon.response(author))
fun angularSpec(author: String) = ArticleSpec(TestArticles.Angular.creation, TestArticles.Angular.response(author))
fun elmSpec(author: String) = ArticleSpec(TestArticles.Elm.creation, TestArticles.Elm.response(author))
fun reactSpec(author: String) = ArticleSpec(TestArticles.React.creation, TestArticles.React.response(author))
object TestArticles {
  object Dragon {
    val creation = CreationDto(
      title = "How to train your dragon",
      description = "Ever wonder how?",
      body = "You have to believe",
      tagList = listOf("reactjs", "angularjs", "dragons")
    )

    fun response(author: String) = ArticleResponseDto(
      slug = "how-to-train-your-dragon",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = author,
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

    fun response(username: String) = ArticleResponseDto(
      slug = "angular-101",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = username,
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

    fun response(username: String) = ArticleResponseDto(
      slug = "react-101",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = username,
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

    fun response(username: String) = ArticleResponseDto(
      slug = "elm",
      title = creation.title,
      description = creation.description,
      body = creation.body,
      tagList = creation.tagList,
      favorited = false,
      favoritesCount = 0,
      author = ProfileResponseDto(
        username = username,
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
    spec = initSpec(port).build()
    fixtures = FixtureFactory(auth)
  }

  @AfterEach
  fun deleteArticles() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `create article`() {
    val client = ApiClient(spec, createUser(TestUsers.Jane).token)

    val req = CreationRequest(TestArticles.Dragon.creation)
    val expected = TestArticles.Dragon.response(TestUsers.Jane.username)

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
      .verifyResponse(Schemas.article, 200)
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
      .verifyResponse(Schemas.article, 201)
      .toDto<ArticleResponse>().apply {
        assertThat(article.tagList).isEmpty()
      }

    val bodyJson = req.toObjectNode().apply {
      pathToObject("article").remove("tagList")
    }.toString()
    client.post("/api/articles", bodyJson)
      .then()
      .verifyResponse(Schemas.article, 201)
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
        .verifyValidationError("title", "must not be blank")
        .verifyValidationError("description", "must not be blank")
        .verifyValidationError("body", "must not be blank")
    }

    listOf("title", "description", "body").map { prop ->
      req.toObjectNode().apply {
        pathToObject("article").remove(prop)
      }.toString().let {
        client.post("/api/articles", it)
          .then()
          .statusCode(400)
          .body("errorCode", Matchers.equalTo("HttpMessageNotReadableException"))
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
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    jane.api.get("/api/articles/${dragon.slug}", token = null)
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected, following = null)
      }

    val cheeta = UserClient.from(TestUsers.Cheeta)
    cheeta.api.get("/api/articles/${dragon.slug}")
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected, following = false)
      }

    cheeta.api.post<Any>("/api/profiles/${TestUsers.Jane.username}/follow")
    cheeta.api.get("/api/articles/${dragon.slug}")
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected, following = true)
      }
  }

  @Test
  fun `get by slug, not found`() {
    UserClient.from(TestUsers.Jane).api.get("/api/articles/not-found")
      .then()
      .statusCode(404)
  }

  @Test
  fun `delete by slug`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    jane.api.delete("/api/articles/${dragon.slug}").then().statusCode(204)
    jane.api.get("/api/articles/${dragon.slug}").then().statusCode(404)
  }

  @Test
  fun `delete by slug, not found`() {
    UserClient.from(TestUsers.Jane).api.delete("/api/articles/not-found")
      .then()
      .statusCode(404)
  }

  @Test
  fun `delete by slug, not author`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val cheeta = UserClient.from(TestUsers.Cheeta)
    cheeta.api.delete("/api/articles/${dragon.slug}").then().statusCode(403)
  }

  @Test
  fun `delete by slug requires auth`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    jane.api.delete("/api/articles/${dragon.slug}", token = null).then().statusCode(401)
    jane.api.get("/api/articles/${dragon.slug}").then().statusCode(200)
  }

  @Test
  fun `update article title`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val updateReq = UpdateRequest(UpdateDto(title = "updated.${dragon.expected.title}"))
    jane.api.put("/api/articles/${dragon.slug}", updateReq)
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected.copy(
          slug = "updated-${dragon.slug}",
          title = "updated.${dragon.expected.title}"
        ))
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article description`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val updateReq = UpdateRequest(UpdateDto(description = "updated.${dragon.expected.description}"))
    jane.api.put("/api/articles/${dragon.slug}", updateReq)
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected.copy(
          description = "updated.${dragon.expected.description}"
        ))
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article body`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val updateReq = UpdateRequest(UpdateDto(body = "updated.${dragon.expected.body}"))
    jane.api.put("/api/articles/${dragon.slug}", updateReq)
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected.copy(
          body = "updated.${dragon.expected.body}"
        ))
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article title, description and body`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val updateReq = UpdateRequest(UpdateDto(
      title = "updated.${dragon.expected.title}",
      description = "updated.${dragon.expected.description}",
      body = "updated.${dragon.expected.body}"
    ))
    jane.api.put("/api/articles/${dragon.slug}", updateReq)
      .then()
      .verifyResponse(Schemas.article, 200)
      .toDto<ArticleResponse>().apply {
        article.assert(dragon.expected.copy(
          slug = "updated-${dragon.slug}",
          title = "updated.${dragon.expected.title}",
          description = "updated.${dragon.expected.description}",
          body = "updated.${dragon.expected.body}"
        ))
        assertThat(article.updatedAt).isAfter(article.createdAt)
      }
  }

  @Test
  fun `update article, not found`() {
    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    UserClient.from(TestUsers.Jane).api.put("/api/articles/not-found", updateReq)
      .then()
      .statusCode(404)
  }

  @Test
  fun `update article, not author`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    UserClient.from(TestUsers.Cheeta).api.put("/api/articles/${dragon.slug}", updateReq)
      .then()
      .statusCode(403)
  }

  @Test
  fun `update article requires auth`() {
    val jane = UserClient.from(TestUsers.Jane)
    val dragon = createArticle(jane, dragonSpec(jane.user.username))

    val updateReq = UpdateRequest(UpdateDto(description = "updated"))
    jane.api.put("/api/articles/${dragon.slug}", updateReq, null)
      .then()
      .statusCode(401)
  }

  @Test
  fun `list articles`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    val react = createArticle(jane, reactSpec(jane.user.username))
    val angular = createArticle(jane, angularSpec(jane.user.username))
    val elm = createArticle(jane, elmSpec(jane.user.username))

    cheeta.api.get("/api/articles", null)
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected, following = null)
        articles[1].assert(angular.expected, following = null)
        articles[2].assert(react.expected, following = null)
        articles[3].assert(dragon.expected, following = null)
      }

    cheeta.api.get("/api/articles")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected)
        articles[1].assert(angular.expected)
        articles[2].assert(react.expected)
        articles[3].assert(dragon.expected)
      }

    cheeta.api.post<Any>("/api/articles/${dragon.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)

    cheeta.api.get("/api/articles")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected)
        articles[1].assert(angular.expected)
        articles[2].assert(react.expected)
        articles[3].assert(dragon.expected, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.post<Any>("/api/profiles/${jane.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get("/api/articles")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected, following = true)
        articles[1].assert(angular.expected, following = true)
        articles[2].assert(react.expected, following = true)
        articles[3].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }
  }

  @Test
  fun `list articles, offset and limit`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    val react = createArticle(jane, reactSpec(jane.user.username))
    val angular = createArticle(jane, angularSpec(jane.user.username))
    val elm = createArticle(jane, elmSpec(jane.user.username))

    cheeta.api.get("/api/articles?limit=2")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(elm.expected)
        articles[1].assert(angular.expected)
      }

    cheeta.api.get("/api/articles?limit=2&offset=1")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(angular.expected)
        articles[1].assert(react.expected)
      }

    cheeta.api.get("/api/articles?offset=1")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(3)

        articles[0].assert(angular.expected)
        articles[1].assert(react.expected)
        articles[2].assert(dragon.expected)
      }
  }

  @Test
  fun `list articles, by author`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    val react = createArticle(jane, reactSpec(jane.user.username))
    val angular = createArticle(jane, angularSpec(jane.user.username))
    val elm = createArticle(jane, elmSpec(jane.user.username))

    cheeta.api.get("/api/articles?author=${TestUsers.Cheeta.username}")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }

    cheeta.api.get("/api/articles?author=${TestUsers.Jane.username}")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected)
        articles[1].assert(angular.expected)
        articles[2].assert(react.expected)
        articles[3].assert(dragon.expected)
      }

    cheeta.api.post<Any>("/api/articles/${dragon.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)

    cheeta.api.get("/api/articles?author=${TestUsers.Jane.username}")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected)
        articles[1].assert(angular.expected)
        articles[2].assert(react.expected)
        articles[3].assert(dragon.expected, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.post<Any>("/api/profiles/${jane.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get("/api/articles?author=${TestUsers.Jane.username}")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected, following = true)
        articles[1].assert(angular.expected, following = true)
        articles[2].assert(react.expected, following = true)
        articles[3].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }
  }

  @Test
  fun `list articles, by tag`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    val react = createArticle(jane, reactSpec(jane.user.username))
    createArticle(jane, angularSpec(jane.user.username))
    createArticle(jane, elmSpec(jane.user.username))

    cheeta.api.get("/api/articles?tag=foo")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }

    cheeta.api.get("/api/articles?tag=dragons")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected)
      }

    cheeta.api.get("/api/articles?tag=reactjs")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(2))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(react.expected)
        articles[1].assert(dragon.expected)
      }

    cheeta.api.post<Any>("/api/articles/${dragon.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)

    cheeta.api.get("/api/articles?tag=dragons")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.get("/api/articles?tag=reactjs")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(2))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(react.expected)
        articles[1].assert(dragon.expected, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.post<Any>("/api/profiles/${jane.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get("/api/articles?tag=dragons")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.get("/api/articles?tag=reactjs")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(2))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(react.expected, following = true)
        articles[1].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }
  }

  @Test
  fun `list articles, by author and tag`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    createArticle(jane, reactSpec(jane.user.username))
    createArticle(jane, angularSpec(jane.user.username))
    createArticle(jane, elmSpec(jane.user.username))

    cheeta.api.get("/api/articles?author=${TestUsers.Jane.username}&tag=dragons")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected)
      }

    cheeta.api.post<Any>("/api/articles/${dragon.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)

    cheeta.api.get("/api/articles?author=${TestUsers.Jane.username}&tag=dragons")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.post<Any>("/api/profiles/${jane.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get("/api/articles?author=${TestUsers.Jane.username}&tag=dragons")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }
  }

  @Test
  fun `list articles, by author, tag and favorited`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    createArticle(jane, reactSpec(jane.user.username))
    createArticle(jane, angularSpec(jane.user.username))
    createArticle(jane, elmSpec(jane.user.username))

    cheeta.api.get(
      "/api/articles?author=${TestUsers.Jane.username}&tag=dragons&favorited=${TestUsers.Cheeta.username}"
    )
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }

    cheeta.api.post<Any>("/api/articles/${dragon.slug}/favorite").then().statusCode(200)

    cheeta.api.get(
      "/api/articles?author=${TestUsers.Jane.username}&tag=dragons&favorited=${TestUsers.Cheeta.username}"
    )
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected, favorited = true, favoritesCount = 1L)
      }

    cheeta.api.post<Any>("/api/profiles/${jane.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get(
      "/api/articles?author=${TestUsers.Jane.username}&tag=dragons&favorited=${TestUsers.Cheeta.username}"
    )
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(1))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(1)

        articles[0].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }
  }

  @Test
  fun `feed articles`() {
    val cheeta = createUser(TestUsers.Cheeta).let { UserClient(it, ApiClient(spec, it.token)) }
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }
    val tarzan = createUser(TestUsers.Tarzan).let { UserClient(it, ApiClient(spec, it.token)) }

    val dragon = createArticle(jane, dragonSpec(jane.user.username))
    val react = createArticle(jane, reactSpec(jane.user.username))

    val angular = createArticle(tarzan, angularSpec(tarzan.user.username))
    val elm = createArticle(tarzan, elmSpec(tarzan.user.username))

    cheeta.api.get("/api/articles/feed", null)
      .then()
      .statusCode(401)

    cheeta.api.get("/api/articles/feed")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(0))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(0)
      }

    cheeta.api.post<Any>("/api/profiles/${tarzan.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get("/api/articles/feed")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(2))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(elm.expected, following = true)
        articles[1].assert(angular.expected, following = true)
      }

    cheeta.api.post<Any>("/api/profiles/${jane.user.username}/follow")
      .then()
      .verifyResponse(Schemas.profile, 200)

    cheeta.api.get("/api/articles/feed")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected, following = true)
        articles[1].assert(angular.expected, following = true)
        articles[2].assert(react.expected, following = true)
        articles[3].assert(dragon.expected, following = true)
      }

    cheeta.api.post<Any>("/api/articles/${angular.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)
    cheeta.api.post<Any>("/api/articles/${dragon.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)

    cheeta.api.get("/api/articles/feed")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(4)

        articles[0].assert(elm.expected, following = true)
        articles[1].assert(angular.expected, following = true, favorited = true, favoritesCount = 1L)
        articles[2].assert(react.expected, following = true)
        articles[3].assert(dragon.expected, following = true, favorited = true, favoritesCount = 1L)
      }

    jane.api.post<Any>("/api/articles/${angular.slug}/favorite")
      .then()
      .verifyResponse(Schemas.article, 200)

    cheeta.api.get("/api/articles/feed?limit=2&offset=1")
      .then()
      .verifyResponse(Schemas.articles, 200)
      .body("articlesCount", Matchers.equalTo(4))
      .toDto<ArticlesResponse>().apply {
        assertThat(articles.size).isEqualTo(2)

        articles[0].assert(angular.expected, following = true, favorited = true, favoritesCount = 2L)
        articles[1].assert(react.expected, following = true)
      }
  }

  @Test
  fun `get tags`() {
    val jane = createUser(TestUsers.Jane).let { UserClient(it, ApiClient(spec, it.token)) }
    val johnDoe = ApiClient(spec)

    JdbcTestUtils.deleteFromTables(jdbcTemplate, TagTbl.table)
    johnDoe.get("/api/tags")
      .then()
      .verifyResponse(Schemas.tags, 200)
      .toDto<TagsResponse>().apply {
        assertThat(tags.size).isEqualTo(0)
      }

    createArticle(jane, dragonSpec(jane.user.username))
    johnDoe.get("/api/tags")
      .then()
      .verifyResponse(Schemas.tags, 200)
      .toDto<TagsResponse>().apply {
        assertThat(tags).containsExactlyInAnyOrder("angularjs", "dragons", "reactjs")
      }

    JdbcTestUtils.deleteFromTables(jdbcTemplate, TagTbl.table)
    createArticle(jane, angularSpec(jane.user.username))
    johnDoe.get("/api/tags")
      .then()
      .verifyResponse(Schemas.tags, 200)
      .toDto<TagsResponse>().apply {
        assertThat(tags).containsExactlyInAnyOrder("101", "angularjs")
      }

    JdbcTestUtils.deleteFromTables(jdbcTemplate, TagTbl.table)
    createArticle(jane, angularSpec(jane.user.username))
    createArticle(jane, dragonSpec(jane.user.username))
    createArticle(jane, elmSpec(jane.user.username))
    createArticle(jane, reactSpec(jane.user.username))
    johnDoe.get("/api/tags")
      .then()
      .verifyResponse(Schemas.tags, 200)
      .toDto<TagsResponse>().apply {
        assertThat(tags).containsExactlyInAnyOrder("101", "angularjs", "dragons", "Elm", "NoExceptions", "reactjs")
      }
  }

  private fun createUser(user: TestUser) =
    userRepo.create(fixtures.validTestUserRegistration(user.username, user.email)).unsafeRunSync()

  private fun UserClient.Companion.from(user: TestUser) =
    createUser(user).let { UserClient(it, ApiClient(spec, it.token)) }
}

private data class ArticleFixture(
  val expected: ArticleResponseDto,
  val slug: String
)

private fun createArticle(
  userClient: UserClient,
  articleSpec: ArticleSpec
) = CreationRequest(articleSpec.req).let {
  ArticleFixture(
    articleSpec.resp,
    userClient.api.post("/api/articles", it)
      .then()
      .verifyResponse(Schemas.article, 201)
      .toDto<ArticleResponse>().article.slug
  )
}

fun ArticleResponseDto.assert(
  expected: ArticleResponseDto,
  following: Boolean? = false,
  favorited: Boolean = false,
  favoritesCount: Long = 0L
) {
  assertThat(this).isEqualToIgnoringGivenFields(expected.copy(favorited = favorited, favoritesCount = favoritesCount),
    "author", "tagList", "createdAt", "updatedAt")
  assertThat(this.author).isEqualTo(expected.author.copy(following = following))
  assertThat(this.tagList).containsExactlyInAnyOrder(*expected.tagList.toTypedArray())
}
