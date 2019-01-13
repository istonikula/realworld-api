// ktlint-disable filename
package io.realworld.domain.users

import arrow.core.right
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.instances.io.monad.monad
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

  val createUser0: CreateUser<ForIO> = { x ->
    User(id = UUID.randomUUID().userId(), email = x.email, token = x.token, username = x.username).liftIO()
  }

  @Test
  fun `happy path`() {
    val actual = object : RegisterUserUseCase<ForIO> {
      override val auth = auth0
      override val createUser = createUser0
      override val validateUser = { x: UserRegistration -> x.autovalid().right().liftIO() }
      override val M = IO.monad()
    }.test(userRegistration).unsafeRunSync()

    assertThat(actual.isRight()).isTrue()
  }

  @Test
  fun `exceptions from dependencies are propagated`() {
    assertThatThrownBy {
      object : RegisterUserUseCase<ForIO> {
        override val auth = auth0
        override val createUser: CreateUser<ForIO> = { IO.raiseError(RuntimeException("BOOM!")) }
        override val validateUser = { x: UserRegistration -> x.autovalid().right().liftIO() }
        override val M = IO.monad()
      }.test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")

    assertThatThrownBy {
      object : RegisterUserUseCase<ForIO> {
        override val auth = auth0
        override val createUser = createUser0
        override val validateUser: ValidateUserRegistration<ForIO> = { IO.raiseError(RuntimeException("BOOM!")) }
        override val M = IO.monad()
      }.test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")
  }

  @Test
  fun `on validation failure, save user is skipped`() {
    var userSaved = false

    catchThrowable {
      object : RegisterUserUseCase<ForIO> {
        override val auth = auth0
        override val createUser: CreateUser<ForIO> = { x ->
          IO {
            userSaved = true
            User(id = UUID.randomUUID().userId(), email = x.email, token = x.token, username = x.username)
          }
        }
        override val validateUser: ValidateUserRegistration<ForIO> = { IO.raiseError(RuntimeException("BOOM!")) }
        override val M = IO.monad()
      }.test(userRegistration).unsafeRunSync()
    }
    assertThat(userSaved).isFalse()
  }

  private fun RegisterUserUseCase<ForIO>.test(input: UserRegistration) = this.run {
    RegisterUserCommand(input).runUseCase().fix()
  }

  private fun UserRegistration.autovalid() = UUID.randomUUID().userId().let {
    ValidUserRegistration(
      id = it,
      username = username,
      email = email,
      token = auth0.createToken(Token(it)),
      encryptedPassword = auth0.encryptPassword(password)
    )
  }
}
