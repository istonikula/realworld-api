package io.realworld.domain.users

import arrow.core.Either
import arrow.core.Option
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
sealed class UserLoginError {
  object BadCredentials : UserLoginError()
}

sealed class UserRegistrationValidationError {
  object EmailAlreadyTaken : UserRegistrationValidationError()
  object UsernameAlreadyTaken : UserRegistrationValidationError()
}

data class UserUpdate(
  val username: Option<String>,
  val email: Option<String>,
  val password: Option<String>,
  val bio: Option<String>,
  val image: Option<String>
)

// TODO these are duplicates
sealed class UserUpdateValidationError {
  object EmailAlreadyTaken : UserUpdateValidationError()
  object UsernameAlreadyTaken : UserUpdateValidationError()
}

interface RegisterUserUseCase {
  val auth: Auth
  val saveUser: SaveUser
  val validateUser: ValidateUserRegistration

  fun RegisterUserCommand.registerUser(): IO<Either<UserRegistrationValidationError, User>> {
    val cmd = this
    return ForEitherT<ForIO, UserRegistrationValidationError>(arrow.effects.IO.monad()) extensions {
      binding {
        val validRegistration = EitherT(validateUser(cmd.data)).bind()
        val savedUser = EitherT(
          saveUser(UserModel(
            email = validRegistration.email,
            username = validRegistration.username,
            password = auth.encryptPassword(validRegistration.password),
            token = auth.createToken(Token(validRegistration.email))
          )).map { it.right() }
        ).bind()
        savedUser.toDomain()
      }.value().fix()
    }
  }
}

interface LoginUserUseCase {
  val auth: Auth
  val getUser: GetUser

  fun LoginUserCommand.loginUser(): IO<Either<UserLoginError, User>> {
    val cmd = this
    return ForEitherT<ForIO, UserLoginError>(IO.monad()) extensions {
      binding {
        val user = EitherT(getUser(cmd.email)).mapLeft(IO.functor(), { UserLoginError.BadCredentials }).bind()
        EitherT(IO.just(
          when (auth.checkPassword(cmd.password, user.password)) {
            true -> user.toDomain().right()
            false -> UserLoginError.BadCredentials.left()
          }
        )).bind()
      }.value().fix()
    }
  }
}
