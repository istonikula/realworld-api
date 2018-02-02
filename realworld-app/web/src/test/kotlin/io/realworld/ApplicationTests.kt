package io.realworld

import com.fasterxml.jackson.databind.ObjectMapper
import io.realworld.domain.core.Auth
import io.realworld.domain.core.Token
import io.realworld.domain.spi.UserModel
import io.realworld.domain.spi.UserRepository
import io.realworld.persistence.InMemoryUserRepository
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.MockBean
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

  @Autowired lateinit var auth: Auth

  @Autowired lateinit var userRepo: InMemoryUserRepository

  @LocalServerPort
  lateinit var port: Integer

  private lateinit var testUser: UserModel

  @BeforeAll
  fun initUser() {
    testUser = UserModel(
      email = "foo@bar.com",
      token = auth.createToken(Token("foo@bar.com")),
      username = "foo",
      password = "baz"
    )
  }

  @Test
  fun `register and login`() {
    val regReq = RegistrationRequest(Registration(username = testUser.username, email = testUser.email, password = testUser.password))
    val expected = User(username = testUser.username, email = testUser.email, token = testUser.token)
    var actual = post("/api/users", regReq)
        .then()
        .statusCode(200)
        .extract().`as`(UserResponse::class.java)
    assertThat(actual.user).isEqualTo(expected)

    val loginReq = LoginRequest(Login(email = regReq.user.email, password = regReq.user.password))
    actual = post("/api/users/login", loginReq)
      .then()
      .statusCode(200)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user).isEqualTo(expected)
  }

  @Test
  fun `invalid request payload is detected`() {
    val req = Login(email = "foo@bar.com", password = "baz")

    post("/api/users/login", asJson(req).replace("\"password\"", "\"bazword\""))
        .prettyPeek()
        .then()
        .statusCode(400)
  }

  @Test
  fun `current user is resolved from token`() {
    userRepo.save(UserModel(
      email = testUser.email,
      token = testUser.token,
      username = testUser.username,
      password = "baz"
    ))

    val actual = get("/api/users", testUser.token)
      .then()
      .statusCode(200)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user.email).isEqualTo("foo@bar.com")
  }

  @Test
  fun `invalid token is reported as 401`() {
    get("/api/users", "invalidToken").then().statusCode(401)
  }

  @Test
  fun `missing auth header is reported as 401`() {
    get("/api/users").then().statusCode(401)
  }

  private fun post(path: String, body: Any) =
    given().baseUri("http://localhost:${port}").contentType(ContentType.JSON).body(body).post(path)

  private fun get(path: String, token: String? = null): Response {
    var spec = given().baseUri("http://localhost:${port}")
    if (token != null) {
      spec = spec.header("Authorization", "Token ${token}")
    }
    return spec.get(path)
  }

  private fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)

}
