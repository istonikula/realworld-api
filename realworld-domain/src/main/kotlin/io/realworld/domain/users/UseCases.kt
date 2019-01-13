package io.realworld.domain.users

import arrow.Kind
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.data.EitherT
import arrow.data.value
import arrow.instances.monad
import arrow.typeclasses.Monad
import arrow.typeclasses.binding
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

interface RegisterUserUseCase<F> {
  val auth: Auth
  val createUser: CreateUser<F>
  val validateUser: ValidateUserRegistration<F>
  val M: Monad<F>

  fun RegisterUserCommand.runUseCase(): Kind<F, Either<UserRegistrationError, User>> {
    val cmd = this
    return EitherT.monad<F, UserRegistrationError>(M).binding {
      val validRegistration = EitherT(validateUser(cmd.data)).bind()
      EitherT(
        M.run { createUser(validRegistration).map { it.right() } }
      ).bind()
    }.value()
  }
}

interface LoginUserUseCase<F> {
  val auth: Auth
  val getUser: GetUserByEmail<F>
  val M: Monad<F>

  fun LoginUserCommand.runUseCase(): Kind<F, Either<UserLoginError, User>> {
    val cmd = this
    return EitherT.monad<F, UserLoginError>(M).binding {
      val userAndPassword = EitherT(
        M.run {
          getUser(cmd.email).map { it.toEither { UserLoginError.BadCredentials } }
        }
      ).bind()
      EitherT(M.just(
        when (auth.checkPassword(cmd.password, userAndPassword.encryptedPassword)) {
          true -> userAndPassword.right()
          false -> UserLoginError.BadCredentials.left()
        }
      )).bind()
      userAndPassword.user
    }.value()
  }
}

interface UpdateUserUseCase<F> {
  val auth: Auth
  val validateUpdate: ValidateUserUpdate<F>
  val updateUser: UpdateUser<F>
  val M: Monad<F>

  fun UpdateUserCommand.runUseCase(): Kind<F, Either<UserUpdateError, User>> {
    val cmd = this
    return EitherT.monad<F, UserUpdateError>(M).binding {
      val validUpdate = EitherT(validateUpdate(cmd.data, cmd.current)).bind()
      EitherT(
        M.run { updateUser(validUpdate, current).map { it.right() } }
      ).bind()
    }.value()
  }
}
