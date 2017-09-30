package io.realworld.domain.api

import io.realworld.domain.api.event.LoggedInEvent
import io.realworld.domain.api.event.LoginEvent
import io.realworld.domain.api.event.RegisterEvent
import io.realworld.domain.api.event.RegisteredEvent

interface UserService {
  fun register(e: RegisterEvent): RegisteredEvent
  fun login(e: LoginEvent): LoggedInEvent
}
