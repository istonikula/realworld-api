package io.realworld.domain.api

import arrow.core.Either
import io.realworld.domain.api.event.*

sealed class UserRegisterError {
  object EmailAlreadyTaken : UserRegisterError()
  object UsernameAlreadyTaken : UserRegisterError()
}

interface UserService {
  fun authenticate(e: AuthenticateEvent): AuthenticatedEvent
  fun register(e: RegisterEvent): Either<UserRegisterError, RegisteredEvent>
  fun login(e: LoginEvent): LoggedInEvent
}
