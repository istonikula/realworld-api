package io.realworld.domain.api

import arrow.core.Option

data class User(
  val email: String,
  val token: String,
  val username: String,
  val bio: String? = null,
  val image: String? = null
)

data class RegisterUserCommand(val data: UserRegistration)
data class UserRegistration(val username: String, val email: String, val password: String)
data class RegisterUserAcknowledgment(val user: User)
sealed class UserRegistrationValidationError {
  object EmailAlreadyTaken : UserRegistrationValidationError()
  object UsernameAlreadyTaken : UserRegistrationValidationError()
}

data class LoginUserCommand(val email: String, val password: String)
data class LoginUserAcknowledgment(val user: User)
sealed class UserLoginError {
  object BadCredentials : UserLoginError()
}

data class UserUpdate(
  val username: Option<String>,
  val email: Option<String>,
  val password: Option<String>,
  val bio: Option<String>,
  val image: Option<String>
)
// TODO these are duplicates
sealed class UserUpdateValidationError {
  object EmailAlreadyTaken : UserUpdateValidationError()
  object UsernameAlreadyTaken : UserUpdateValidationError()
}
