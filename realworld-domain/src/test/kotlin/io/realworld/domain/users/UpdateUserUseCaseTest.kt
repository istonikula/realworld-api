package io.realworld.domain.users

import io.realworld.domain.fixtures.UserFactory
import io.realworld.domain.fixtures.update
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.Test

private val userFactory = UserFactory(Stubs.auth)

class UpdateUserUseCaseTest {
  private val jane = userFactory.createUser("jane")
  private val janeUpdated = jane.update()

  @Test
  fun update() {
    object : UpdateUserUseCase {
      override val validateUpdate = Stubs.validateUpdate(userFactory.validUpdate)
      override val updateUser = Stubs.updateUser
    }.test(janeUpdated, jane).fold(
      { fail<Nothing>("right expected $it") },
      {
        assertThat(it.email).isEqualTo(janeUpdated.email.orNull()!!)
        assertThat(it.username).isEqualTo(janeUpdated.username.orNull()!!)
      }
    )
  }

  @Test
  fun `email already taken`() {
    object : UpdateUserUseCase {
      override val validateUpdate = Stubs.validUserUpdateError(UserUpdateError.EmailAlreadyTaken)
      override val updateUser = Stubs.unexpectedUpdateUser
    }.test(janeUpdated, jane).fold(
      { assertThat(it).isSameAs(UserUpdateError.EmailAlreadyTaken) },
      { fail("left expected") }
    )
  }

  @Test
  fun `username already taken`() {
    object : UpdateUserUseCase {
      override val validateUpdate = Stubs.validUserUpdateError(UserUpdateError.UsernameAlreadyTaken)
      override val updateUser = Stubs.unexpectedUpdateUser
    }.test(janeUpdated, jane).fold(
      { assertThat(it).isSameAs(UserUpdateError.UsernameAlreadyTaken) },
      { fail("left expected") }
    )
  }
}

private fun UpdateUserUseCase.test(data: UserUpdate, current: User) = run {
  UpdateUserCommand(data, current).runUseCase()
}.unsafeRunSync()
