package io.realworld.domain.api

import arrow.core.Either
import io.realworld.domain.api.dto.UserDto


data class RegisterUserCommand(val data: UserRegistration)
data class UserRegistration(val username: String, val email: String, val password: String)
data class RegisterUserAcknowledgment(val user: UserDto)

sealed class UserRegistrationValidationError {
  object EmailAlreadyTaken : UserRegistrationValidationError()
  object UsernameAlreadyTaken : UserRegistrationValidationError()
}

typealias RegisterUser = (cmd: RegisterUserCommand) -> Either<UserRegistrationValidationError, RegisterUserAcknowledgment>
