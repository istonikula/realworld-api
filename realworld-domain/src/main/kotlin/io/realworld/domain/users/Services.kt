package io.realworld.domain.users

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.extensions
import arrow.effects.fix
import arrow.typeclasses.binding

interface ValidateUserService {
  val userRepository: UserRepository

  fun UserRegistration.validate(): IO<Either<UserRegistrationError, UserRegistration>> {
    val cmd = this
    return ForIO extensions {
      binding {
        when {
          userRepository.existsByEmail(cmd.email).bind() ->
            UserRegistrationError.EmailAlreadyTaken.left()
          userRepository.existsByUsername(cmd.username).bind() ->
            UserRegistrationError.UsernameAlreadyTaken.left()
          else -> cmd.right()
        }
      }.fix()
    }
  }
}

interface ValidateUserUpdateService {
  val userRepository: UserRepository

  fun UserUpdate.validate(): IO<Either<UserUpdateError, UserUpdate>> {
    val cmd = this
    val email = cmd.email.orNull()
    val username = cmd.username.orNull()
    return ForIO extensions {
      binding {
        when {
          email != null && userRepository.existsByEmail(email).bind() ->
            UserUpdateError.EmailAlreadyTaken.left()
          username != null && userRepository.existsByUsername(username).bind() ->
            UserUpdateError.UsernameAlreadyTaken.left()
          else -> cmd.right()
        }
      }.fix()
    }
  }
}
