package io.realworld.domain.users

import io.realworld.domain.fixtures.UserFactory
import io.realworld.domain.fixtures.registration
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

private val userFactory = UserFactory(Stubs.auth)

class ValidateUserServiceTest {
  private val jane = userFactory.createUser("jane")

  @Test
  fun validate() {
    val registration = jane.registration()

    object : ValidateUserService {
      override val auth = Stubs.auth
      override val existsByUsername = Stubs.existsByUsername(false)
      override val existsByEmail = Stubs.existsByEmail(false)
    }.test(registration).fold(
      { fail<Nothing>("right expected $it") },
      {
        assertThat(it).isEqualToIgnoringGivenFields(
          userFactory.run { registration.valid(it.id) },
          "encryptedPassword"
        )
        assertThat(Stubs.auth.checkPassword(registration.password, it.encryptedPassword)).isTrue()
      }
    )
  }

  @Test
  fun `email already taken`() {
    object : ValidateUserService {
      override val auth = Stubs.auth
      override val existsByUsername = Stubs.existsByUsername(false)
      override val existsByEmail = Stubs.existsByEmail(true)
    }.test(jane.registration()).fold(
      { assertThat(it).isSameAs(UserRegistrationError.EmailAlreadyTaken) },
      { fail("left expected") }
    )
  }

  @Test
  fun `username already taken`() {
    object : ValidateUserService {
      override val auth = Stubs.auth
      override val existsByUsername = Stubs.existsByUsername(true)
      override val existsByEmail = Stubs.existsByEmail(false)
    }.test(jane.registration()).fold(
      { assertThat(it).isSameAs(UserRegistrationError.UsernameAlreadyTaken) },
      { fail("left expected") }
    )
  }
}

private fun ValidateUserService.test(x: UserRegistration) = runBlocking {
  x.validate()
}
