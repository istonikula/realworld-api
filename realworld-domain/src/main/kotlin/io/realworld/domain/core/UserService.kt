package io.realworld.domain.core

import io.realworld.domain.api.UserService
import io.realworld.domain.api.event.AuthenticateEvent
import io.realworld.domain.api.event.AuthenticatedEvent
import io.realworld.domain.spi.UserRepository

class CoreUserService(
  val auth: Auth,
  val userRepository: UserRepository
) : UserService {
  override fun authenticate(e: AuthenticateEvent): AuthenticatedEvent {
    val token = auth.parse(e.token)
    val user = userRepository.findByEmail(token.email)
    return when (user?.email) {
      // TODO check expiration
      token.email -> AuthenticatedEvent(user.toDto())
      else -> throw RuntimeException("Authentication required")
    }
  }
}
