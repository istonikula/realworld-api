package io.realworld.domain.core

import arrow.core.Either
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
import io.realworld.domain.api.LoginUserAcknowledgment
import io.realworld.domain.api.LoginUserCommand
import io.realworld.domain.api.RegisterUserAcknowledgment
import io.realworld.domain.api.RegisterUserCommand
import io.realworld.domain.api.UserLoginError
import io.realworld.domain.api.UserRegistration
import io.realworld.domain.api.UserRegistrationValidationError
import io.realworld.domain.spi.GetUser
import io.realworld.domain.spi.SaveUser
import io.realworld.domain.spi.UserModel
import io.realworld.domain.spi.UserNotFound
import io.realworld.domain.spi.UserRepository
import io.realworld.domain.spi.ValidateUserRegistration

interface RegisterUserWorkflowSyntax {
  val auth: Auth
  val saveUser: SaveUser
  val validateUser: ValidateUserRegistration

  fun RegisterUserCommand.registerUser(): IO<Either<UserRegistrationValidationError, RegisterUserAcknowledgment>> {
    val cmd = this
    return ForEitherT<ForIO, UserRegistrationValidationError>(IO.monad()) extensions {
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
        RegisterUserAcknowledgment(savedUser.toDomain())
      }.value().fix()
    }
  }
}

interface ValidateUserSyntax {
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

interface SaveUserSyntax {
  val userRepository: UserRepository

  fun UserModel.save(): IO<UserModel> {
    return IO { userRepository.save(this) }
  }
}

interface LoginUserWorkflowSyntax {
  val auth: Auth
  val getUser: GetUser

  fun LoginUserCommand.loginUser(): IO<Either<UserLoginError, LoginUserAcknowledgment>> {
    val cmd = this
    return ForEitherT<ForIO, UserLoginError>(IO.monad()) extensions {
      binding {
        val user = EitherT(getUser(cmd.email)).mapLeft(IO.functor(), { UserLoginError.BadCredentials }).bind()
        EitherT(IO.just(
          when (auth.checkPassword(cmd.password, user.password)) {
            true -> LoginUserAcknowledgment(user.toDomain()).right()
            false -> UserLoginError.BadCredentials.left()
          }
        )).bind()
      }.value().fix()
    }
  }
}

typealias Email = String
interface GetUserSyntax {
  val userRepository: UserRepository

  fun Email.getUser(): IO<Either<UserNotFound, UserModel>> {
    return IO { userRepository.findByEmail(this)?.right() ?: UserNotFound().left() }
  }
}
