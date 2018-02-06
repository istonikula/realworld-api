package io.realworld.domain.api

import io.realworld.domain.api.event.AuthenticateEvent
import io.realworld.domain.api.event.AuthenticatedEvent

sealed class UserRegisterError {
  object EmailAlreadyTaken : UserRegisterError()
  object UsernameAlreadyTaken : UserRegisterError()
}

interface UserService {
  fun authenticate(e: AuthenticateEvent): AuthenticatedEvent
}
