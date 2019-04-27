package io.realworld.domain.users

import arrow.core.none
import arrow.core.some
import io.realworld.domain.fixtures.UserFactory
import io.realworld.domain.fixtures.registration
import io.realworld.domain.fixtures.userAndPassword
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

private val userFactory = UserFactory(Stubs.auth)

class LoginUserUseCaseTest {
  private val jane = userFactory.createUser("jane").registration()

  @Test
  fun login() {
    val expected = userFactory.run { jane.valid().userAndPassword() }

    object : LoginUserUseCase {
      override val auth = Stubs.auth
      override val getUser = Stubs.getUserByEmail { expected.some() }
    }.test(jane.email, jane.password).fold(
      { fail<Nothing>("right expected $it") },
      { assertThat(it).isEqualTo(expected.user) }
    )
  }

  @Test
  fun `not found`() {
    object : LoginUserUseCase {
      override val auth = Stubs.auth
      override val getUser = Stubs.getUserByEmail { none() }
    }.test(jane.email, jane.password).fold(
      { assertThat(it).isEqualTo(UserLoginError.BadCredentials) },
      { fail("left expected") }
    )
  }

  @Test
  fun `invalid password`() {
    object : LoginUserUseCase {
      override val auth = Stubs.auth
      override val getUser = Stubs.getUserByEmail {
        userFactory.run { jane.valid().userAndPassword().some() }
      }
    }.test(jane.email, "invalid password").fold(
      { assertThat(it).isEqualTo(UserLoginError.BadCredentials) },
      { fail("left expected") }
    )
  }
}

private fun LoginUserUseCase.test(email: String, password: String) = run {
  LoginUserCommand(email, password).runUseCase()
}.unsafeRunSync()
