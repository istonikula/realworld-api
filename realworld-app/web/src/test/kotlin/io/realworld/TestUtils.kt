package io.realworld

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.realworld.domain.articles.ValidArticleCreation
import io.realworld.domain.articles.articleId
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.User
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.domain.users.userId
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.module.jsv.JsonSchemaValidator
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers
import java.util.UUID

fun initSpec(port: Int) = RequestSpecBuilder()
  .setContentType(ContentType.JSON)
  .setBaseUri("http://localhost:$port")
  .addFilter(RequestLoggingFilter())
  .addFilter(ResponseLoggingFilter())

inline fun <reified T> ValidatableResponse.toDto(): T = this.extract().`as`(T::class.java)

private val defaultObjectMapper = ObjectMapper()
fun <T> T.toObjectNode(objectMapper: ObjectMapper = defaultObjectMapper) = objectMapper.valueToTree<ObjectNode>(this)
fun JsonNode.pathToObject(fieldName: String) = this.path(fieldName) as ObjectNode

class ApiClient(val spec: RequestSpecification, val defaultToken: String? = null) {

  fun get(path: String, token: String? = defaultToken) =
    RestAssured.given().spec(spec).token(token).get(path)

  fun <T> post(path: String, body: T? = null, token: String? = defaultToken) =
    RestAssured.given().spec(spec).token(token).maybeBody(body).post(path)

  fun <T> put(path: String, body: T?, token: String? = defaultToken) =
    RestAssured.given().spec(spec).token(token).maybeBody(body).put(path)

  fun delete(path: String, token: String? = defaultToken) =
    RestAssured.given().spec(spec).token(token).delete(path)

  private fun RequestSpecification.token(token: String?) =
    if (token != null) {
      this.header("Authorization", "Token $token")
    } else this

  private fun RequestSpecification.maybeBody(body: Any?) =
    if (body != null) {
      this.body(body)
    } else this
}

class UserClient(val user: User, val api: ApiClient) {
  companion object
}

class FixtureFactory(val auth: Auth) {
  fun validTestArticleCreation() = ValidArticleCreation(
    id = UUID.randomUUID().articleId(),
    slug = "how-to-train-your-dragon",
    title = "How to train your dragon",
    description = "Ever wonder how?",
    body = "You have to believe",
    tagList = listOf("reactjs", "angularjs", "dragons")
  )

  fun validTestUserRegistration(username: String, email: String): ValidUserRegistration {
    val id = UUID.randomUUID().userId()
    return ValidUserRegistration(
      id = id,
      username = username,
      email = email,
      encryptedPassword = auth.encryptPassword("plain"),
      token = auth.createToken(Token(id))
    )
  }
}

object Schemas {
  const val article = "json-schemas/resp-article.json"
  const val articles = "json-schemas/resp-articles.json"
  const val comment = "json-schemas/resp-comment.json"
  const val comments = "json-schemas/resp-comments.json"
  const val profile = "json-schemas/resp-profile.json"
  const val tags = "json-schemas/resp-tags.json"
  const val user = "json-schemas/resp-user.json"
}

fun ValidatableResponse.verifyResponse(schema: String, statusCode: Int) =
  statusCode(statusCode)
  .body(JsonSchemaValidator.matchesJsonSchemaInClasspath(schema))

fun ValidatableResponse.verifyValidationError(path: String, message: String) =
  body("errors", Matchers.hasItem(Matchers.equalTo(mapOf(
    "message" to message,
    "metadata" to mapOf("path" to path)
  ))))

