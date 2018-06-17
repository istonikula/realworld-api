package io.realworld

import com.fasterxml.jackson.databind.ObjectMapper
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.UserRepository
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.persistence.UserTbl
import io.realworld.persistence.UserTbl.token
import io.realworld.users.LoginDto
import io.realworld.users.RegistrationDto
import io.realworld.users.UserResponse
import io.realworld.users.UserResponseDto
import io.realworld.users.UserUpdateDto
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
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
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.jdbc.JdbcTestUtils
import java.util.*

data class RegistrationRequest(var user: RegistrationDto)
data class LoginRequest(var user: LoginDto)
data class UserUpdateRequest(var user: UserUpdateDto)

@TestInstance(PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class Spring5ApplicationTests {

  @Autowired lateinit var jdbcTemplate: JdbcTemplate

  @Autowired lateinit var objectMapper: ObjectMapper

  @Autowired lateinit var auth: Auth

  @SpyBean lateinit var userRepo: UserRepository

  @LocalServerPort
  var port: Int = 0

  object TestUser {
    val email = "foo@bar.com"
    val username = "foo"
    val password = "plain"
  }

  @AfterEach
  fun deleteUser() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `register and login`() {
    val regReq = RegistrationRequest(RegistrationDto(
      username = TestUser.username,
      email = TestUser.email,
      password = TestUser.password
    ))
    val expected = UserResponseDto(username = TestUser.username, email = TestUser.email, token = "ignore")
    var actual = post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(201)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user).isEqualToIgnoringGivenFields(expected, "token")

    val loginReq = LoginRequest(LoginDto(email = regReq.user.email, password = regReq.user.password))
    actual = post("/api/users/login", loginReq)
      .then()
      .statusCode(200)
      .extract().`as`(UserResponse::class.java)
    assertThat(actual.user).isEqualToIgnoringGivenFields(expected, token)
  }

  @Test
  fun `cannot register already existing username`() {
    userRepo.create(validTestUserRegistration()).unsafeRunSync()
    val regReq = RegistrationRequest(RegistrationDto(
      username = TestUser.username,
      email = "unique.${TestUser.email}",
      password = TestUser.password
    ))
    post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(422)
      .body("errors.username.message", equalTo("already taken"))
  }

  @Test
  fun `cannot register already existing email`() {
    userRepo.create(validTestUserRegistration()).unsafeRunSync()
    val regReq = RegistrationRequest(RegistrationDto(
      username = "unique",
      email = TestUser.email,
      password = TestUser.password
    ))
    post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(422)
      .body("errors.email.message", equalTo("already taken"))
  }

  @Test
  fun `unexpected registration error yields 500`() {
    val regReq = RegistrationRequest(RegistrationDto(
      username = TestUser.username,
      email = TestUser.email,
      password = TestUser.password
    ))

    doThrow(RuntimeException("BOOM!")).`when`(userRepo).existsByEmail(TestUser.email)

    post("/api/users", regReq)
      .prettyPeek()
      .then()
      .statusCode(500)
  }

  @Test
  fun `invalid request payload is detected`() {
    val req = LoginRequest(LoginDto(email = "foo@bar.com", password = "baz"))

    post("/api/users/login", asJson(req).replace("\"password\"", "\"bazword\""))
        .prettyPeek()
        .then()
        .statusCode(422)
  }

  @Test
  fun `current user is resolved from token`() {
    val registered = validTestUserRegistration()
    userRepo.create(registered).unsafeRunSync()

    val actual = get("/api/user", registered.token)
      .prettyPeek()
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
  fun `invalid password is reported as 401`() {
    val registered = validTestUserRegistration()
    userRepo.create(registered).unsafeRunSync()

    with( LoginRequest(LoginDto(email = registered.email, password = "invalid"))) {
      post("/api/users/login", this).then().statusCode(401)
    }
  }

  @Test
  fun `update user email`() {
    val registered = validTestUserRegistration()
    userRepo.create(registered).unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(email = "updated.${registered.email}")).apply {
      val actual = put("/api/user", this, registered.token)
        .then()
        .statusCode(200)
        .extract().`as`(UserResponse::class.java)
      assertThat(actual.user.email).isEqualTo("updated.${registered.email}")
    }
  }

  @Test
  fun `update user password`() {
    val registered = validTestUserRegistration()
    userRepo.create(registered).unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(password = "updated.plain")).apply {
      put("/api/user", this, registered.token).then().statusCode(200)
    }
    LoginRequest(LoginDto(email = registered.email, password = "updated.plain")).apply {
      post("/api/users/login", this).then().statusCode(200)
    }
  }

  @Test
  fun `update user username`() {
    val registered = validTestUserRegistration()
    userRepo.create(registered).unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(username = "updated.${registered.username}")).apply {
      val actual = put("/api/user", this, registered.token)
        .then()
        .statusCode(200)
        .extract().`as`(UserResponse::class.java)
      assertThat(actual.user.username).isEqualTo("updated.${registered.username}")
    }
  }

  @Test
  fun `update user username, image, bio`() {
    val registered = validTestUserRegistration()
    userRepo.create(registered).unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(
      username = "updated.${registered.username}",
      image = "updated.image",
      bio = "updated.bio"
    )).apply {
      val actual = put("/api/user", this, registered.token)
        .then()
        .statusCode(200)
        .extract().`as`(UserResponse::class.java)
      assertThat(actual.user.username).isEqualTo("updated.${registered.username}")
      assertThat(actual.user.image).isEqualTo("updated.image")
      assertThat(actual.user.bio).isEqualTo("updated.bio")
    }
  }

  private fun post(path: String, body: Any) =
    given().baseUri("http://localhost:${port}").contentType(ContentType.JSON).body(body).post(path)

  private fun put(path: String, body: Any, token: String? = null) =
    given().baseUri("http://localhost:${port}").token(token).contentType(ContentType.JSON).body(body).put(path)

  private fun get(path: String, token: String? = null) =
    given().baseUri("http://localhost:${port}").token(token).get(path)

  private fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)

  private fun String.encrypt() = auth.encryptPassword(this)

  private fun String.checkPass(plain: String) = auth.checkPassword(plain, this)

  private fun validTestUserRegistration(): ValidUserRegistration {
    val id = UUID.randomUUID()
    return ValidUserRegistration(
      id = id,
      username = TestUser.username,
      email = TestUser.email,
      encryptedPassword = auth.encryptPassword(TestUser.password),
      token = auth.createToken(Token(id))
    )
  }

  fun RequestSpecification.token(token: String?) =
    if (token != null) {
      this.header("Authorization", "Token ${token}")
    } else this

}
