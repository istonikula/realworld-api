package io.realworld.domain.users

import arrow.core.right
import arrow.effects.IO
import arrow.effects.liftIO
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import io.realworld.domain.common.Token
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import java.util.UUID

class RegisterUserWorkflowTests {
  val auth0 = Auth(Settings().apply {
    security.tokenSecret = "secret"
  }.security)

  val userRegistration = UserRegistration(
    "foo", "foo@bar.com", "bar"
  )

  val createUser0: CreateUser = { x ->
    User(id = UUID.randomUUID(), email = x.email, token = x.token, username = x.username).liftIO()
  }

  @Test
  fun `happy path`() {
    val actual = object : RegisterUserUseCase {
      override val auth = auth0
      override val createUser = createUser0
      override val validateUser = { x: UserRegistration -> x.autovalid().right().liftIO() }
    }.test(userRegistration).unsafeRunSync()

    assertThat(actual.isRight()).isTrue()
  }

  @Test
  fun `exceptions from dependencies are propagated`() {
    assertThatThrownBy {
      object : RegisterUserUseCase {
        override val auth = auth0
        override val createUser: CreateUser = { _ -> IO.raiseError(RuntimeException("BOOM!")) }
        override val validateUser = { x: UserRegistration -> x.autovalid().right().liftIO() }
      }.test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")

    assertThatThrownBy {
      object : RegisterUserUseCase {
        override val auth = auth0
        override val createUser = createUser0
        override val validateUser: ValidateUserRegistration = { _ -> IO.raiseError(RuntimeException("BOOM!")) }
      }.test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")
  }

  @Test
  fun `on validation failure, save user is skipped`() {
    var userSaved = false

    catchThrowable {
      object : RegisterUserUseCase {
        override val auth = auth0
        override val createUser: CreateUser = { x ->
          IO {
            userSaved = true
            User(id = UUID.randomUUID(), email = x.email, token = x.token, username = x.username)
          }
        }
        override val validateUser = { _: UserRegistration -> IO { throw RuntimeException("BOOM!") } }
      }.test(userRegistration).unsafeRunSync()
    }
    assertThat(userSaved).isFalse()
  }

  private fun RegisterUserUseCase.test(input: UserRegistration) = this.run {
    RegisterUserCommand(input).runUseCase()
  }

  private fun UserRegistration.autovalid() = UUID.randomUUID().let {
    ValidUserRegistration(
      id = it,
      username = username,
      email = email,
      token = auth0.createToken(Token(it)),
      encryptedPassword = auth0.encryptPassword(password)
    )
  }
}

private interface KtlintDisableFilenameRule
