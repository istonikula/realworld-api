package io.realworld

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.ValidUserRegistration
import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import io.restassured.specification.RequestSpecification
import java.util.UUID

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

class FixtureFactory(val auth: Auth) {
  fun validTestUserRegistration(username: String, email: String): ValidUserRegistration {
    val id = UUID.randomUUID()
    return ValidUserRegistration(
      id = id,
      username = username,
      email = email,
      encryptedPassword = auth.encryptPassword("plain"),
      token = auth.createToken(Token(id))
    )
  }
}
