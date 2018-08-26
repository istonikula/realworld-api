package io.realworld

import io.realworld.domain.common.Auth
import io.realworld.persistence.UserRepository
import io.realworld.persistence.UserTbl
import io.restassured.builder.RequestSpecBuilder
import io.restassured.filter.log.RequestLoggingFilter
import io.restassured.filter.log.ResponseLoggingFilter
import io.restassured.http.ContentType
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.equalTo
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileTests {
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
  fun deleteUser() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `get profile`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = fixtures.validTestUserRegistration("bar", "bar@realworld.io")
    val user3 = fixtures.validTestUserRegistration("baz", "baz@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()
    userRepo.create(user3).unsafeRunSync()

    userRepo.addFollower(user2.id, user1.id).unsafeRunSync()
    userRepo.addFollower(user3.id, user1.id).unsafeRunSync()

    val user1Client = ApiClient(spec, user1.token)
    val user2Client = ApiClient(spec, user2.token)

    user1Client.get("/api/profiles/bar")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(true))

    user1Client.get("/api/profiles/baz")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("baz"))
      .body("profile.following", equalTo(true))

    user2Client.get("/api/profiles/foo")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("foo"))
      .body("profile.following", equalTo(false))
  }

  @Test
  fun `get profile, not found`() {
    ApiClient(spec).get("/api/profiles/not-found").then().statusCode(404)
  }

  @Test
  fun `get profile without token`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = fixtures.validTestUserRegistration("bar", "bar@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()

    userRepo.addFollower(user1.id, user2.id).unsafeRunSync()

    ApiClient(spec).get("/api/profiles/bar")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(null))
  }

  @Test
  fun `follow`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = fixtures.validTestUserRegistration("bar", "bar@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()

    val client = ApiClient(spec, user1.token)

    client.get("/api/profiles/bar")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(false))

    client.post<Any>("/api/profiles/bar/follow")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(true))
  }

  @Test
  fun `follow phantom`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    userRepo.create(user1).unsafeRunSync()

    ApiClient(spec, user1.token).post<Any>("/api/profiles/bar/follow")
      .then()
      .statusCode(404)
  }

  @Test
  fun `follow already followed`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = fixtures.validTestUserRegistration("bar", "bar@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()
    userRepo.addFollower(user2.id, user1.id).unsafeRunSync()

    ApiClient(spec, user1.token).post<Any>("/api/profiles/bar/follow")
      .then()
      .statusCode(200)
  }

  @Test
  fun `unfollow`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = fixtures.validTestUserRegistration("bar", "bar@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()

    val client = ApiClient(spec, user1.token)

    client.post<Any>("/api/profiles/bar/follow")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(true))

    client.delete("/api/profiles/bar/follow", user1.token)
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(false))
  }

  @Test
  fun `unfollow phantom`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    userRepo.create(user1).unsafeRunSync()

    ApiClient(spec, user1.token).delete("/api/profiles/bar/follow")
      .then()
      .statusCode(404)
  }

  @Test
  fun `unfollow not followed`() {
    val user1 = fixtures.validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = fixtures.validTestUserRegistration("bar", "bar@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()

    ApiClient(spec, user1.token).delete("/api/profiles/bar/follow")
      .then()
      .statusCode(200)
  }
}
