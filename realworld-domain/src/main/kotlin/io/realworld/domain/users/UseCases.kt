package io.realworld.domain.users

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import io.realworld.domain.common.Auth

data class RegisterUserCommand(val data: UserRegistration)
data class LoginUserCommand(val email: String, val password: String)
data class UpdateUserCommand(val data: UserUpdate, val current: User)
sealed class UserLoginError {
  object BadCredentials : UserLoginError()
}

sealed class UserRegistrationError {
  object EmailAlreadyTaken : UserRegistrationError()
  object UsernameAlreadyTaken : UserRegistrationError()
}

sealed class UserUpdateError {
  object EmailAlreadyTaken : UserUpdateError()
  object UsernameAlreadyTaken : UserUpdateError()
}

interface RegisterUserUseCase {
  val createUser: CreateUser
  val validateUser: ValidateUserRegistration

  suspend fun RegisterUserCommand.runUseCase(): Either<UserRegistrationError, User> {
    val cmd = this
    return either {
      val validRegistration = validateUser(cmd.data).bind()
      createUser(validRegistration)
    }
  }
}

interface LoginUserUseCase {
  val auth: Auth
  val getUser: GetUserByEmail

  suspend fun LoginUserCommand.runUseCase(): Either<UserLoginError, User> {
    val cmd = this
    return either {
      val userAndPassword = getUser(cmd.email).toEither { UserLoginError.BadCredentials }.bind()
      ensure(auth.checkPassword(cmd.password, userAndPassword.encryptedPassword)) { UserLoginError.BadCredentials }
      userAndPassword.user
    }
  }
}

interface UpdateUserUseCase {
  val validateUpdate: ValidateUserUpdate
  val updateUser: UpdateUser

  suspend fun UpdateUserCommand.runUseCase(): Either<UserUpdateError, User> {
    val cmd = this
    return either {
      val validUpdate = validateUpdate(cmd.data, cmd.current).bind()
      updateUser(validUpdate, current)
    }
  }
}
