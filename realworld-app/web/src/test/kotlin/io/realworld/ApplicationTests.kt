package io.realworld

import com.fasterxml.jackson.databind.ObjectMapper
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.UserModel
import io.realworld.persistence.InMemoryUserRepository
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension

data class RegistrationRequest(var user: RegistrationDto)
data class LoginRequest(var user: LoginDto)
data class UserUpdateRequest(var user: UserUpdateDto)

@TestInstance(PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class Spring5ApplicationTests {

  @Autowired lateinit var objectMapper: ObjectMapper

  @Autowired lateinit var auth: Auth

  @SpyBean lateinit var userRepo: InMemoryUserRepository

  @LocalServerPort
  var port: Int = 0

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

  @AfterEach
  fun deleteUser() {
    userRepo.deleteAll()
  }

  @Test
  fun `register and login`() {
    val regReq = RegistrationRequest(RegistrationDto(username = testUser.username, email = testUser.email, password = testUser.password))
    val expected = UserDto(username = testUser.username, email = testUser.email, token = testUser.token)
    var actual = post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(201)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user).isEqualTo(expected)

    val loginReq = LoginRequest(LoginDto(email = regReq.user.email, password = regReq.user.password))
    actual = post("/api/users/login", loginReq)
      .then()
      .statusCode(200)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user).isEqualTo(expected)
  }

  @Test
  fun `cannot register already existing username`() {
    userRepo.save(UserModel(
      email = testUser.email,
      token = testUser.token,
      username = testUser.username,
      password = "baz"
    ))
    val regReq = RegistrationRequest(RegistrationDto(username = testUser.username, email = "unique.${testUser.email}", password = testUser.password))
    post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(422)
      .body("errors.username.message", equalTo("already taken"))
  }

  @Test
  fun `cannot register already existing email`() {
    userRepo.save(UserModel(
      email = testUser.email,
      token = testUser.token,
      username = testUser.username,
      password = "baz"
    ))
    val regReq = RegistrationRequest(RegistrationDto(username = "unique", email = testUser.email, password = testUser.password))
    post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(422)
      .body("errors.email.message", equalTo("already taken"))
  }

  @Test
  fun `unexpected registration error yields 500`() {
    val regReq = RegistrationRequest(RegistrationDto(username = testUser.username, email = testUser.email, password = testUser.password))

    doThrow(RuntimeException("BOOM!")).`when`(userRepo).existsByEmail(testUser.email)

    post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(500)
  }

  @Test
  fun `invalid request payload is detected`() {
    val req = LoginDto(email = "foo@bar.com", password = "baz")

    post("/api/users/login", asJson(req).replace("\"password\"", "\"bazword\""))
        .prettyPeek()
        .then()
        .statusCode(422)
  }

  @Test
  fun `current user is resolved from token`() {
    userRepo.save(UserModel(
      email = testUser.email,
      token = testUser.token,
      username = testUser.username,
      password = "baz"
    ))

    val actual = get("/api/user", testUser.token)
      .then()
      .statusCode(200)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user.email).isEqualTo("foo@bar.com")
  }

  @Test
  fun `invalid token is reported as 401`() {
    get("/api/user", "invalidToken").then().statusCode(401)
  }

  @Test
  fun `missing auth header is reported as 401`() {
    get("/api/user").then().statusCode(401)
  }

  @Test
  fun `update user email`() {
    userRepo.save(UserModel(
      email = testUser.email,
      token = testUser.token,
      username = testUser.username,
      password = "baz"
    ))

    val updateReq = UserUpdateRequest(UserUpdateDto(email = "updated.${testUser.email}"))
    println(asJson(updateReq))
    val actual = put("/api/user", updateReq, testUser.token)
      .then()
      .statusCode(200)
      .extract().`as`(UserResponse::class.java)
    // TODO assert email changed
    // - in response
    // - in repo
  }

  private fun post(path: String, body: Any) =
    given().baseUri("http://localhost:${port}").contentType(ContentType.JSON).body(body).post(path)

  private fun put(path: String, body: Any, token: String? = null) =
    given().baseUri("http://localhost:${port}").token(token).contentType(ContentType.JSON).body(body).put(path)

  private fun get(path: String, token: String? = null) =
    given().baseUri("http://localhost:${port}").token(token).get(path)

  private fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)

  fun RequestSpecification.token(token: String?) =
    if (token != null) {
      this.header("Authorization", "Token ${token}")
    } else this

}
