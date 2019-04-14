// ktlint-disable filename
package io.realworld.domain.users

import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import io.realworld.domain.common.Token
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.util.UUID

class RegisterUserUseCaseTests {
  val auth = Auth(Settings().apply {
    security.tokenSecret = "secret"
  }.security)

  val userRegistration = UserRegistration(
    "foo", "foo@bar.com", "bar"
  )

  @Test
  fun register() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUser(validRegistration)
      override val createUser = Stubs.createUser
    }.test(userRegistration).fold(
      { fail<Nothing>("right expected $it") },
      {
        assertThat(it.email).isEqualTo(userRegistration.email)
        assertThat(it.username).isEqualTo(userRegistration.username)
      }
    )
  }

  @Test
  fun `email already taken`() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUserError(UserRegistrationError.EmailAlreadyTaken)
      override val createUser = Stubs.unexpectedCreateUser
    }.test(userRegistration).fold(
      { assertThat(it).isSameAs(UserRegistrationError.EmailAlreadyTaken) },
      { fail("left expected") }
    )
  }

  @Test
  fun `username already taken`() {
    object : RegisterUserUseCase {
      override val validateUser = Stubs.validateUserError(UserRegistrationError.UsernameAlreadyTaken)
      override val createUser = Stubs.unexpectedCreateUser
    }.test(userRegistration).fold(
      { assertThat(it).isSameAs(UserRegistrationError.UsernameAlreadyTaken) },
      { fail("left expected") }
    )
  }

  private fun RegisterUserUseCase.test(input: UserRegistration) = this.run {
    RegisterUserCommand(input).runUseCase()
  }.unsafeRunSync()

  private val validRegistration = { x: UserRegistration ->
    UUID.randomUUID().userId().let {
      ValidUserRegistration(
        id = it,
        username = x.username,
        email = x.email,
        token = auth.createToken(Token(it)),
        encryptedPassword = auth.encryptPassword(x.password)
      )
    }
  }
}
