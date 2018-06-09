package io.realworld.domain.users

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.effects.IO

interface ValidateUser {
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

interface SaveUserIO { // TODO repo should return IO
  val userRepository: UserRepository

  fun UserModel.save(): IO<UserModel> {
    return IO { userRepository.save(this) }
  }
}

typealias Email = String
interface GetUserByEmail {
  val userRepository: UserRepository

  fun Email.getUser(): IO<Either<UserNotFound, UserModel>> {
    return IO { userRepository.findByEmail(this)?.right() ?: UserNotFound().left() }
  }
}
