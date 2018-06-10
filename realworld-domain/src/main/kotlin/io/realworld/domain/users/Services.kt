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

  fun UserUpdate.validate(current: User): IO<Either<UserUpdateError, UserUpdate>> {
    val cmd = this
    return ForIO extensions {
      binding {
        when {
          cmd.email.fold({ false }, { current.email !== it && userRepository.existsByEmail(it).bind() }) ->
            UserUpdateError.EmailAlreadyTaken.left()
          cmd.username.fold({ false }, { current.username !== it && userRepository.existsByUsername(it).bind() }) ->
            UserUpdateError.UsernameAlreadyTaken.left()
          else -> cmd.right()
        }
      }.fix()
    }
  }
}
