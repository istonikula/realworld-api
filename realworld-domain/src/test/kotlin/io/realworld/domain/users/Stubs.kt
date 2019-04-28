package io.realworld.domain.users

import arrow.core.Option
import arrow.core.left
import arrow.core.right
import arrow.effects.liftIO
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Settings
import unexpected
import java.util.UUID

object Stubs {
  // -- AUTH

  val auth = Auth(Settings().apply {
    security.tokenSecret = "secret"
  }.security)

  // -- CREATE USER

  val createUser: CreateUser = { x ->
    User(id = UUID.randomUUID().userId(), email = x.email, token = x.token, username = x.username).liftIO()
  }

  val unexpectedCreateUser: CreateUser =
    { _: ValidUserRegistration -> unexpected("create user") }

  fun validateUser(fn: (UserRegistration) -> ValidUserRegistration): ValidateUserRegistration =
    { x: UserRegistration -> fn(x).right().liftIO() }

  fun validateUserError(error: UserRegistrationError): ValidateUserRegistration =
    { _ -> error.left().liftIO() }

  // -- UPDATE USER

  val updateUser: UpdateUser = { update, current ->
    current.copy(
      username = update.username,
      email = update.email,
      bio = update.bio,
      image = update.image
    ).liftIO()
  }

  val unexpectedUpdateUser: UpdateUser =
    { _: ValidUserUpdate, _: User -> unexpected("update user") }

  fun validateUpdate(fn: (UserUpdate, User) -> ValidUserUpdate): ValidateUserUpdate =
    { x: UserUpdate, current: User -> fn(x, current).right().liftIO() }

  fun validUserUpdateError(error: UserUpdateError): ValidateUserUpdate =
    { _: UserUpdate, _: User -> error.left().liftIO() }

  // -- USER MISC

  fun getUserByEmail(resultL: () -> Option<UserAndPassword>): GetUserByEmail =
    { _ -> resultL().liftIO() }

  fun existsByEmail(exists: Boolean): ExistsByEmail =
    { _: String -> exists.liftIO() }

  fun existsByUsername(exists: Boolean): ExistsByUsername =
    { _: String -> exists.liftIO() }
}
