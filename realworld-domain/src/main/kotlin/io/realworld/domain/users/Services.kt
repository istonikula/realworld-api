package io.realworld.domain.users

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.effects.IO

interface ValidateUserService {
  val userRepository: UserRepository

  fun UserRegistration.validate(): IO<Either<UserRegistrationError, UserRegistration>> {
    return IO {
      when {
        userRepository.existsByEmail(this.email) ->
          UserRegistrationError.EmailAlreadyTaken.left()
        userRepository.existsByUsername(this.username) ->
          UserRegistrationError.UsernameAlreadyTaken.left()
        else -> this.right()
      }
    }
  }
}

interface CreateUserService {
  val userRepository: UserRepository

  fun ValidUserRegistration.create(): IO<User> {
    return IO { userRepository.create(this) }
  }
}

interface ValidateUserUpdateService {
  val userRepository: UserRepository

  fun UserUpdate.validate(): IO<Either<UserUpdateError, UserUpdate>> {
    val email = this.email.orNull()
    val username = this.username.orNull()

    return IO {
      val v = email != null && userRepository.existsByEmail(email)
      when {
        email != null && userRepository.existsByEmail(email) ->
          UserUpdateError.EmailAlreadyTaken.left()
        username != null && userRepository.existsByUsername(username) ->
          UserUpdateError.UsernameAlreadyTaken.left()
        else -> this.right()
      }
    }
  }
}

typealias Email = String
interface GetUserByEmailService {
  val userRepository: UserRepository

  fun Email.getUser(): IO<Either<UserNotFound, UserAndPassword>> {
    return IO { userRepository.findByEmail(this)?.right() ?: UserNotFound.left() }
  }
}
