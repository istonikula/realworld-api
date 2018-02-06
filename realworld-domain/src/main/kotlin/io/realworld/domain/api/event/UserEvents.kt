package io.realworld.domain.api.event

import io.realworld.domain.api.dto.UserDto

class AuthenticateEvent(val token: String) : CommandEvent

class AuthenticatedEvent(val user: UserDto) : ResponseEvent
