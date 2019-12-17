package io.realworld.domain.users

import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.fx.IO
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import unexpected
import java.util.UUID

/* ktlint-disable curly-spacing */
object Stubs {
  // -- AUTH

  val auth = Auth(Settings().apply {
    security.tokenSecret = "secret"
  }.security)

  // -- CREATE USER

  val createUser: CreateUser = { x ->
    IO { User(id = UUID.randomUUID().userId(), email = x.email, token = x.token, username = x.username) }
  }

  val unexpectedCreateUser: CreateUser =
    { _: ValidUserRegistration -> unexpected("create user") }

  fun validateUser(fn: (UserRegistration) -> ValidUserRegistration): ValidateUserRegistration =
    { x: UserRegistration -> IO { fn(x).right() } }

  fun validateUserError(error: UserRegistrationError): ValidateUserRegistration =
    { _ -> IO { error.left() } }

  // -- UPDATE USER

  val updateUser: UpdateUser = { update, current ->
    IO { current.copy(
      username = update.username,
      email = update.email,
      bio = update.bio,
      image = update.image
    ) }
  }

  val unexpectedUpdateUser: UpdateUser =
    { _: ValidUserUpdate, _: User -> unexpected("update user") }

  fun validateUpdate(fn: (UserUpdate, User) -> ValidUserUpdate): ValidateUserUpdate =
    { x: UserUpdate, current: User -> IO { fn(x, current).right() } }

  fun validUserUpdateError(error: UserUpdateError): ValidateUserUpdate =
    { _: UserUpdate, _: User -> IO { error.left() } }

  // -- USER MISC

  fun getUserByEmail(resultL: () -> Option<UserAndPassword>): GetUserByEmail =
    { _ -> IO { resultL() } }

  fun existsByEmail(exists: Boolean): ExistsByEmail =
    { _: String -> IO { exists } }

  fun existsByUsername(exists: Boolean): ExistsByUsername =
    { _: String -> IO { exists } }
}
/* ktlint-enable curly-spacing */
