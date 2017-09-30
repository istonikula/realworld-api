package io.realworld

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension


data class RegistrationRequest(var user: Registration)
data class LoginRequest(var user: Login)
data class UserResponse(var user: User)

@TestInstance(PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class Spring5ApplicationTests {

  @Autowired lateinit var objectMapper: ObjectMapper

  @LocalServerPort
  lateinit var port: Integer

  @Test
  fun `register user`() {
    val req = RegistrationRequest(Registration(username = "foo", email = "foo@bar.com", password = "baz"))
    val expected = User(username = "foo", email = "foo@bar.com", token = "TODO")

    val actual = post("/api/users", req)
        .then()
        .statusCode(200)
        .extract().`as`(UserResponse::class.java)

    assertThat(actual.user).isEqualTo(expected)
  }

  @Test
  fun `serialization to user works`() {
    val req = LoginRequest(Login(email = "foo@bar.com", password = "baz"))
    val expected = User(email = "foo@bar.com", token = "TODO", username = "foo@bar.com")

    val actual = post("/api/users/login", req)
        .then()
        .statusCode(200)
        .extract().`as`(UserResponse::class.java)

    assertThat(actual.user).isEqualTo(expected)
  }

  @Test
  fun `invalid request payload is detected`() {
    val req = Login(email = "foo@bar.com", password = "baz")

    post("/api/users/login", asJson(req).replace("\"password\"", "\"bazword\""))
        .then()
        .statusCode(400)
  }

  private fun post(path: String, body: Any) =
      given().baseUri("http://localhost:${port}").contentType(ContentType.JSON).body(body).post(path)

  private fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)


}
