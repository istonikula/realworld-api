// ktlint-disable filename
package io.realworld.domain.users

import arrow.core.none
import arrow.core.some
import io.realworld.domain.fixtures.UserFactory
import io.realworld.domain.fixtures.userAndPassword
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

val userFactory = UserFactory(Stubs.auth)

class RegisterUserUseCaseTests {
  private val jane = userFactory.createRegistration("jane")

  @Test
  fun register() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUser(userFactory.validRegistration)
      override val createUser = Stubs.createUser
    }.test(jane).fold(
      { fail<Nothing>("right expected $it") },
      {
        assertThat(it.email).isEqualTo(jane.email)
        assertThat(it.username).isEqualTo(jane.username)
      }
    )
  }

  @Test
  fun `email already taken`() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUserError(UserRegistrationError.EmailAlreadyTaken)
      override val createUser = Stubs.unexpectedCreateUser
    }.test(jane).fold(
      { assertThat(it).isSameAs(UserRegistrationError.EmailAlreadyTaken) },
      { fail("left expected") }
    )
  }

  @Test
  fun `username already taken`() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUserError(UserRegistrationError.UsernameAlreadyTaken)
      override val createUser = Stubs.unexpectedCreateUser
    }.test(jane).fold(
      { assertThat(it).isEqualTo(UserRegistrationError.UsernameAlreadyTaken) },
      { fail("left expected") }
    )
  }
}

class LoginUserUseCaseTests {
  private val jane = userFactory.createRegistration("jane")

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

private fun RegisterUserUseCase.test(input: UserRegistration) = this.run {
  RegisterUserCommand(input).runUseCase()
}.unsafeRunSync()

private fun LoginUserUseCase.test(email: String, password: String) = this.run {
  LoginUserCommand(email, password).runUseCase()
}.unsafeRunSync()

