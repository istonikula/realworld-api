package io.realworld.domain.users

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import arrow.data.EitherT
import arrow.data.value
import arrow.effects.ForIO
import arrow.effects.IO
import arrow.effects.fix
import arrow.effects.functor
import arrow.effects.monad
import arrow.instances.ForEitherT
import arrow.typeclasses.binding
import io.realworld.domain.common.Auth
import io.realworld.domain.common.Token

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

object UserNotFound

interface RegisterUserUseCase {
  val auth: Auth
  val createUser: CreateUser
  val validateUser: ValidateUserRegistration

  fun RegisterUserCommand.runUseCase(): IO<Either<UserRegistrationError, User>> {
    val cmd = this
    return ForEitherT<ForIO, UserRegistrationError>(arrow.effects.IO.monad()) extensions {
      binding {
        val validRegistration = EitherT(validateUser(cmd.data)).bind()
        EitherT(
          createUser(ValidUserRegistration(
            email = validRegistration.email,
            username = validRegistration.username,
            token = auth.createToken(Token(validRegistration.email)),
            encryptedPassword = auth.encryptPassword(validRegistration.password)
          )).map { it.right() }
        ).bind()
      }.value().fix()
    }
  }
}

interface LoginUserUseCase {
  val auth: Auth
  val getUser: GetUser

  fun LoginUserCommand.runUseCase(): IO<Either<UserLoginError, User>> {
    val cmd = this
    return ForEitherT<ForIO, UserLoginError>(IO.monad()) extensions {
      binding {
        val user = EitherT(getUser(cmd.email)).mapLeft(IO.functor(), { UserLoginError.BadCredentials }).bind()
        EitherT(IO.just(
          when (auth.checkPassword(cmd.password, user.encryptedPassword)) {
            true -> user.right()
            false -> UserLoginError.BadCredentials.left()
          }
        )).bind()
        user.user
      }.value().fix()
    }
  }
}

interface UpdateUserUseCase {
  val auth: Auth
  val validateUpdate: ValidateUserUpdate
  val updateUser: UpdateUser

  fun UpdateUserCommand.runUseCase(): IO<Either<UserUpdateError, User>> {
    val cmd = this
    return ForEitherT<ForIO, UserUpdateError>(IO.monad()) extensions {
      binding {
        val user = EitherT(validateUpdate(cmd.data)).bind()
        EitherT(
          updateUser(ValidUserUpdate(
            email = user.email.getOrElse { cmd.current.email },
            username = user.username.getOrElse { cmd.current.username },
            encryptedPassword = user.password.map { auth.encryptPassword(it) },
            bio = user.bio.getOrElse { cmd.current.bio },
            image = user.image.getOrElse { cmd.current.image }
          ), current).map { it.right() }
        ).bind()
      }.value().fix()
    }
  }
}
