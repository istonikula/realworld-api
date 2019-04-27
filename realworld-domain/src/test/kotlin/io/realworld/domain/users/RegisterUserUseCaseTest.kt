package io.realworld.domain.users

import io.realworld.domain.fixtures.UserFactory
import io.realworld.domain.fixtures.registration
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

private val userFactory = UserFactory(Stubs.auth)

class RegisterUserUseCaseTest {
  private val jane = userFactory.createUser("jane")

  @Test
  fun register() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUser(userFactory.validRegistration)
      override val createUser = Stubs.createUser
    }.test(jane.registration()).fold(
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
    }.test(jane.registration()).fold(
      { assertThat(it).isSameAs(UserRegistrationError.EmailAlreadyTaken) },
      { fail("left expected") }
    )
  }

  @Test
  fun `username already taken`() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUserError(UserRegistrationError.UsernameAlreadyTaken)
      override val createUser = Stubs.unexpectedCreateUser
    }.test(jane.registration()).fold(
      { assertThat(it).isEqualTo(UserRegistrationError.UsernameAlreadyTaken) },
      { fail("left expected") }
    )
  }
}

private fun RegisterUserUseCase.test(input: UserRegistration) = run {
  RegisterUserCommand(input).runUseCase()
}.unsafeRunSync()
