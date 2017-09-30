package io.realworld.domain.api.event

import io.realworld.domain.api.dto.UserDto

class LoginEvent(val email: String, val password: String): CommandEvent
class RegisterEvent(val username: String, val email: String, val password: String): CommandEvent

class LoggedInEvent(val user: UserDto): ResponseEvent
class RegisteredEvent(val user: UserDto): ResponseEvent
