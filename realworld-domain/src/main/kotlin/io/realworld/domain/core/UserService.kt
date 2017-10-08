package io.realworld.domain.core

import io.realworld.domain.api.UserService
import io.realworld.domain.api.event.*
import io.realworld.domain.spi.UserModel
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

  override fun register(e: RegisterEvent) = RegisteredEvent(
    userRepository.save(UserModel(
      email = e.email,
      username = e.username,
      password = auth.encryptPassword(e.password),
      token = auth.createToken(Token(e.email))
    )).toDto()
  )

  override fun login(e: LoginEvent): LoggedInEvent {
    val user = userRepository.findByEmail(e.email)
    return when(user?.password?.let { auth.checkPassword(e.password, it) }) {
      // TODO renew token when we have timestamp
      true -> LoggedInEvent(user.toDto())
      false, null -> throw RuntimeException("Bad credentials")
    }
  }
}
