package io.realworld.domain.users

import arrow.core.left
import arrow.core.right
import arrow.effects.liftIO
import unexpected
import java.util.UUID

object Stubs {
  val createUser: CreateUser = { x ->
    User(id = UUID.randomUUID().userId(), email = x.email, token = x.token, username = x.username).liftIO()
  }

  val unexpectedCreateUser: CreateUser =
    { _: ValidUserRegistration -> unexpected("create user") }

  fun validateUser(fn: (UserRegistration) -> ValidUserRegistration): ValidateUserRegistration = { x: UserRegistration ->
    fn(x).right().liftIO()
  }

  fun validateUserError(error: UserRegistrationError): ValidateUserRegistration = { _ ->
    error.left().liftIO()
  }
}
