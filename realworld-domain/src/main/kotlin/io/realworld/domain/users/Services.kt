package io.realworld.domain.users

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.effects.IO

interface ValidateUserService {
  val userRepository: UserRepository

  fun UserRegistration.validate(): IO<Either<UserRegistrationValidationError, UserRegistration>> {
    return IO {
      when {
        userRepository.existsByEmail(this.email) ->
          UserRegistrationValidationError.EmailAlreadyTaken.left()
        userRepository.existsByUsername(this.username) ->
          UserRegistrationValidationError.UsernameAlreadyTaken.left()
        else -> this.right()
      }
    }
  }
}

interface CreateUserService { // TODO repo should return IO
  val userRepository: UserRepository

  fun ValidUserRegistration.create(): IO<User> {
    return IO { userRepository.create(this) }
  }
}

typealias Email = String
interface GetUserByEmailService {
  val userRepository: UserRepository

  fun Email.getUser(): IO<Either<UserNotFound, UserAndPassword>> {
    return IO { userRepository.findByEmail(this)?.right() ?: UserNotFound().left() }
  }
}
