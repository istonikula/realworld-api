package io.realworld.domain.api.core

import io.realworld.domain.api.UserService
import io.realworld.domain.api.dto.UserDto
import io.realworld.domain.api.event.LoggedInEvent
import io.realworld.domain.api.event.LoginEvent
import io.realworld.domain.api.event.RegisterEvent
import io.realworld.domain.api.event.RegisteredEvent
import org.springframework.stereotype.Service

@Service
class FakeUserServiceHandler : UserService {
  override fun register(e: RegisterEvent): RegisteredEvent {
    return RegisteredEvent(UserDto(
        email = e.email,
        token = "TODO", // TODO create token
        username = e.username
    ))
  }

  override fun login(e: LoginEvent): LoggedInEvent {
    return LoggedInEvent(UserDto(
        email = e.email,
        token = "TODO", // TODO create token
        username = e.email
    ))
  }
}
