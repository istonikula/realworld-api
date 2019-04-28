package io.realworld.domain.users

import io.realworld.domain.fixtures.UserFactory
import io.realworld.domain.fixtures.update
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

private val userFactory = UserFactory(Stubs.auth)

class ValidateUserUpdateServiceTest {
  private val jane = userFactory.createUser("jane")
  private val janeUpdated = jane.update()

  @Test
  fun validate() {
    object : ValidateUserUpdateService {
      override val auth = Stubs.auth
      override val existsByUsername = Stubs.existsByUsername(false)
      override val existsByEmail = Stubs.existsByEmail(false)
    }.test(janeUpdated, jane).fold(
      { fail<Nothing>("right expected $it") },
      {
        assertThat(it).isEqualTo(
          userFactory.run { janeUpdated.valid(jane) }
        )
      }
    )
  }

  @Test
  fun `email already taken`() {
    object : ValidateUserUpdateService {
      override val auth = Stubs.auth
      override val existsByUsername = Stubs.existsByUsername(false)
      override val existsByEmail = Stubs.existsByEmail(true)
    }.test(janeUpdated, jane).fold(
      { assertThat(it).isSameAs(UserUpdateError.EmailAlreadyTaken) },
      { fail("left expected") }
    )
  }

  @Test
  fun `username already taken`() {
    object : ValidateUserUpdateService {
      override val auth = Stubs.auth
      override val existsByUsername = Stubs.existsByUsername(true)
      override val existsByEmail = Stubs.existsByEmail(false)
    }.test(janeUpdated, jane).fold(
      { assertThat(it).isSameAs(UserUpdateError.UsernameAlreadyTaken) },
      { fail("left expected") }
    )
  }
}

private fun ValidateUserUpdateService.test(update: UserUpdate, current: User) = run {
  update.validate(current)
}.unsafeRunSync()
