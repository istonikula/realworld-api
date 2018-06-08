package io.realworld.domain.users

import arrow.core.Either
import arrow.effects.IO
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class RegisterUserWorkflowTests {
  val auth0 = Auth(Settings().apply {
    security.tokenSecret = "secret"
  }.security)

  val userRegistration = UserRegistration(
    "foo", "foo@bar.com", "bar"
  )

  @Test
  fun `happy path`() {
    val actual = object : RegisterUserUseCase {
      override val auth = auth0
      override val saveUser = { x: UserModel -> IO { x } }
      override val validateUser = { x: UserRegistration -> IO { Either.right(x) } }
    }.test(userRegistration).unsafeRunSync()

    assertThat(actual.isRight()).isTrue()
  }

  @Test
  fun `exceptions from dependencies are propagated`() {
    assertThatThrownBy {
      object : RegisterUserUseCase {
        override val auth = auth0
        override val saveUser = { x: UserModel -> IO { throw RuntimeException("BOOM!") } }
        override val validateUser = { x: UserRegistration -> IO { Either.right(x) } }
      }.test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")

    assertThatThrownBy {
      object : RegisterUserUseCase {
        override val auth = auth0
        override val saveUser = { x: UserModel -> IO { x } }
        override val validateUser = { x: UserRegistration -> IO { throw RuntimeException("BOOM!") } }
      }.test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")
  }

  @Test
  fun `on validation failure, save user is skipped`() {
    var userSaved = false

    catchThrowable {
      object : RegisterUserUseCase {
        override val auth = auth0
        override val saveUser = { x: UserModel -> IO {
          userSaved = true
          x
        }}
        override val validateUser = { x: UserRegistration -> IO { throw RuntimeException("BOOM!") } }
      }.test(userRegistration).unsafeRunSync()
    }
    assertThat(userSaved).isFalse()
  }

  private fun RegisterUserUseCase.test(input: UserRegistration) = this.run {
    RegisterUserCommand(input).registerUser()
  }
}