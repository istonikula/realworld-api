package io.realworld

import arrow.effects.ForIO
import arrow.effects.fix
import io.realworld.domain.common.Auth
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.realworld.persistence.UserTbl.token
import io.realworld.users.LoginDto
import io.realworld.users.RegistrationDto
import io.realworld.users.UserResponse
import io.realworld.users.UserResponseDto
import io.realworld.users.UserUpdateDto
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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

data class RegistrationRequest(val user: RegistrationDto)
data class LoginRequest(val user: LoginDto)
data class UserUpdateRequest(val user: UserUpdateDto)

@TestInstance(PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
class UserTests {
  @LocalServerPort
  var port: Int = 0

  @Autowired lateinit var jdbcTemplate: JdbcTemplate

  @Autowired lateinit var auth: Auth

  @SpyBean lateinit var userRepo: UserRepository<ForIO>

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

  private object TestUser {
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
    val client = ApiClient(spec)

    val regReq = RegistrationRequest(RegistrationDto(
      username = TestUser.username,
      email = TestUser.email,
      password = TestUser.password
    ))
    val expected = UserResponseDto(username = TestUser.username, email = TestUser.email, token = "ignore")
    var actual: UserResponse = client.post("/api/users", regReq)
      .then()
      .statusCode(201)
      .toDto()
    assertThat(actual.user).isEqualToIgnoringGivenFields(expected, "token")

    val loginReq = LoginRequest(LoginDto(email = regReq.user.email, password = regReq.user.password))
    actual = client.post("/api/users/login", loginReq)
      .then()
      .statusCode(200)
      .toDto()
    assertThat(actual.user).isEqualToIgnoringGivenFields(expected, token)
  }

  @Test
  fun `cannot register already existing username`() {
    userRepo.create(fixtures.validTestUserRegistration(TestUser.username, TestUser.email)).fix().unsafeRunSync()
    val regReq = RegistrationRequest(RegistrationDto(
      username = TestUser.username,
      email = "unique.${TestUser.email}",
      password = TestUser.password
    ))
    ApiClient(spec).post("/api/users", regReq)
      .then()
      .statusCode(422)
      .body("errors.username.message", equalTo("already taken"))
  }

  @Test
  fun `cannot register already existing email`() {
    userRepo.create(fixtures.validTestUserRegistration(TestUser.username, TestUser.email)).fix().unsafeRunSync()
    val regReq = RegistrationRequest(RegistrationDto(
      username = "unique",
      email = TestUser.email,
      password = TestUser.password
    ))
    ApiClient(spec).post("/api/users", regReq)
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

    ApiClient(spec).post("/api/users", regReq)
      .then()
      .statusCode(500)
  }

  @Test
  fun `invalid request payload is detected`() {
    val req = LoginRequest(LoginDto(email = "foo@bar.com", password = "baz"))

    ApiClient(spec).post("/api/users/login", req.toObjectNode().apply {
      pathToObject("user").remove("password")
    })
      .then()
      .statusCode(422)
  }

  @Test
  fun `current user is resolved from token`() {
    val registered = fixtures.validTestUserRegistration(TestUser.username, TestUser.email)
    userRepo.create(registered).fix().unsafeRunSync()

    val actual: UserResponse = ApiClient(spec).get("/api/user", registered.token)
      .then()
      .statusCode(200)
      .toDto()
    assertThat(actual.user.email).isEqualTo("foo@bar.com")
  }

  @Test
  fun `invalid token is reported as 401`() {
    ApiClient(spec).get("/api/user", "invalidToken").then().statusCode(401)
  }

  @Test
  fun `missing auth header is reported as 401`() {
    ApiClient(spec).get("/api/user").then().statusCode(401)
  }

  @Test
  fun `invalid password is reported as 401`() {
    val registered = fixtures.validTestUserRegistration(TestUser.username, TestUser.email)
    userRepo.create(registered).fix().unsafeRunSync()

    with(LoginRequest(LoginDto(email = registered.email, password = "invalid"))) {
      ApiClient(spec).post("/api/users/login", this).then().statusCode(401)
    }
  }

  @Test
  fun `update user email`() {
    val registered = fixtures.validTestUserRegistration(TestUser.username, TestUser.email)
    userRepo.create(registered).fix().unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(email = "updated.${registered.email}")).apply {
      val actual: UserResponse = ApiClient(spec).put("/api/user", this, registered.token)
        .then()
        .statusCode(200)
        .toDto()
      assertThat(actual.user.email).isEqualTo("updated.${registered.email}")
    }
  }

  @Test
  fun `update user password`() {
    val registered = fixtures.validTestUserRegistration(TestUser.username, TestUser.email)
    userRepo.create(registered).fix().unsafeRunSync()

    val client = ApiClient(spec)

    UserUpdateRequest(UserUpdateDto(password = "updated.plain")).apply {
      client.put("/api/user", this, registered.token).then().statusCode(200)
    }
    LoginRequest(LoginDto(email = registered.email, password = "updated.plain")).apply {
      client.post("/api/users/login", this).then().statusCode(200)
    }
  }

  @Test
  fun `update user username`() {
    val registered = fixtures.validTestUserRegistration(TestUser.username, TestUser.email)
    userRepo.create(registered).fix().unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(username = "updated.${registered.username}")).apply {
      val actual: UserResponse = ApiClient(spec).put("/api/user", this, registered.token)
        .then()
        .statusCode(200)
        .toDto()
      assertThat(actual.user.username).isEqualTo("updated.${registered.username}")
    }
  }

  @Test
  fun `update user username, image, bio`() {
    val registered = fixtures.validTestUserRegistration(TestUser.username, TestUser.email)
    userRepo.create(registered).fix().unsafeRunSync()

    UserUpdateRequest(UserUpdateDto(
      username = "updated.${registered.username}",
      image = "updated.image",
      bio = "updated.bio"
    )).apply {
      val actual: UserResponse = ApiClient(spec).put("/api/user", this, registered.token)
        .then()
        .statusCode(200)
        .toDto()
      assertThat(actual.user.username).isEqualTo("updated.${registered.username}")
      assertThat(actual.user.image).isEqualTo("updated.image")
      assertThat(actual.user.bio).isEqualTo("updated.bio")
    }
  }
}
