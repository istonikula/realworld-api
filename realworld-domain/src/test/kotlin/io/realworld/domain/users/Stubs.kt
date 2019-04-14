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

  // -- USER

  val createUser: CreateUser = { x ->
    User(id = UUID.randomUUID().userId(), email = x.email, token = x.token, username = x.username).liftIO()
  }

  val unexpectedCreateUser: CreateUser =
    { _: ValidUserRegistration -> unexpected("create user") }

  fun validateUser(fn: (UserRegistration) -> ValidUserRegistration): ValidateUserRegistration =
    { x: UserRegistration -> fn(x).right().liftIO() }

  fun validateUserError(error: UserRegistrationError): ValidateUserRegistration =
    { _ -> error.left().liftIO() }

  // -- GET USER

  fun getUserByEmail(resultL: () -> Option<UserAndPassword>): GetUserByEmail =
    { _ -> resultL().liftIO() }

}
