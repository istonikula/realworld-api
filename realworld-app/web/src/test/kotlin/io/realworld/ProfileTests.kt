package io.realworld

import com.fasterxml.jackson.databind.ObjectMapper
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token
import io.realworld.domain.users.UserRepository
import io.realworld.domain.users.ValidUserRegistration
import io.realworld.persistence.UserTbl
import io.restassured.RestAssured
import io.restassured.specification.RequestSpecification
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.jdbc.JdbcTestUtils
import java.util.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileTests {
  @Autowired
  lateinit var jdbcTemplate: JdbcTemplate

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var auth: Auth

  @Autowired
  lateinit var userRepo: UserRepository

  @LocalServerPort
  var port: Int = 0

  @AfterEach
  fun deleteUser() {
    JdbcTestUtils.deleteFromTables(jdbcTemplate, UserTbl.table)
  }

  @Test
  fun `get profile`() {
    val user1 = validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = validTestUserRegistration("bar", "bar@realworld.io")
    val user3 = validTestUserRegistration("baz", "baz@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()
    userRepo.create(user3).unsafeRunSync()

    userRepo.addFollower(user1.username, user2.username).unsafeRunSync()
    userRepo.addFollower(user1.username, user3.username).unsafeRunSync()

    get("/api/profiles/bar", user1.token)
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(true))

    get("/api/profiles/baz", user1.token)
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("baz"))
      .body("profile.following", equalTo(true))

    get("/api/profiles/foo", user2.token)
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("foo"))
      .body("profile.following", equalTo(false))
  }

  @Test
  fun `get profile without token`() {
    val user1 = validTestUserRegistration("foo", "foo@realworld.io")
    val user2 = validTestUserRegistration("bar", "bar@realworld.io")
    userRepo.create(user1).unsafeRunSync()
    userRepo.create(user2).unsafeRunSync()

    userRepo.addFollower(user1.username, user2.username).unsafeRunSync()

    get("/api/profiles/bar")
      .then()
      .statusCode(200)
      .body("profile.username", equalTo("bar"))
      .body("profile.following", equalTo(null))
  }

  private fun get(path: String, token: String? = null) =
    RestAssured.given().baseUri("http://localhost:${port}").token(token).get(path)

  private fun validTestUserRegistration(username: String, email: String): ValidUserRegistration {
    val id = UUID.randomUUID()
    return ValidUserRegistration(
      id = id,
      username = username,
      email = email,
      encryptedPassword = auth.encryptPassword("plain"),
      token = auth.createToken(Token(id))
    )
  }

  fun RequestSpecification.token(token: String?) =
    if (token != null) {
      this.header("Authorization", "Token ${token}")
    } else this
}
