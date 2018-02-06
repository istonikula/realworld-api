package io.realworld.domain.core

import arrow.core.Either
import arrow.effects.IO
import io.realworld.domain.api.RegisterUserCommand
import io.realworld.domain.api.UserRegistration
import io.realworld.domain.spi.Settings
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test

class RegisterUserWorkflowTests {
  val auth = Auth(Settings().apply {
    security.tokenSecret = "secret"
  }.security)

  val userRegistration = UserRegistration(
    "foo", "foo@bar.com", "bar"
  )

  @Test
  fun `happy path`() {
    val actual = RegisterUserWorkflow(
      auth,
      { IO { Either.right(it) } },
      { IO { Either.right(it) } }
    ).test(userRegistration).unsafeRunSync()

    assertThat(actual.isRight()).isTrue()
  }

  @Test
  fun `exceptions from dependencies are propagated`() {
    assertThatThrownBy {
      RegisterUserWorkflow(
        auth,
        { IO { throw RuntimeException("BOOM!") } },
        { IO { Either.right(it) } }
      ).test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")

    assertThatThrownBy {
      RegisterUserWorkflow(
        auth,
        { IO { Either.right(it) } },
        { IO { throw RuntimeException("BOOM!") } }
      ).test(userRegistration).unsafeRunSync()
    }.hasMessage("BOOM!")
  }

  @Test
  fun `on validation failure, save user is skipped`() {
    var userSaved = false

    catchThrowable {
      RegisterUserWorkflow(
        auth,
        { IO { throw RuntimeException("BOOM!") } },
        {
          IO {
            userSaved = true
            Either.right(it)
          }
        }
      ).test(userRegistration).unsafeRunSync()
    }
    assertThat(userSaved).isFalse()
  }

  private fun RegisterUserWorkflow.test(input: UserRegistration) =
    this.invoke(RegisterUserCommand(input))
}
