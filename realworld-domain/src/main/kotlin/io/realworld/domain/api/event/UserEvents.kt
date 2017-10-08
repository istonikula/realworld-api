package io.realworld.domain.api.event

import io.realworld.domain.api.dto.UserDto

class AuthenticateEvent(val token: String) : CommandEvent
class LoginEvent(val email: String, val password: String) : CommandEvent
class RegisterEvent(val username: String, val email: String, val password: String) : CommandEvent

class AuthenticatedEvent(val user: UserDto) : ResponseEvent
class LoggedInEvent(val user: UserDto) : ResponseEvent
class RegisteredEvent(val user: UserDto) : ResponseEvent
