package io.realworld.domain.users

import io.realworld.domain.common.DomainError

object ErrorMsg {
  const val emailAlreadyTaken = "Email already taken"
  const val usernameAlreadyTaken = "Username already taken"
}

sealed class UserLoginError(override val msg: String) : DomainError.Single() {
  object BadCredentials : UserLoginError("Bad credentials")
}

sealed class UserRegistrationError(override val msg: String) : DomainError.Single() {
  object EmailAlreadyTaken : UserRegistrationError(ErrorMsg.emailAlreadyTaken)
  object UsernameAlreadyTaken : UserRegistrationError(ErrorMsg.usernameAlreadyTaken)
}

sealed class UserUpdateError(override val msg: String) : DomainError.Single() {
  object EmailAlreadyTaken : UserUpdateError(ErrorMsg.emailAlreadyTaken)
  object UsernameAlreadyTaken : UserUpdateError(ErrorMsg.usernameAlreadyTaken)
}
