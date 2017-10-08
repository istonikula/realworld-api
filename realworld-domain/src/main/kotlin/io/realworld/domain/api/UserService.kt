package io.realworld.domain.api

import io.realworld.domain.api.event.*

interface UserService {
  fun authenticate(e: AuthenticateEvent): AuthenticatedEvent
  fun register(e: RegisterEvent): RegisteredEvent
  fun login(e: LoginEvent): LoggedInEvent
}
