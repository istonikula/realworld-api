package io.realworld.domain.users

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import arrow.fx.ForIO
import arrow.fx.IO
import arrow.fx.extensions.io.monad.monad
import arrow.fx.fix
import arrow.mtl.EitherT
import arrow.mtl.extensions.eithert.monad.monad
import arrow.mtl.value
import io.realworld.domain.common.Auth

data class RegisterUserCommand(val data: UserRegistration)
data class LoginUserCommand(val email: String, val password: String)
data class UpdateUserCommand(val data: UserUpdate, val current: User)

interface RegisterUserUseCase {
  val createUser: CreateUser
  val validateUser: ValidateUserRegistration

  fun RegisterUserCommand.runUseCase(): IO<Either<UserRegistrationError, User>> {
    val cmd = this
    return EitherT.monad<UserRegistrationError, ForIO>(IO.monad()).fx.monad {
      val validRegistration = EitherT(validateUser(cmd.data)).bind()
      EitherT(createUser(validRegistration).map { it.right() }).bind()
    }.value().fix()
  }
}

interface LoginUserUseCase {
  val auth: Auth
  val getUser: GetUserByEmail

  fun LoginUserCommand.runUseCase(): IO<Either<UserLoginError, User>> {
    val cmd = this
    return EitherT.monad<UserLoginError, ForIO>(IO.monad()).fx.monad {
      val userAndPassword = EitherT(
        getUser(cmd.email).map {
          it.toEither { UserLoginError.BadCredentials }
        }
      ).bind()
      EitherT(IO.just(
        when (auth.checkPassword(cmd.password, userAndPassword.encryptedPassword)) {
          true -> userAndPassword.right()
          false -> UserLoginError.BadCredentials.left()
        }
      )).bind()
      userAndPassword.user
    }.value().fix()
  }
}

interface UpdateUserUseCase {
  val validateUpdate: ValidateUserUpdate
  val updateUser: UpdateUser

  fun UpdateUserCommand.runUseCase(): IO<Either<UserUpdateError, User>> {
    val cmd = this
    return EitherT.monad<UserUpdateError, ForIO>(IO.monad()).fx.monad {
      val validUpdate = EitherT(validateUpdate(cmd.data, cmd.current)).bind()
      EitherT(updateUser(validUpdate, current).map { it.right() }).bind()
    }.value().fix()
  }
}
